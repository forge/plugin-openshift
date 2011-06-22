package com.redhat.openshift;

import java.io.InputStream;

import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.PackagingFacet;
import org.jboss.forge.resources.FileResource;
import org.jboss.forge.resources.Resource;
import org.jboss.forge.shell.plugins.Plugin;
import org.jboss.forge.shell.plugins.Alias;
import org.jboss.forge.shell.ShellPrompt;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.forge.shell.plugins.Command;
import org.jboss.forge.shell.plugins.PipeIn;
import org.jboss.forge.shell.plugins.PipeOut;
import org.jboss.forge.shell.plugins.Option;
import org.jboss.forge.shell.plugins.DefaultCommand;

@Alias("openshift")
@Singleton
public class Openshift implements org.jboss.forge.shell.plugins.Plugin {
	@Inject
	private ShellPrompt prompt;
	
	@Inject
	private Project project;

	@Command("deploy")
	public void deploy(@PipeIn String in, PipeOut out,
			@Option(name="loginName") String loginName, 
			@Option(name="cloudProvider", completer=CloudProviderCompleter.class) String cloudProvider 
			)
	{
		String password = prompt.prompt("Enter your OpenShift password.");
		
		FileResource<?> finalArtifact = (FileResource<?>) project.getFacet(PackagingFacet.class).getFinalArtifact();
		finalArtifact.exists();
	}
	
	@Command
	public void undeploy(@PipeIn InputStream in, PipeOut out)
	{
		
	}
	
}
