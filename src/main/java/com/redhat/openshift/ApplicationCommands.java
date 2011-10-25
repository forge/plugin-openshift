/*
 *  Copyright 2010 Red Hat, Inc.
 *  
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may
 *  not use this file except in compliance with the License. You may obtain a
 *  copy of the License at
 *  
 *  	http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations
 *  under the License.
 */
package com.redhat.openshift;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.maven.model.Model;
import org.jboss.forge.maven.MavenCoreFacet;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.PackagingFacet;
import org.jboss.forge.resources.FileResource;
import org.jboss.forge.shell.Shell;
import org.jboss.forge.shell.ShellColor;
import org.jboss.forge.shell.plugins.PipeOut;

import com.redhat.openshift.dao.ApplicationDao;
import com.redhat.openshift.dao.EnvironmentDao;
import com.redhat.openshift.dao.exceptions.ConnectionException;
import com.redhat.openshift.dao.exceptions.InternalClientException;
import com.redhat.openshift.dao.exceptions.InvalidCredentialsException;
import com.redhat.openshift.dao.exceptions.OperationFailedException;
import com.redhat.openshift.dao.exceptions.UnsupportedEnvironmentVersionException;
import com.redhat.openshift.model.Application;
import com.redhat.openshift.model.Cartridge;
import com.redhat.openshift.model.Environment;
import com.redhat.openshift.model.Feature;
import com.redhat.openshift.utils.Formatter;

/**
 * @author <a href="mailto:kraman+forge@gmail.com">Krishna Raman</a>
 *  
 */
@Singleton
public class ApplicationCommands {
	@Inject private Project project;
	@Inject private Provider<Openshift> base;
	@Inject private Formatter formatter;
	@Inject private EnvironmentDao environmentDao;
	@Inject private ApplicationDao applicationDao;
	@Inject private Shell shell;

	public void listApplications(String in, PipeOut out){
		String ssoCookie = base.get().getSsoCookie();
		
		try{
			out.println("Retrieving list of environments...");
			List<Environment> list = environmentDao.listEnvironments(ssoCookie, base.get().getFlexHost(), base.get().getFlexContext());
			for (Environment environment : list) {
				if(!environment.getClusterStatus().equalsIgnoreCase("UNRESPONSIVE") &&
						!environment.getClusterStatus().equalsIgnoreCase("STOPPED")){
					formatter.printTable(new String[]{"Environment Id", "Name", "DNS", "Load Balanced?", "Location", "State"},
							 new String[]{"Id","Name","Dns", "LoadBalanced", "Location", "ClusterStatus"},
							 new int[]{16,15,40,20,15,10},
							 environment, 0, out);
					try{
						List<Application> applications = applicationDao.listApplications(environment);
						if(applications.size() > 0){
							out.println("    Applications:");
							formatter.printTable(new String[]{"GUID","Name", "Version", "State"},
								 new String[]{"Guid","Name","Version", "Status"},
								 new int[]{40,15,20,25},
								 applications, 1, out);
						}else{
							out.println("    No Application for this environment");						
						}
					}catch(UnsupportedEnvironmentVersionException e){
						out.println(ShellColor.RED,"Environment " + environment.getName() + " is running on an older version of the base system and cannot be accessed using this plugin");
					} catch (ConnectionException e){
						out.println(ShellColor.RED,"Unable to connect to environment " + environment.getName() + ". Connection timeout occured");
					}
				}
			}
		}catch (InternalClientException e) {
			out.println(ShellColor.RED,"Encountered an unexpected error. Do you have the latest openshift plugin?");
		}  catch (KeyManagementException e) {
			out.println(ShellColor.RED,"Error initlizing HTTPS.");
		} catch (NoSuchAlgorithmException e) {
			out.println(ShellColor.RED,"Error initlizing HTTPS.");
		} catch (Exception e) {
			out.println(ShellColor.RED,"Internal error. Do you have the latest openshift plugin?");
		}
	}
	
	public void stopApplication( String in, PipeOut out, String appId){
		String ssoCookie = base.get().getSsoCookie();
		
		if(appId == null){
			MavenCoreFacet maven = project.getFacet(MavenCoreFacet.class);
			Model pom = maven.getPOM();
			Properties props = pom.getProperties();
			appId = props.getProperty("com.openshift.application");
		}
		
		if(appId==null){
			out.println(ShellColor.RED,"Please run 'rhc setup' or specify the application to deploy to using the '--applicationId' flag.");
			return;
		}
		
		try{
			Application app = findApplication(in,out,ssoCookie,appId);
			if(app == null)
				return;
			
			if(app.getStatus().equals("CREATED")){
				out.println(ShellColor.RED,"Please run 'rhc deploy' to deploy a war/ear file before attempting to stop the application");
				return;
			}
			
			applicationDao.stopApplication(app.getEnvironment(), app);
		}catch (InternalClientException e) {
			out.println(ShellColor.RED,"Encountered an unexpected error. Do you have the latest openshift plugin?");	
		} catch (Exception e) {
			e.printStackTrace();
			out.println(ShellColor.RED,"Internal error. Do you have the latest openshift plugin?");
		}
	}
	
	public void startApplication( String in, PipeOut out, String appId){
		String ssoCookie = base.get().getSsoCookie();
		
		if(appId == null){
			MavenCoreFacet maven = project.getFacet(MavenCoreFacet.class);
			Model pom = maven.getPOM();
			Properties props = pom.getProperties();
			appId = props.getProperty("com.openshift.application");
		}
		
		if(appId==null){
			out.println(ShellColor.RED,"Please run 'rhc setup' or specify the application to deploy to using the '--applicationId' flag.");
			return;
		}
		
		try{
			Application app = findApplication(in,out,ssoCookie,appId);
			
			if(app == null)
				return;
			
			if(app.getStatus().equals("CREATED")){
				out.println(ShellColor.RED,"Please run 'rhc deploy' to deploy a war/ear file before attempting to start the application");
				return;
			}
				
			applicationDao.startApplication(app.getEnvironment(), app);
		}catch (InternalClientException e) {
			out.println(ShellColor.RED,"Encountered an unexpected error. Do you have the latest openshift plugin?");	
		} catch (Exception e) {
			e.printStackTrace();
			out.println(ShellColor.RED,"Internal error. Do you have the latest openshift plugin?");
		}
	}
	
	public void restartApplication( String in, PipeOut out, String appId){
		String ssoCookie = base.get().getSsoCookie();
		
		if(appId == null){
			MavenCoreFacet maven = project.getFacet(MavenCoreFacet.class);
			Model pom = maven.getPOM();
			Properties props = pom.getProperties();
			appId = props.getProperty("com.openshift.application");
		}
		
		if(appId==null){
			out.println(ShellColor.RED,"Please run 'rhc setup' or specify the application to deploy to using the '--applicationId' flag.");
			return;
		}
		
		try{
			Application app = findApplication(in,out,ssoCookie,appId);
			if(app == null)
				return;
			
			if(app.getStatus().equals("CREATED")){
				out.println(ShellColor.RED,"Please run 'rhc deploy' to deploy a war/ear file before attempting to start the application");
				return;
			}
			
			applicationDao.restartApplication(app.getEnvironment(), app);
		}catch (InternalClientException e) {
			out.println(ShellColor.RED,"Encountered an unexpected error. Do you have the latest openshift plugin?");	
		} catch (Exception e) {
			e.printStackTrace();
			out.println(ShellColor.RED,"Internal error. Do you have the latest openshift plugin?");
		}
	}
	
	public void deploy( String in, PipeOut out, String appId, Boolean withRestart){
		FileResource<?> finalArtifact = (FileResource<?>) project.getFacet(PackagingFacet.class).getFinalArtifact();
		if(!finalArtifact.exists()){
			out.println(ShellColor.RED,"Please build your applciation first.");
			return;
		}

		if(appId == null){
			MavenCoreFacet maven = project.getFacet(MavenCoreFacet.class);
			Model pom = maven.getPOM();
			Properties props = pom.getProperties();
			appId = props.getProperty("com.openshift.application");
		}
		
		if(appId==null){
			out.println(ShellColor.RED,"Please run 'rhc setup' or specify the application to deploy to using the '--applicationId' flag.");
			return;
		}
		
		String ssoCookie = base.get().getSsoCookie();
		try{
			Application app = findApplication(in,out,ssoCookie,appId);
			if(app == null)
				return;
			out.print("Deploying " + finalArtifact.getName() + " to application " + app.getName() + " on environment " + app.getEnvironment().getName() + "...");
			applicationDao.deployWar(app.getEnvironment(), app, finalArtifact.getUnderlyingResourceObject());
			out.println(ShellColor.GREEN,"[OK]");
			if(withRestart){
				shell.execute("rhc restart-application --applicationId " + app.getGuid());
			}
		}catch (InternalClientException e) {
			out.println(ShellColor.RED,"Encountered an unexpected error. Do you have the latest openshift plugin?");	
		} catch (Exception e) {
			e.printStackTrace();
			out.println(ShellColor.RED,"Internal error. Do you have the latest openshift plugin?");
		}
	}

	public void createApplication(String in, PipeOut out, String environmentId,
			String appName, String appVersion) {
		String ssoCookie = base.get().getSsoCookie();
		
		try{
			out.println("Retrieving list of environments...");
			List<Environment> environmentList = environmentDao.listEnvironments(ssoCookie, base.get().getFlexHost(), base.get().getFlexContext());
			List<Environment> candidates = new ArrayList<Environment>();
			for (Environment env : environmentList) {
				if( env.getName().equals(environmentId) || Long.toString(env.getId()).equals(environmentId.trim()) ){
					candidates.add(env);
				}
			}
			if(candidates.size()>1){
				out.println(ShellColor.RED,"Multiple environments share the name " + environmentId + ". Please provide the environment ID");
				return;
			}
			if(candidates.size()==0){
				out.println(ShellColor.RED,"Can not find environment identified by" + environmentId + ".");
				return;
			}
			Environment env = candidates.get(0);
			
			out.print("Creating application " + appName + " on environment " + env.getName() + "...");
			Application app = applicationDao.createApplication(env,appName,appVersion);
			out.println(ShellColor.GREEN, "[OK]");
			
			out.print("Configuring JBoss...");
			List<Cartridge> availableCartridges = applicationDao.getAvailableCartridges(env);
			List<Cartridge> appCartridges = applicationDao.getCartridges(env,app);
			
			List<String> requiredFeatures = new ArrayList<String>();
			requiredFeatures.add("jboss-as7");
			requiredFeatures.add("jdk6");
			
			for (Cartridge cartridge : availableCartridges) {
				ArrayList<Feature> provides = cartridge.getProvides();
				for (Feature feature : provides) {
					if(requiredFeatures.contains(feature.getName())){
						appCartridges.add(cartridge);
						break;
					}
				}
			}	
			
			applicationDao.setCartridges(env,app,appCartridges);
			out.println(ShellColor.GREEN, "[OK]");
			base.get().setLastApplicationCreated(app);
			out.println(ShellColor.GREEN,"Applicaton created succesfully.");
		}catch (InternalClientException e) {
			out.println(ShellColor.RED,"Encountered an unexpected error.");
		}catch (OperationFailedException e ){
			out.println(ShellColor.RED,"Unable to create application" + environmentId + ".");
		} catch (ConnectionException e) {
			out.println(ShellColor.RED,"Unable to connect to Flex server.");
			e.printStackTrace();
		} catch (InvalidCredentialsException e) {
			out.println(ShellColor.RED,"Invalid credentials");
			e.printStackTrace();
		}
		
	}

	public void deleteApplication(String in, PipeOut out, String appId) {
		String ssoCookie = base.get().getSsoCookie();
		
		if(appId == null){
			MavenCoreFacet maven = project.getFacet(MavenCoreFacet.class);
			Model pom = maven.getPOM();
			Properties props = pom.getProperties();
			appId = props.getProperty("com.openshift.application");
		}
		
		if(appId==null){
			out.println(ShellColor.RED,"Please run 'rhc setup' or specify the application to deploy to using the '--applicationId' flag.");
			return;
		}
		
		try{
			Application app = findApplication(in,out,ssoCookie,appId);
			if(app == null)
				return;
			applicationDao.deleteApplication(app.getEnvironment(), app);
		}catch (com.redhat.openshift.dao.exceptions.OperationFailedException e) {
			out.println(ShellColor.RED,"Unable to delete the application. Please stop the application first.");	
		}catch (InternalClientException e) {
			out.println(ShellColor.RED,"Encountered an unexpected error. Do you have the latest openshift plugin?");	
		} catch (Exception e) {
			e.printStackTrace();
			out.println(ShellColor.RED,"Internal error. Do you have the latest openshift plugin?");
		}
	}

	private Application findApplication(String in, PipeOut out, String ssoCookie, String appId) 
			throws ConnectionException, InternalClientException, InvalidCredentialsException, OperationFailedException {
		out.println("Retrieving list of environments...");
		List<Environment> list = environmentDao.listEnvironments(ssoCookie, base.get().getFlexHost(), base.get().getFlexContext());
		
		List<Application> candidates = new ArrayList<Application>();
		for (Environment environment : list) {
			if(!environment.getClusterStatus().equalsIgnoreCase("UNRESPONSIVE") &&
					!environment.getClusterStatus().equalsIgnoreCase("STOPPED")){
				try{
					List<Application> applications = applicationDao.listApplications(environment);
					for (Application application : applications) {
						if(application.getGuid().equals(appId) || application.getName().equals(appId)){
							application.setEnvironment(environment);
							candidates.add(application);
						}
					}
				}catch(UnsupportedEnvironmentVersionException e){
					//ignore
				}catch(ConnectionException e){
					//ignore
				}
			}
		}
		
		if(candidates.size()>1){
			out.println(ShellColor.RED,"Multiple applications share the name " + appId + ". Please provide the application GUID");
			return null;
		}
		
		if(candidates.size()==0){
			out.println(ShellColor.RED,"Can not find application identified by " + appId + ".");
			return null;
		}
		return candidates.get(0);
	}
}
