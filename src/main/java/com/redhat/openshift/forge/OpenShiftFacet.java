package com.redhat.openshift.forge;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
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
import com.openshift.client.ICartridge;
import com.openshift.client.IOpenShiftConnection;
import com.openshift.client.IUser;
import com.openshift.client.InvalidCredentialsOpenShiftException;
import com.openshift.client.JBossCartridge;
import com.openshift.client.JenkinsCartridge;
import com.openshift.client.NotFoundOpenShiftException;
import com.openshift.client.OpenShiftEndpointException;
import com.openshift.client.OpenShiftException;
import com.openshift.internal.client.Cartridge;
import com.openshift.internal.client.JenkinsApplication;
import com.redhat.openshift.core.OpenShiftServiceFactory;

@Alias("forge.openshift")
public class OpenShiftFacet extends BaseFacet {
  
    private static final int MAX_WAIT = 500;

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
        IOpenShiftConnection openshiftService = OpenShiftServiceFactory
				.create(rhLogin, password, baseUrl);

        IUser user = openshiftService.getUser();
        
        ShellMessages.info(out, "Found OpenShift User: " + user.getRhlogin());
        
        ICartridge jbossCartridge = getJBossCartridge(openshiftService);
        
        ShellMessages.info(out, "Found JBoss Cartridge: " + jbossCartridge.getName());

        IApplication application = Util.createApplication(openshiftService, jbossCartridge, user, name, out);
        if (application == null)
           return false;

        if (!project.getProjectRoot().getChildDirectory(".git").exists()) {
            String[] params = { "init" };
            if (NativeSystemCall.execFromPath("git", params, out, project.getProjectRoot()) != 0)
               return false;
        }

        ShellMessages.info(out, "Waiting for OpenShift to propagate DNS");
        if (!waitForOpenShift(application.getApplicationUrl(), out)) {
            ShellMessages.error(out, "OpenShift did not propagate DNS properly");
            return false;
        }

        if (!Util.isOpenshiftRemotePresent(out, project)) {
            String[] remoteParams = { "remote", "add", "openshift", "-f", application.getGitUrl() };
            if (NativeSystemCall.execFromPath("git", remoteParams, out, project.getProjectRoot()) != 0) {
               ShellMessages.error(out, "Failed to connect to OpenShift GIT repository, project is in an inconsistent state. Remove the .git directory manually, and delete the application using rhc-ctl-app -c destroy -a " + application.getName() + " -b");
               return false;
            }
        } else
           ShellMessages.info(out, "'openshift' remote alias already present in Git, using it");
        
        addOpenShiftProfile();

        ShellMessages.success(out, "Application deployed to " + application.getApplicationUrl());

        return true;
    }
    
    private ICartridge getJBossCartridge(IOpenShiftConnection openshiftService) throws OpenShiftException {
    	List<ICartridge> cartridges = openshiftService.getStandaloneCartridges();
    	for (ICartridge cartridge : cartridges){
    		if (cartridge.getName().contains("jbossas"))
    			return cartridge;
    	}
    	
    	return null;
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
                else
                    if (shell.isVerbose())
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

		public void checkServerTrusted(X509Certificate[] chain,
				String authType) throws CertificateException {
		}

		public void checkClientTrusted(X509Certificate[] chain,
				String authType) throws CertificateException {
		}
	}

}
