package com.redhat.openshift.forge;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RemoteSession;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.util.FS;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.events.InstallFacets;
import org.jboss.forge.shell.Shell;
import org.jboss.forge.shell.ShellMessages;
import org.jboss.forge.shell.ShellPrompt;
import org.jboss.forge.shell.plugins.Alias;
import org.jboss.forge.shell.plugins.Command;
import org.jboss.forge.shell.plugins.Option;
import org.jboss.forge.shell.plugins.PipeOut;
import org.jboss.forge.shell.plugins.RequiresFacet;
import org.jboss.forge.shell.plugins.RequiresProject;
import org.jboss.forge.shell.plugins.SetupCommand;
import org.jboss.forge.shell.util.NativeSystemCall;

import com.jcraft.jsch.JSch;
import com.openshift.client.IApplication;
import com.openshift.client.ICartridge;
import com.openshift.client.IEmbeddableCartridge;
import com.openshift.client.IEmbeddedCartridge;
import com.openshift.client.IOpenShiftConnection;
import com.openshift.client.IUser;
import com.openshift.client.InvalidCredentialsOpenShiftException;
import com.openshift.client.OpenShiftException;
import com.redhat.openshift.core.OpenShiftServiceFactory;
import com.redhat.openshift.forge.jsch.ForgeJschConfigSessionFactory;
import com.redhat.openshift.forge.jsch.JschToForgeLogger;

public @Alias("rhc")
@RequiresProject
@RequiresFacet(OpenShiftFacet.class)
class OpenShiftPlugin implements org.jboss.forge.shell.plugins.Plugin {

    public static final int CONNECT_TIMEOUT_MINUTES = 2;

    @Inject
    private Event<InstallFacets> request;

    @Inject
    private Project project;

    @Inject
    private OpenShiftConfiguration configuration;

    @Inject
    private FacetInstallerConfigurationHolder holder;

    @Inject
    private Shell shell;

    @Inject
    private ShellPrompt prompt;

    @Inject
    private JschToForgeLogger jschToForgeLogger;

    @Inject
    private ForgeJschConfigSessionFactory forgeJschConfigSessionFactory;

    @SetupCommand(help = "Install and set up the OpenShift plugin")
    public void setup(
            PipeOut out,
            @Option(name = "app", help = "Application name (alphanumeric - max 32 chars)") final String name,
            @Option(name = "rhlogin", help = "Red Hat login (RHN or OpenShift login with OpenShift access)") final String rhLogin,
            @Option(name = "gitremotename", help = "git remote name for openshift. Default to 'openshift'", defaultValue = "openshift") final String gitRemote,
            @Option(name = "scaling", help = "Enable scaling for this app. Operations. This operation can take several minutes") final boolean scaling)
            throws OpenShiftException, IOException {
        try {
            // Warn take scaling application can take several minutes
            if (scaling) {
                if (!shell.promptBoolean("Scaling application can take up to " + CONNECT_TIMEOUT_MINUTES
                        + " minutes to complete. Do you want to continue ?")) {
                    return;
                }
            }
            // make sure we wipe a previous use of the configuration
            configuration.setName(null);
            if (!project.hasFacet(OpenShiftFacet.class)) {
                holder.clear();
                holder.setName(name);
                holder.setRhLogin(rhLogin);
                holder.setGitRemoteRepo(gitRemote);
                holder.setScaling(scaling);
                request.fire(new InstallFacets(OpenShiftFacet.class));
            }

            if (project.hasFacet(OpenShiftFacet.class)) {
                ShellMessages.success(out, "OpenShift (rhc) is installed.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Command(help = "Add a Cartdrige do a existing application")
    public void addCartridge(PipeOut out,
            @Option(name = "app", help = "Application name (alphanumeric - max 32 chars)") String app,
            @Option(name = "rhlogin", help = "Red Hat login (RHN or OpenShift login with OpenShift access)") String rhLogin) {

        rhLogin = Util.getRhLogin(rhLogin, configuration, out, prompt);
        app = Util.getName(app, configuration, project, prompt);
        String password = Util.getPassword(prompt);
        String baseUrl = Util.getDefaultBaseUrl(out, configuration);

        IOpenShiftConnection openshiftService = OpenShiftServiceFactory.create(rhLogin, password, baseUrl);
        List<IEmbeddableCartridge> cartdriges = openshiftService.getEmbeddableCartridges();
        int option = shell.promptChoice("Choose the cartdrige:", cartdriges);
        IEmbeddableCartridge embeddableCartridge = cartdriges.get(option);
        Util.addCartridgeToApplication(openshiftService, app, embeddableCartridge, out);

    }

    @Command(help = "Tail the logs of the application")
    public void tail(PipeOut out, @Option(name = "app", help = "Application name (alphanumeric - max 32 chars)") String app,
            @Option(name = "rhlogin", help = "Red Hat login (RHN or OpenShift login with OpenShift access)") String rhLogin)
            throws IOException {
        rhLogin = Util.getRhLogin(rhLogin, configuration, out, prompt);
        app = Util.getName(app, configuration, project, prompt);
        String password = Util.getPassword(prompt);
        String baseUrl = Util.getDefaultBaseUrl(out, configuration);

        IOpenShiftConnection openshift = OpenShiftServiceFactory.create(rhLogin, password, baseUrl);
        IApplication application = openshift.getUser().getDefaultDomain().getApplicationByName(app);

        String logPath = "**/logs/*.log";
        String options = "-f -n 100";

        JSch.setLogger(jschToForgeLogger);
      
	String hostFormat = "%s-%s.rhcloud.com";
	if (baseUrl != null) {
	    if (baseUrl.contains("amazonaws"))
		hostFormat = "%s-%s.dev.rhcloud.com";
	    else if (baseUrl.contains("int."))
		hostFormat = "%s-%s.int.rhcloud.com";
	    else if (baseUrl.contains("stg."))
		hostFormat = "%s-%s.stg.rhcloud.com";
	}
        String host = String.format(hostFormat, application.getName(), application.getDomain().getId());
        
        URIish uri = new URIish().setHost(host).setUser(application.getUUID());
        BufferedReader br = null;
        try {

            SshSessionFactory.setInstance(forgeJschConfigSessionFactory);
            RemoteSession remoteSession = SshSessionFactory.getInstance().getSession(uri, CredentialsProvider.getDefault(),
                    FS.DETECTED, 0);
            String command = buildCommand(logPath, options);
            
            Process process = remoteSession.exec(command, 0);
            final TailPrintThread tailThread = new TailPrintThread(shell, process, remoteSession);
            while (!shell.promptBoolean("Press 'Q' anytime to stop. Do you understand ?")) {
                //Repeat until positive answer
            }
            tailThread.start();
            while (tailThread.isAlive()) {
                int key = shell.scan();
                if (key == 'q' || key == 'Q'){
                    tailThread.stopTail();
                }
            }
            ShellMessages.info(shell, "Tail stopped");
        } catch (Exception e) {
            ShellMessages.error(out, e.getMessage());
            e.printStackTrace();
        } finally {
            if (br != null) {
                br.close();
            }
        }

    }

    /**
     * Builds the 'ssh tail' command that should be executed on the remote OpenShift platform.
     * 
     * @param filePath
     * @param options
     * @return
     * @throws UnsupportedEncodingException
     */
    private String buildCommand(final String filePath, final String options) {
        StringBuilder commandBuilder = new StringBuilder("tail ");
        if (options != null && !options.isEmpty()) {
            commandBuilder.append(options).append(" ");
        }
        commandBuilder.append(filePath);
        final String command = commandBuilder.toString();
        if (shell.isVerbose()) {
            shell.println("ssh command to execute: " + command);
        }
        return command;
    }

    @Command(help = "Deploys the current application to OpenShift")
    public void deploy(
            PipeOut out,
            @Option(name = "app", help = "Application name (alphanumeric - max 32 chars)") final String app,
            @Option(name = "rhlogin", help = "Red Hat login (RHN or OpenShift login with OpenShift access)") final String rhLogin)
            throws Exception {
        String[] addParams = { "add", "." };
        NativeSystemCall.execFromPath("git", addParams, out, project.getProjectRoot());

        String[] commitParams = { "commit", "-a", "-m", "\"deploy\"" };
        NativeSystemCall.execFromPath("git", commitParams, out, project.getProjectRoot());

        String[] remoteParams = { "merge", "openshift/master", "-s", "recursive", "-X", "ours" };
        if (NativeSystemCall.execFromPath("git", remoteParams, out, project.getProjectRoot()) != 0) {
            ShellMessages.error(out, "Failed to rebase onto openshift");
        }

        /*
         * --progress is needed to see git status output from stderr
         */
        String[] pushParams = { "push", "openshift", "HEAD", "-f", "--progress" };
        NativeSystemCall.execFromPath("git", pushParams, out, project.getProjectRoot());
    }

    @Command(help = "Checks the status of a deployed application")
    public void status(PipeOut out, @Option(name = "app", help = "Application name (alphanumeric - max 32 chars)") String name,
            @Option(name = "rhlogin", help = "Red Hat login (RHN or OpenShift login with OpenShift access)") String rhLogin)
            throws Exception {
        rhLogin = Util.getRhLogin(rhLogin, configuration, out, prompt);
        name = Util.getName(name, configuration, project, prompt);
        String password = Util.getPassword(prompt);
        String baseUrl = Util.getDefaultBaseUrl(out, configuration);

        IOpenShiftConnection openshiftService = OpenShiftServiceFactory.create(rhLogin, password, baseUrl);
        try {
            IUser user = openshiftService.getUser();
            IApplication app = user.getDefaultDomain().getApplicationByName(name);
            Util.healthCheck(app, baseUrl, out);
        } catch (InvalidCredentialsOpenShiftException e) {
            Util.displayCredentialsError(out, e);
        }
    }

    @Command(help = "Removes the current application from OpenShift")
    public void destroy(PipeOut out,
            @Option(name = "app", help = "Application name (alphanumeric - max 32 chars)") String name,
            @Option(name = "rhlogin", help = "Red Hat login (RHN or OpenShift login with OpenShift access)") String rhLogin)
            throws Exception {
        rhLogin = Util.getRhLogin(rhLogin, configuration, out, prompt);
        name = Util.getName(name, configuration, project, prompt);
        String password = Util.getPassword(prompt);
        String baseUrl = Util.getDefaultBaseUrl(out, configuration);

        boolean confirm = prompt.promptBoolean("About to destroy application " + name + " on OpenShift. Are you sure?", true);

        if (confirm) {
            IOpenShiftConnection openshiftService = OpenShiftServiceFactory.create(rhLogin, password, baseUrl);
            try {
                IUser user = openshiftService.getUser();
                IApplication app = user.getDefaultDomain().getApplicationByName(name);
                app.destroy();
                ShellMessages.success(out, "Destroyed application " + name + " on OpenShift");
            } catch (InvalidCredentialsOpenShiftException e) {
                Util.displayCredentialsError(out, e);
            }
        }
    }

    @Command(help = "Displays information about your OpenShift applications")
    public void list(PipeOut out,
            @Option(name = "rhlogin", help = "Red Hat login (RHN or OpenShift login with OpenShift access)") String rhLogin)
            throws Exception {
        rhLogin = Util.getRhLogin(rhLogin, configuration, out, prompt);
        String password = Util.getPassword(prompt);
        String baseUrl = Util.getDefaultBaseUrl(out, configuration);

        IOpenShiftConnection openshiftService = OpenShiftServiceFactory.create(rhLogin, password, baseUrl);
        try {
            IUser user = openshiftService.getUser();
            ShellMessages.info(out, "Applications on OpenShift:");
            for (IApplication app : user.getDefaultDomain().getApplications()) {
                Util.printApplicationInfo(out, app, user.getDefaultDomain().getId(), app.getApplicationUrl());
            }
        } catch (InvalidCredentialsOpenShiftException e) {
            Util.displayCredentialsError(out, e);
        }
    }

    @Command(help = "Embed Jenkins into your OpenShift application", value = "embed-jenkins")
    public void embedJenkins(
            PipeOut out,
            @Option(name = "app", help = "Application name (alphanumeric - max 32 chars)") String name,
            @Option(name = "rhlogin", help = "Red Hat login (RHN or OpenShift login with OpenShift Express access)") String rhLogin)
            throws Exception {
        rhLogin = Util.getRhLogin(rhLogin, configuration, out, prompt);
        name = Util.getName(name, configuration, project, prompt);
        String password = Util.getPassword(prompt);
        String baseUrl = Util.getDefaultBaseUrl(out, configuration);
        IOpenShiftConnection openshiftService = OpenShiftServiceFactory.create(rhLogin, password, baseUrl);

        IUser user = openshiftService.getUser();

        if (jenkinsClientEmbedded(user.getDefaultDomain().getApplicationByName(name), out)) {
            ShellMessages.error(out, "Jenkins is already embedded!");
            return;
        }

        String jenkinsAppName = findJenkinsApp(user, out);
        if (jenkinsAppName == null) {
            jenkinsAppName = "jenkins";
            ShellMessages.info(out, "Adding \"jenkins\" application to domain");
            ShellMessages.info(out, "This is only needed if you haven't used Jenkins with OpenShift before");
            Util.createApplication(openshiftService, ICartridge.JENKINS_14, user, jenkinsAppName, false, out);
            ShellMessages.info(out, "Successfully added \"jenkins\" application to domain");
        }
        ShellMessages.info(out, "Embedding Jenkins client into application \"" + name + "\".");
        System.setProperty("sun.net.client.defaultReadTimeout", String.valueOf(1000 * 60 * CONNECT_TIMEOUT_MINUTES));
        IApplication app = user.getDefaultDomain().getApplicationByName(name);
        app.addEmbeddableCartridge(getJenkinsClientcartridge(openshiftService));
        ShellMessages.info(out, "Successfully embedded Jenkins into \"" + name + "\"");
        ShellMessages.info(out, "Any builds will now happen in Jenkins, available at http://" + jenkinsAppName + "-"
                + user.getDefaultDomain().getId() + ".rhcloud.com");
    }

    protected IEmbeddableCartridge getJenkinsClientcartridge(IOpenShiftConnection openshiftService) throws OpenShiftException {
        for (IEmbeddableCartridge cartridge : openshiftService.getEmbeddableCartridges()) {
            if (cartridge.getName().contains("jenkins-client"))
                return cartridge;
        }
        throw new OpenShiftException("No Jenkins Client found to add");
    }

    private boolean jenkinsClientEmbedded(IApplication app, PipeOut out) throws OpenShiftException {
        for (IEmbeddedCartridge cartridge : app.getEmbeddedCartridges()) {
            if (cartridge.getName().startsWith("jenkins"))
                return true;
        }
        return false;
    }

    private String findJenkinsApp(IUser user, PipeOut out) throws OpenShiftException {
        for (IApplication app : user.getDefaultDomain().getApplications()) {
            if (app.getCartridge().getName().startsWith("jenkins"))
                return app.getName();
        }
        return null;
    }

}