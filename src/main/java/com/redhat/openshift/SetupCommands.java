package com.redhat.openshift;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.maven.model.Model;
import org.jboss.forge.maven.MavenCoreFacet;
import org.jboss.forge.project.Project;
import org.jboss.forge.shell.Shell;
import org.jboss.forge.shell.ShellColor;
import org.jboss.forge.shell.ShellPrompt;
import org.jboss.forge.shell.plugins.PipeOut;

import com.redhat.openshift.dao.ApplicationDao;
import com.redhat.openshift.dao.CloudAccountDao;
import com.redhat.openshift.dao.EnvironmentDao;
import com.redhat.openshift.dao.exceptions.ConnectionException;
import com.redhat.openshift.dao.exceptions.InternalClientException;
import com.redhat.openshift.dao.exceptions.InvalidCredentialsException;
import com.redhat.openshift.dao.exceptions.OperationFailedException;
import com.redhat.openshift.dao.exceptions.UnsupportedEnvironmentVersionException;
import com.redhat.openshift.model.Application;
import com.redhat.openshift.model.CloudAccount;
import com.redhat.openshift.model.Environment;
import com.redhat.openshift.utils.Formatter;

public class SetupCommands {

    @Inject
    private Provider<Openshift> base;
    @Inject
    private EnvironmentDao environmentDao;
    @Inject
    private Formatter formatter;
    @Inject
    private CloudAccountDao cloudAccountDao;
    @Inject
    private ApplicationDao applicationDao;
    @Inject
    private Shell shell;
    @Inject
    private ShellPrompt prompt;
    @Inject
    private Project project;

    private CloudAccount getCloudAccount(String in, PipeOut out,
	    List<CloudAccount> cloudList) {
	try {
	    if (cloudList.size() == 0) {
		out.println("You do not have any cloud accounts, creating a new one...");
		shell.execute("rhc-flex register-cloud");
		return base.get().getLastCloudCreated();
	    }

	    try {
		out.println("Choose a cloud account to use for the new environment");
		formatter.printTable(
			new String[] { "Cloud Id", "Name", "Type" },
			new String[] { "Id", "Name", "Type" }, new int[] { 10,
				15, 20 }, cloudList, 0, out);
	    } catch (Exception e) {
		// should not happen
		e.printStackTrace();
	    }
	    ArrayList<String> choices = new ArrayList<String>();
	    for (CloudAccount acc : cloudList) {
		choices.add(acc.getName() + " (id: " + acc.getId() + ")");
	    }
	    choices.add("New cloud account");
	    int choiceIdx = prompt.promptChoice(
		    "Choose a cloud account or N for a new cloud", choices);
	    if (choices.get(choiceIdx).equalsIgnoreCase("N")) {
		shell.execute("rhc-flex register-cloud");
		return base.get().getLastCloudCreated();
	    } else {
		return cloudList.get(choiceIdx);
	    }
	} catch (Exception e) {
	    throw new RuntimeException(e);
	}
    }

    private Environment createEnvironment(String in, PipeOut out,
	    List<CloudAccount> cloudList, CloudAccount lastCloudCreated) {
	try {
	    out.println("You do not have any environments, creating a new one...");
	    out.println();
	    if (lastCloudCreated != null) {
		shell.execute("rhc-flex create-environment --cloudId "
			+ lastCloudCreated.getId());
		return base.get().getLastEnvironmentCreated();
	    } else {
		CloudAccount cloudAccount = getCloudAccount(in, out, cloudList);
		return createEnvironment(in, out, cloudList, cloudAccount);
	    }
	} catch (Exception e) {
	    throw new RuntimeException(e);
	}
    }

    private Environment getEnvironment(String in, PipeOut out,
	    List<CloudAccount> cloudList, CloudAccount cloudAccount)
	    throws ConnectionException, InternalClientException,
	    InvalidCredentialsException, OperationFailedException {
	String ssoCookie = base.get().getSsoCookie();
	String flexHost = base.get().getFlexHost();
	String flexContext = base.get().getFlexContext();

	List<Environment> environments = environmentDao.listEnvironments(
		ssoCookie, flexHost, flexContext);
	if (environments.size() == 0) {
	    return createEnvironment(in, out, cloudList, cloudAccount);
	} else {
	    out.println("Choose an environment to host your application");
	    try {
		formatter.printTable(new String[] { "Environment Id", "Name",
			"DNS", "Load Balanced?", "Location", "State" },
			new String[] { "Id", "Name", "Dns", "LoadBalanced",
				"Location", "ClusterStatus" }, new int[] { 16,
				15, 40, 20, 15, 10 }, environments, 0, out);
	    } catch (Exception e) {
		// should not happen
		e.printStackTrace();
	    }

	    ArrayList<String> choices = new ArrayList<String>();
	    for (Environment env : environments) {
		if (env.getClusterStatus().equals("STARTED"))
		    choices.add(env.getName() + " (id: " + env.getId() + ")");
	    }
	    choices.add("New");
	    int choiceIdx = prompt.promptChoice(
		    "Choose an environment id or New for a new environment",
		    choices);
	    if (choices.get(choiceIdx).equalsIgnoreCase("N")) {
		return createEnvironment(in, out, cloudList, cloudAccount);
	    } else {
		return environments.get(choiceIdx);
	    }
	}
    }

    private Application getApplication(String in, PipeOut out,
	    Environment environment) throws InternalClientException,
	    ConnectionException, InvalidCredentialsException,
	    OperationFailedException {
	List<Application> applications = null;
	try {
	    applications = applicationDao.listApplications(environment);
	} catch (UnsupportedEnvironmentVersionException e) {
	    // ignore
	    return null;
	} catch (ConnectionException e) {
	    // ignore
	    return null;
	}

	if (applications.size() == 0) {
	    return createApplication(in, out, environment);
	} else {
	    try {
		formatter.printTable(new String[] { "GUID", "Name", "Version",
			"State" }, new String[] { "Guid", "Name", "Version",
			"Status" }, new int[] { 40, 15, 20, 25 }, applications,
			0, out);
	    } catch (Exception e) {
		// should not happen
		e.printStackTrace();
	    }

	    ArrayList<String> choices = new ArrayList<String>();
	    for (Application app : applications) {
		choices.add(app.getName() + " (Guid: " + app.getGuid() + ")");
	    }
	    choices.add("N");
	    int choiceIdx = prompt.promptChoice(
		    "Choose an application GUID or N for a new application",
		    choices);
	    if (choices.get(choiceIdx).equalsIgnoreCase("N")) {
		return createApplication(in, out, environment);
	    } else {
		return applications.get(choiceIdx);
	    }
	}
    }

    private Application createApplication(String in, PipeOut out,
	    Environment environment) {
	try {
	    out.println("You do not have any environments, creating a new one...");
	    out.println();
	    shell.execute("rhc-flex create-application --environmentId "
		    + environment.getId());
	    return base.get().getLastApplicationCreated();
	} catch (Exception e) {
	    throw new RuntimeException(e);
	}
    }

    public void setup(String in, PipeOut out, Properties rhcProperties) {
	String ssoCookie = base.get().getSsoCookie();
	String flexHost = base.get().getFlexHost();
	String flexContext = base.get().getFlexContext();

	if (!rhcProperties.containsKey("rhlogin")) {
	    if (prompt.promptBoolean("Remember login name?")) {
		base.get().saveRhcProperties("rhlogin",
			"\"" + base.get().getUsername() + "\"");
	    }
	}

	out.println("This wizard will help you set up a new cloud account, environment and application template for you Java EE application");
	out.println();
	try {
	    CloudAccount cloudAccount = null;
	    List<CloudAccount> cloudList = cloudAccountDao.listClouds(
		    ssoCookie, flexHost, flexContext);

	    // No Cloud account found, make one
	    if (cloudList.size() == 0) {
		cloudAccount = getCloudAccount(in, out, cloudList);
	    }

	    Environment environment = getEnvironment(in, out, cloudList,
		    cloudAccount);
	    Application application = getApplication(in, out, environment);

	    MavenCoreFacet maven = project.getFacet(MavenCoreFacet.class);
	    Model pom = maven.getPOM();
	    Properties props = pom.getProperties();
	    props.setProperty("com.openshift.application",
		    application.getGuid());
	    pom.setProperties(props);
	    maven.setPOM(pom);
	} catch (InternalClientException e) {
	    out.println(ShellColor.RED, "Encountered an unexpected error.");
	    e.printStackTrace();
	} catch (OperationFailedException e) {
	    out.println(ShellColor.RED, "Unable to list cloud accounts.");
	    e.printStackTrace();
	} catch (ConnectionException e) {
	    out.println(ShellColor.RED, "Unable to connect to Flex server.");
	    e.printStackTrace();
	} catch (InvalidCredentialsException e) {
	    out.println(ShellColor.RED, "Invalid credentials");
	    e.printStackTrace();
	}
    }

}
