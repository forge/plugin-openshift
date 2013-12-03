package com.redhat.openshift.forge;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.jboss.forge.maven.MavenCoreFacet;
import org.jboss.forge.maven.plugins.ConfigurationBuilder;
import org.jboss.forge.maven.plugins.ConfigurationElementBuilder;
import org.jboss.forge.maven.plugins.MavenPlugin;
import org.jboss.forge.maven.plugins.MavenPluginAdapter;
import org.jboss.forge.maven.plugins.MavenPluginBuilder;
import org.jboss.forge.project.dependencies.DependencyBuilder;
import org.jboss.forge.project.facets.BaseFacet;
import org.jboss.forge.shell.Shell;
import org.jboss.forge.shell.ShellMessages;
import org.jboss.forge.shell.ShellPrintWriter;
import org.jboss.forge.shell.ShellPrompt;
import org.jboss.forge.shell.plugins.Alias;
import org.jboss.forge.shell.util.NativeSystemCall;

import com.openshift.client.IApplication;
import com.openshift.client.IDomain;
import com.openshift.client.IOpenShiftConnection;
import com.openshift.client.IUser;
import com.openshift.client.InvalidCredentialsOpenShiftException;
import com.openshift.client.OpenShiftException;
import com.openshift.client.cartridge.IStandaloneCartridge;
import com.redhat.openshift.core.OpenShiftServiceFactory;

@Alias("forge.openshift")
public class OpenShiftFacet extends BaseFacet {

    @Inject
    private ShellPrompt prompt;

    @Inject
    private ShellPrintWriter out;

    @Inject
    private Shell shell;

    @Inject
    private OpenShiftConfiguration configuration;

    @Inject
    private FacetInstallerConfigurationHolder holder;

    @Override
    public boolean isInstalled() {
        try {
            return Util.isGitInit(project) && Util.isOpenshiftRemotePresent(out, project);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean install() {
        try {
            return internalInstall();
        } catch (InvalidCredentialsOpenShiftException e) {
            Util.displayCredentialsError(out, e);
            return false;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean internalInstall() throws Exception, InvalidCredentialsOpenShiftException {

        String name = Util.getName(holder.getName(), configuration, project, prompt);
        String rhLogin = Util.getRhLogin(holder.getRhLogin(), configuration, out, prompt);
        String baseUrl = Util.getDefaultBaseUrl(out, configuration);

        ShellMessages.info(out, "Using RHLOGIN:" + rhLogin + " for " + baseUrl);

        // Set up the project name :-)
        configuration.setName(name);
        String password = Util.getPassword(prompt);
        IOpenShiftConnection openshiftService = OpenShiftServiceFactory.create(rhLogin, password, baseUrl);
        
        IUser user = openshiftService.getUser();
        
        if (user == null){
        	ShellMessages.info(out, "Error. Could not find OpenShift User: " + user.getRhlogin());
        	return false;
        }
        
        IDomain domain = user.getDefaultDomain();
        if (domain == null){
        	ShellMessages.info(out, "Error. Could not find OpenShift Domain for User: " + user.getRhlogin());
        	return false;
        }
        
        boolean appExists = domain.hasApplicationByName(name);

        ShellMessages.info(out, "Found OpenShift User: " + user.getRhlogin());

        IApplication application = null;
        if (appExists) {
            ShellMessages.warn(out, String.format("Application with [%s] name already exists on Openshift", name));
            boolean ok = shell.promptBoolean("Do you want to continue ?");
            if (ok) {
                application = openshiftService.getUser().getDefaultDomain().getApplicationByName(name);
            } else {
                ShellMessages.warn(out, "Setup cancelled");
                return false;
            }
        } else {
            IStandaloneCartridge jbossCartridge = getJBossCartridge(openshiftService);

            ShellMessages.info(out, "Using JBoss Cartridge: " + jbossCartridge.getName());
            application = Util.createApplication(openshiftService, jbossCartridge, user, name, holder.isScaling(), out);
        }

        if (application == null) {
            return false;
        }

        if (!project.getProjectRoot().getChildDirectory(".git").exists()) {
            String[] params = { "init" };
            if (NativeSystemCall.execFromPath("git", params, out, project.getProjectRoot()) != 0) {
                return false;
            }
        }

        ShellMessages.info(out, "Waiting for OpenShift to propagate DNS");
        if (!waitForOpenShift(application.getApplicationUrl(), out)) {
            ShellMessages.error(out, "OpenShift did not propagate DNS properly");
            return false;
        }

        if (!Util.isOpenshiftRemotePresent(out, project)) {
            String[] remoteParams = { "remote", "add", holder.getGitRemoteRepo(), "-f", application.getGitUrl() };
            if (NativeSystemCall.execFromPath("git", remoteParams, out, project.getProjectRoot()) != 0) {
                ShellMessages
                        .error(out,
                                "Failed to connect to OpenShift GIT repository, project is in an inconsistent state. Remove the .git directory manually, and delete the application using rhc app delete -a "
                                        + application.getName());
                return false;
            }
        } else {
            ShellMessages.info(out,
                    String.format("'%s' remote alias already present in Git, using it", holder.getGitRemoteRepo()));
        }
        addOpenShiftProfile();
        ShellMessages.success(out, "Application deployed to " + application.getApplicationUrl());
        return true;

    }

    private IStandaloneCartridge getJBossCartridge(IOpenShiftConnection openshiftService) throws OpenShiftException {
        List<IStandaloneCartridge> cartridges = openshiftService.getStandaloneCartridges();
        List<IStandaloneCartridge> jbossCartdriges = new ArrayList<IStandaloneCartridge>();
        for (IStandaloneCartridge cartridge : cartridges) {
            if (cartridge.getName().contains("jboss")) {
                jbossCartdriges.add(cartridge);
            }
        }
        return shell.promptChoiceTyped("Choose a JBoss Cartridge:", jbossCartdriges, jbossCartdriges.get(0));
    }

    private void addOpenShiftProfile() {
        MavenCoreFacet facet = getProject().getFacet(MavenCoreFacet.class);
        for (Profile p : facet.getPOM().getProfiles()) {
            if (p.getId().equals("openshift"))
                return;
        }
        MavenPlugin plugin = MavenPluginBuilder
                .create()
                .setDependency(
                        DependencyBuilder.create().setGroupId("org.apache.maven.plugins").setArtifactId("maven-war-plugin")
                                .setVersion("2.1.1"))
                .setConfiguration(
                        ConfigurationBuilder
                                .create()
                                .addConfigurationElement(
                                        ConfigurationElementBuilder.create().setName("outputDirectory").setText("deployments"))
                                .addConfigurationElement(
                                        ConfigurationElementBuilder.create().setName("warName").setText("ROOT")));
        Profile profile = new Profile();
        profile.setId("openshift");
        profile.setBuild(new BuildBase());
        profile.getBuild().addPlugin(new MavenPluginAdapter(plugin));

        Model pom = facet.getPOM();
        pom.addProfile(profile);
        facet.setPOM(pom);
    }

    private boolean waitForOpenShift(String urlString, ShellPrintWriter out) {
        int dnsTimeout = Util.getDefaultDNSTimeout(out, configuration);
        for (int i = 0; i < dnsTimeout; i++) {
            try {
                URL url = new URL(urlString);
                if (i % 5 == 0)
                    ShellMessages.info(out, "Trying to contact " + url + " (attempt " + (i + 1) + " of " + dnsTimeout + ")");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                if (isHttps(url)) {
                    HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;
                    httpsConnection.setHostnameVerifier(new NoopHostnameVerifier());
                    setPermissiveSSLSocketFactory(httpsConnection);
                }
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK)
                    return true;
                else if (shell.isVerbose())
                    ShellMessages.info(out, "ResponseCode=" + connection.getResponseCode());
            } catch (Exception e) {
                if (shell.isVerbose())
                    ShellMessages.info(out, "Caught exception: " + e);
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return false;
    }

    private boolean isHttps(URL url) {
        return "https".equals(url.getProtocol());
    }

    private static class NoopHostnameVerifier implements HostnameVerifier {

        public boolean verify(String hostname, SSLSession sslSession) {
            return true;
        }
    }

    private void setPermissiveSSLSocketFactory(HttpsURLConnection connection) {
        try {
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(new KeyManager[0], new TrustManager[] { new PermissiveTrustManager() }, new SecureRandom());
            SSLSocketFactory socketFactory = sslContext.getSocketFactory();
            ((HttpsURLConnection) connection).setSSLSocketFactory(socketFactory);
        } catch (KeyManagementException e) {
            // ignore
        } catch (NoSuchAlgorithmException e) {
            // ignore
        }
    }

    private static class PermissiveTrustManager implements X509TrustManager {

        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }
    }

}
