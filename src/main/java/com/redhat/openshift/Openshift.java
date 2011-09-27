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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.forge.parser.java.util.Strings;
import org.jboss.forge.shell.Shell;
import org.jboss.forge.shell.ShellColor;
import org.jboss.forge.shell.ShellPrompt;
import org.jboss.forge.shell.plugins.Alias;
import org.jboss.forge.shell.plugins.Command;
import org.jboss.forge.shell.plugins.Option;
import org.jboss.forge.shell.plugins.PipeIn;
import org.jboss.forge.shell.plugins.PipeOut;

import com.redhat.openshift.completer.AppIdListCompleter;
import com.redhat.openshift.completer.CloudArchCompleter;
import com.redhat.openshift.completer.CloudIdListCompleter;
import com.redhat.openshift.completer.CloudProviderCompleter;
import com.redhat.openshift.completer.CloudRegionCompleter;
import com.redhat.openshift.completer.EnvIdListCompleter;
import com.redhat.openshift.dao.ApplicationDao;
import com.redhat.openshift.dao.CloudAccountDao;
import com.redhat.openshift.dao.EnvironmentDao;
import com.redhat.openshift.dao.exceptions.ConnectionException;
import com.redhat.openshift.dao.exceptions.InternalClientException;
import com.redhat.openshift.dao.exceptions.InvalidCredentialsException;
import com.redhat.openshift.dao.exceptions.OperationFailedException;
import com.redhat.openshift.model.Application;
import com.redhat.openshift.model.CloudAccount;
import com.redhat.openshift.model.Environment;

/**
 * @author <a href="mailto:kraman+forge@gmail.com">Krishna Raman</a>
 * 
 */
@Alias("rhc")
@Singleton
public class Openshift implements org.jboss.forge.shell.plugins.Plugin {
    @Inject
    private ShellPrompt prompt;
    @Inject
    private Shell shell;
    @Inject
    private CloudAccountDao cloudAccountDao;
    @Inject
    private EnvironmentDao environmentDao;
    @Inject
    private ApplicationDao applicationDao;

    @Inject
    private LoginCommands loginCommands;
    @Inject
    private CloudCommands cloudCommands;
    @Inject
    private EnvironmentCommands envCommands;
    @Inject
    private ApplicationCommands appCommands;
    @Inject
    private SetupCommands setupCommands;

    private final Properties rhcProperties;
    private String ssoCookie;
    private String flexHost;
    private String flexContext;

    protected List<CloudAccount> cachedCloudList;
    protected List<Environment> cachedEnvironmentList;
    protected List<Application> cachedApplicationList;
    protected ArrayList<String> supportedCloudProviders;
    protected ArrayList<String> supportedCloudRegions;

    // Used for setup wizard
    private String username;
    private CloudAccount lastCloudCreated;
    private Environment lastEnvironmentCreated;
    private Application lastApplicationCreated;

    static {
    }

    public Openshift() {
	rhcProperties = new Properties();
	ssoCookie = null;
	loadRhcProperties();
    }

    synchronized protected void updateCache(PipeOut out) {
	if (ssoCookie == null)
	    return;

	cachedApplicationList = new ArrayList<Application>();
	cachedCloudList = new ArrayList<CloudAccount>();
	cachedEnvironmentList = new ArrayList<Environment>();
	supportedCloudProviders = new ArrayList<String>();
	supportedCloudRegions = new ArrayList<String>();
	try {
	    cachedCloudList = cloudAccountDao.listClouds(ssoCookie, flexHost,
		    flexContext);
	    supportedCloudProviders = cloudAccountDao
		    .listSupportedCloudProviders(ssoCookie, flexHost,
			    flexContext);
	    for (String type : supportedCloudProviders) {
		supportedCloudRegions.addAll(cloudAccountDao
			.listSupportedCloudLocations(ssoCookie, flexHost,
				flexContext, type));
	    }
	    cachedEnvironmentList = environmentDao.listEnvironments(ssoCookie,
		    flexHost, flexContext);
	    for (Environment e : this.cachedEnvironmentList) {
		try {
		    this.cachedApplicationList.addAll(applicationDao
			    .listApplications(e));
		} catch (Exception ex) {
		    // out.println(ShellColor.CYAN,
		    // "Unable to load application data for environment: " +
		    // e.getName());
		    // ignore
		}
	    }
	} catch (InternalClientException e) {
	    out.println(ShellColor.RED, "Unable to load cache data");
	    e.printStackTrace();
	} catch (ConnectionException e) {
	    out.println(ShellColor.RED,
		    "Unable to connect to Openshift Flex server");
	    e.printStackTrace();
	} catch (InvalidCredentialsException e) {
	    out.println(ShellColor.RED,
		    "Your login credentials have expired. Please log in again.");
	    e.printStackTrace();
	} catch (OperationFailedException e) {
	    out.println(ShellColor.RED, "Unable to load cache data");
	    e.printStackTrace();
	}
    }

    private void loadRhcProperties() {
	try {
	    // load baked in properties
	    InputStream stream = Openshift.class.getClassLoader()
		    .getResourceAsStream("openshift.properties");
	    if (stream != null)
		rhcProperties.load(stream);
	    else
		System.err
			.println("Unable to load configuration from plugnin jar");
	} catch (IOException e) {
	    System.err.println("Unable to load configuration from plugnin jar");
	}
	try {
	    // load baked in properties
	    rhcProperties.load(new FileInputStream(new File(System
		    .getProperty("user.home")
		    + File.separator
		    + ".openshift"
		    + File.separator + "openshift.conf")));
	} catch (IOException e) {
	    System.err.println("Unable to load configuration from user home");
	} catch (SecurityException e) {
	    System.err.println("Unable to load configuration from user home");
	}

	String flexUriStr = rhcProperties.getProperty("flex_server").trim();
	if (!flexUriStr.startsWith("https"))
	    flexUriStr = "https://" + flexUriStr;

	URI flexUri = URI.create(flexUriStr);
	flexHost = flexUri.getHost();
	flexContext = flexUri.getPath() + "/rest";
    }

    protected void saveRhcProperties(String key, String value) {
	try {
	    rhcProperties.setProperty(key, value);
	    rhcProperties
		    .store(new FileOutputStream(new File(System
			    .getProperty("user.home")
			    + File.separator
			    + ".openshift" + File.separator + "openshift.conf")),
			    "Updated " + key);
	} catch (IOException e) {
	    System.err.println("Unable to save configuration");
	}
    }

    @Command("login")
    public void login(
	    @PipeIn String in,
	    PipeOut out,
	    @Option(name = "login", required = false, description = "Login name", shortName = "l") String login,
	    @Option(name = "password", required = false, description = "Password", shortName = "p") String password) {
	this.ssoCookie = loginCommands.login(in, out, rhcProperties, prompt,
		login, password);
	out.print("Preloading cache...");
	updateCache(out);
	out.println(ShellColor.GREEN, "[OK]");
    }

    @Command("register-cloud")
    public void registerCloud(
	    @PipeIn String in,
	    PipeOut out,
	    @Option(name = "name", required = true, description = "Name of the new cloud account") String cloudName,
	    @Option(name = "provider", completer = CloudProviderCompleter.class, required = true, description = "Cloud provider", defaultValue = "EC2") String cloudProvider,
	    @Option(name = "account", required = true, description = "Cloud provider account ID") String account,
	    @Option(name = "credentials", required = true, description = "Cloud provider credentials") String credentials,
	    @Option(name = "secrey-key", required = true, description = "Cloud provider secret key") String secretKey) {
	cloudCommands.registerCloud(in, out, cloudName, cloudProvider, account,
		credentials, secretKey);
    }

    @Command("list-clouds")
    public void listClouds(@PipeIn String in, PipeOut out) {
	cloudCommands.listClouds(in, out);
    }

    @Command("deregister-cloud")
    public void deregisterClouds(
	    @PipeIn String in,
	    PipeOut out,
	    @Option(name = "cloudId", required = true, description = "Name or ID of the cloud account to be deleted", completer = CloudIdListCompleter.class) String cloudId) {
	cloudCommands.deregisterClouds(in, out, cloudId);
    }

    @Command("list-environments")
    public void listEnvironments(@PipeIn String in, PipeOut out) {
	envCommands.listEnvironments(in, out);
    }

    @Command("create-environment")
    public void createEnvironment(
	    @PipeIn String in,
	    PipeOut out,
	    @Option(name = "name", required = true, description = "Name of the new environment") String environmentName,
	    @Option(name = "cloudId", required = true, description = "Name or ID of the cloud account to use", completer = CloudIdListCompleter.class) String cloudId,
	    @Option(name = "num-servers", required = true, defaultValue = "1", description = "Number of servers to start the environment with") String numNodes,
	    @Option(name = "location", required = true, defaultValue = "us-east-1", completer = CloudRegionCompleter.class, description = "Cloud location/region to create the environment in") String location,
	    @Option(name = "arch", required = true, completer = CloudArchCompleter.class, description = "VM architecture to use for this environment (32 or 64)") String architecture,
	    @Option(name = "load-balanced", required = true, flagOnly = true, defaultValue = "false", description = "Does the environment need a load balancer?") String isLoadBalanced,
	    @Option(name = "min-cores-per-node", required = true, defaultValue = "1", description = "Number of cores per server") String minCoresPerNode,
	    @Option(name = "min-volume-size-per-node", required = true, defaultValue = "10", description = "File system volume size in GB") String minVolumeSizePerNode,
	    @Option(name = "min-memory-per-node", required = true, defaultValue = "1024", description = "Minimum RAM per server") String minMemoryPerNode) {

	String adminPassword = null;
	while (Strings.isNullOrEmpty(adminPassword)) {
	    adminPassword = prompt
		    .promptSecret("Password for the admin user [required]:");
	}

	envCommands.createEnvironment(in, out, environmentName, adminPassword,
		cloudId, numNodes, location, architecture, isLoadBalanced,
		minCoresPerNode, minVolumeSizePerNode, minMemoryPerNode);
    }

    @Command("delete-environment")
    public void deleteEnvironment(
	    @PipeIn String in,
	    PipeOut out,
	    @Option(name = "environmentId", required = true, description = "Name or ID of the environment to be deleted", completer = EnvIdListCompleter.class) String environmentId) {
	envCommands.deleteEnvironment(in, out, environmentId);
    }

    @Command("stop-environment")
    public void stopEnvironment(
	    @PipeIn String in,
	    PipeOut out,
	    @Option(name = "environmentId", required = true, description = "Name or ID of the environment to be stopped", completer = EnvIdListCompleter.class) String environmentId) {
	envCommands.stopEnvironment(in, out, environmentId);
    }

    @Command("start-environment")
    public void startEnvironment(
	    @PipeIn String in,
	    PipeOut out,
	    @Option(name = "environmentId", required = true, description = "Name or ID of the environment to be started", completer = EnvIdListCompleter.class) String environmentId) {
	envCommands.startEnvironment(in, out, environmentId);
    }

    @Command("scale-up-environment")
    public void scaleUpEnvironment(
	    @PipeIn String in,
	    PipeOut out,
	    @Option(name = "environmentId", required = true, description = "Name or ID of the environment to be scaled up", completer = EnvIdListCompleter.class) String environmentId,
	    @Option(name = "num-servers", required = true, defaultValue = "1", description = "Number of servers to start the environment with") String numNodes,
	    @Option(name = "min-cores-per-node", required = true, defaultValue = "1", description = "Number of cores per server") String minCoresPerNode,
	    @Option(name = "min-volume-size-per-node", required = true, defaultValue = "10", description = "File system volume size in GB") String minVolumeSizePerNode,
	    @Option(name = "min-memory-per-node", required = true, defaultValue = "1024", description = "Minimum RAM per server") String minMemoryPerNode) {
	envCommands.scaleUpEnvironment(in, out, environmentId, numNodes,
		minCoresPerNode, minVolumeSizePerNode, minMemoryPerNode);
    }

    @Command("create-application")
    public void createApplication(
	    @PipeIn String in,
	    PipeOut out,
	    @Option(name = "environmentId", required = true, description = "Name or ID of the environment to be scaled up", completer = EnvIdListCompleter.class) String environmentId,
	    @Option(name = "name", required = true, description = "Application name") String appName,
	    @Option(name = "version", required = true, description = "Application version") String appVersion) {
	appCommands.createApplication(in, out, environmentId, appName,
		appVersion);
    }

    @Command("list-applications")
    public void listApplications(@PipeIn String in, PipeOut out) {
	appCommands.listApplications(in, out);
    }

    @Command("delete-application")
    public void deleteApplication(
	    @PipeIn String in,
	    PipeOut out,
	    @Option(name = "applicationId", required = false, description = "Name or ID of the application to delete", completer = AppIdListCompleter.class) String appId) {
	appCommands.deleteApplication(in, out, appId);
    }

    @Command("stop-application")
    public void stopApplication(
	    @PipeIn String in,
	    PipeOut out,
	    @Option(name = "applicationId", required = false, description = "Name or ID of the application to stop", completer = AppIdListCompleter.class) String appId) {
	appCommands.stopApplication(in, out, appId);
    }

    @Command("start-application")
    public void startApplication(
	    @PipeIn String in,
	    PipeOut out,
	    @Option(name = "applicationId", required = false, description = "Name or ID of the application to stop", completer = AppIdListCompleter.class) String appId) {
	appCommands.startApplication(in, out, appId);
    }

    @Command("restart-application")
    public void restartApplication(
	    @PipeIn String in,
	    PipeOut out,
	    @Option(name = "applicationId", required = false, description = "Name or ID of the application to restart", completer = AppIdListCompleter.class) String appId) {
	appCommands.restartApplication(in, out, appId);
    }

    @Command("deploy")
    public void deploy(
	    @PipeIn String in,
	    PipeOut out,
	    @Option(name = "restart", required = false, defaultValue = "false", flagOnly = true, description = "Resatrt the application after deploying it") Boolean withRestart,
	    @Option(name = "applicationId", required = false, description = "Name or ID of the application to deploy", completer = AppIdListCompleter.class) String appId) {
	appCommands.deploy(in, out, appId, withRestart);
    }

    @Command("setup")
    public void setup(@PipeIn String in, PipeOut out) {
	setupCommands.setup(in, out, rhcProperties);
    }

    protected String getSsoCookie() {
	if (this.ssoCookie == null)
	    try {
		shell.execute("rhc login");
	    } catch (Exception e) {
		throw new RuntimeException("Login failed with exception: "
			+ e.getMessage(), e);
	    }
	return ssoCookie;
    }

    protected String getFlexHost() {
	return flexHost;
    }

    protected String getFlexContext() {
	return flexContext;
    }

    public List<String> getSupportedCloudProviders() {
	return this.supportedCloudProviders;
    }

    public List<String> getSupportedCloudRegions() {
	return this.supportedCloudRegions;
    }

    public List<CloudAccount> getCachedCloudList() {
	return this.cachedCloudList;
    }

    public List<Environment> getCachedEnvironmentList() {
	return this.cachedEnvironmentList;
    }

    public List<Application> getCachedApplicationList() {
	return this.cachedApplicationList;
    }

    public void setLastCloudCreated(CloudAccount lastCloudCreated) {
	this.lastCloudCreated = lastCloudCreated;
    }

    public CloudAccount getLastCloudCreated() {
	return lastCloudCreated;
    }

    public void setLastEnvironmentCreated(Environment lastEnvironmentCreated) {
	this.lastEnvironmentCreated = lastEnvironmentCreated;
    }

    public Environment getLastEnvironmentCreated() {
	return lastEnvironmentCreated;
    }

    public void setLastApplicationCreated(Application lastApplicationCreated) {
	this.lastApplicationCreated = lastApplicationCreated;
	this.cachedApplicationList.add(lastApplicationCreated);
    }

    public Application getLastApplicationCreated() {
	return lastApplicationCreated;
    }

    public void setUsername(String username) {
	this.username = username;
    }

    public String getUsername() {
	return username;
    }
}
