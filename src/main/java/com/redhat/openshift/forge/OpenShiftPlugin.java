package com.redhat.openshift.forge;

import java.io.IOException;

import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.events.InstallFacets;
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

import com.openshift.express.client.IOpenShiftService;
import com.openshift.express.client.IUser;
import com.openshift.express.client.InvalidCredentialsOpenShiftException;
import com.openshift.express.client.JBossCartridge;
import com.openshift.express.client.JenkinsCartridge;
import com.openshift.express.client.NotFoundOpenShiftException;
import com.openshift.express.client.OpenShiftException;
import com.openshift.express.internal.client.ApplicationInfo;
import com.openshift.express.internal.client.EmbeddableCartridge;
import com.openshift.express.internal.client.EmbeddableCartridgeInfo;
import com.openshift.express.internal.client.InternalUser;
import com.openshift.express.internal.client.JenkinsClientEmbeddableCartridge;
import com.openshift.express.internal.client.UserInfo;
import com.redhat.openshift.core.OpenShiftServiceFactory;

public @Alias("rhc")
@RequiresProject
@RequiresFacet(OpenShiftFacet.class)
class OpenShiftPlugin implements org.jboss.forge.shell.plugins.Plugin {

    @Inject
    private Event<InstallFacets> request;

    @Inject
    private Project project;

    @Inject
    private OpenShiftConfiguration configuration;
    
    @Inject
    private FacetInstallerConfigurationHolder holder;
    
    @Inject ShellPrompt prompt;

    @SetupCommand(help = "Install and set up the OpenShift plugin")
    public void setup(PipeOut out, @Option(name = "app", help = "Application name (alphanumeric - max 32 chars)") final String name, @Option(name = "rhlogin", help = "Red Hat login (RHN or OpenShift login with OpenShift access)") final String rhLogin)
            throws OpenShiftException, IOException {
    	try {
    	     // make sure we wipe a previous use of the configuration
    	     configuration.setName(null);
	        if (!project.hasFacet(OpenShiftFacet.class)) {
	            holder.clear();
	            if (name != null)
	               holder.setName(name);
	            if (rhLogin != null)
	               holder.setRhLogin(rhLogin);
	            request.fire(new InstallFacets(OpenShiftFacet.class));
	        }
	
	        if (project.hasFacet(OpenShiftFacet.class)) {
	            ShellMessages.success(out, "OpenShift (rhc) is installed.");
	        }
    	} catch (Exception e){
    		e.printStackTrace();
    	}

    }

    @Command(help = "Deploys the current application to OpenShift")
    public void deploy(PipeOut out, @Option(name = "app", help = "Application name (alphanumeric - max 32 chars)") final String app, @Option(name = "rhlogin", help = "Red Hat login (RHN or OpenShift login with OpenShift access)") final String rhLogin) throws Exception {
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
    public void status(PipeOut out, @Option(name = "app", help = "Application name (alphanumeric - max 32 chars)") String name, @Option(name = "rhlogin", help = "Red Hat login (RHN or OpenShift login with OpenShift access)") String rhLogin) throws Exception {
        rhLogin = Util.getRhLogin(rhLogin, configuration, out, prompt);
        name = Util.getName(name, configuration, project, prompt);
        String password = Util.getPassword(prompt);
        String baseUrl = Util.getDefaultBaseUrl(out, configuration);

        IOpenShiftService openshiftService = OpenShiftServiceFactory.create(baseUrl);
        try {
           IUser user = new InternalUser(rhLogin, password, openshiftService);
           String status = openshiftService.getStatus(name, new JBossCartridge(openshiftService, user), user);
           ShellMessages.info(out, "Status of application follows\n");
           out.print(status);
        } catch (InvalidCredentialsOpenShiftException e) {
           Util.displayCredentialsError(out, e);
        }
    }
    
    @Command(help = "Removes the current application from OpenShift")
    public void destroy(PipeOut out, @Option(name = "app", help = "Application name (alphanumeric - max 32 chars)") String name, @Option(name = "rhlogin", help = "Red Hat login (RHN or OpenShift login with OpenShift access)") String rhLogin) throws Exception {
        rhLogin = Util.getRhLogin(rhLogin, configuration, out, prompt);
        name = Util.getName(name, configuration, project, prompt);
        String password = Util.getPassword(prompt);
        String baseUrl = Util.getDefaultBaseUrl(out, configuration);

        boolean confirm = prompt.promptBoolean("About to destroy application " + name + " on OpenShift. Are you sure?", true);
        
        if (confirm) {
           IOpenShiftService openshiftService = OpenShiftServiceFactory.create(baseUrl);
           try {
        	  IUser user = new InternalUser(rhLogin, password, openshiftService);
              openshiftService.destroyApplication(name, new JBossCartridge(openshiftService, user), user);
              ShellMessages.success(out, "Destroyed application " + name + " on OpenShift");
           } catch (InvalidCredentialsOpenShiftException e) {
              Util.displayCredentialsError(out, e);
           }
        }
    }
    
    @Command(help = "Displays information about your OpenShift applications")
    public void list(PipeOut out, @Option(name = "rhlogin", help = "Red Hat login (RHN or OpenShift login with OpenShift access)") String rhLogin) throws Exception {
        rhLogin = Util.getRhLogin(rhLogin, configuration, out, prompt);
        String password = Util.getPassword(prompt);
        String baseUrl = Util.getDefaultBaseUrl(out, configuration);

        IOpenShiftService openshiftService = OpenShiftServiceFactory.create(baseUrl);
        try {
           UserInfo info = openshiftService.getUserInfo(new InternalUser(rhLogin, password, openshiftService));
           ShellMessages.info(out, "Applications on OpenShift:");
           for (ApplicationInfo app : info.getApplicationInfos()) {
              Util.printApplicationInfo(out, app, info.getNamespace(), info.getRhcDomain());
           }
        } catch (InvalidCredentialsOpenShiftException e) {
           Util.displayCredentialsError(out, e);
        }
    }
    
    @Command(help = "Embed Jenkins into your OpenShift application", value="embed-jenkins")
    public void embedJenkins(PipeOut out,  @Option(name = "app", help = "Application name (alphanumeric - max 32 chars)") String name, @Option(name = "rhlogin", help = "Red Hat login (RHN or OpenShift login with OpenShift Express access)") String rhLogin) throws Exception {
        rhLogin = Util.getRhLogin(rhLogin, configuration, out, prompt);
        name = Util.getName(name, configuration, project, prompt);
        String password = Util.getPassword(prompt);
        String baseUrl = Util.getDefaultBaseUrl(out, configuration);
        IOpenShiftService openshift = OpenShiftServiceFactory.create(baseUrl);
        
        IUser user = new InternalUser(rhLogin, password, openshift);
        
        // Get the userInfo
        UserInfo userInfo = null;
        try {
            userInfo = openshift.getUserInfo(user);
        } catch (NotFoundOpenShiftException e) {
            Util.displayNonExistentDomainError(out, e);
            return;
        }
        
        if (jenkinsClientEmbedded(userInfo.getApplicationInfoByName(name), out)) {
           ShellMessages.error(out, "Jenkins is already embedded!");
           return;
        }
        
        String jenkinsAppName = findJenkinsApp(userInfo, out); 
        if (jenkinsAppName == null) {
           ShellMessages.info(out, "Adding \"jenkins\" application to domain");
           ShellMessages.info(out, "This is only needed if you haven't used Jenkins with OpenShift before");
           Util.createApplication(openshift, new JenkinsCartridge(openshift, user), user, "jenkins", out);
           ShellMessages.info(out, "Successfully added \"jenkins\" application to domain");
        }
        ShellMessages.info(out, "Embedding Jenkins client into application \"" + name + "\"");
        openshift.addEmbeddedCartridge(name, new JenkinsClientEmbeddableCartridge(openshift, user), user);
        ShellMessages.info(out, "Successfully embedded Jenkins into \"" + name + "\"");
        ShellMessages.info(out, "Any builds will now happen in Jenkins, available at http://" + jenkinsAppName + "-" + userInfo.getNamespace() + ".rhcloud.com" );
    }
    
    private boolean jenkinsClientEmbedded(ApplicationInfo applicationInfo, PipeOut out) {
       for (EmbeddableCartridgeInfo cartridge : applicationInfo.getEmbeddedCartridges()) {
          if (cartridge.getName().startsWith("jenkins"))
             return true;
       }
       return false;
    }
    
    private String findJenkinsApp(UserInfo info, PipeOut out) {
       for (ApplicationInfo app : info.getApplicationInfos()) {
          if (app.getCartridge().getName().startsWith("jenkins"))
             return app.getName();
       }
       return null;
    }


}