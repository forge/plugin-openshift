package com.redhat.openshift.forge;

import static org.jboss.forge.env.ConfigurationScope.PROJECT;
import static org.jboss.forge.env.ConfigurationScope.USER;

import java.io.FileReader;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.jboss.forge.env.Configuration;
import org.jboss.forge.shell.ShellMessages;
import org.jboss.forge.shell.ShellPrintWriter;
import org.jboss.forge.shell.project.ProjectScoped;

@ProjectScoped
public class OpenShiftConfiguration {

    private static final String NAMESPACE = "forge.openshift";

    private enum Key {
        NAME, RHLOGIN
    }

    private Properties rhcProperties;

    @Inject
    private ShellPrintWriter out;

    @Inject
    private Configuration persistentConfiguration;
   
    @PostConstruct
    public void load() {
        rhcProperties = new Properties();
        try {
	    FileReader reader = null;
	    try {
		reader = new FileReader(EXPRESS_CONF);
		rhcProperties.load(reader);
	    } finally {
		reader.close();
	    }
            ShellMessages.info(out, "Loaded OpenShift configuration from " + EXPRESS_CONF);
        } catch (Exception e) {
            // Swallow
        }
    }

    public String getName() {
        return persistentConfiguration.getScopedConfiguration(PROJECT).getString(createKey(Key.NAME));
    }

    public void setName(String name) {
        this.persistentConfiguration.getScopedConfiguration(PROJECT).setProperty(createKey(Key.NAME), name);
    }

    public String getRhLogin() {
    	String rhlogin = Util.unquote(getRhcProperties().getProperty("default_rhlogin"));
    	
    	if (rhlogin == null || rhlogin.trim().length() == 0)
    		rhlogin = persistentConfiguration.getScopedConfiguration(USER).getString(createKey(Key.RHLOGIN));
    	
    	return rhlogin;
    }

    public void setRhLogin(String rhLogin) {
        persistentConfiguration.getScopedConfiguration(USER).setProperty(createKey(Key.RHLOGIN), rhLogin);
    }

    private String createKey(Key key) {
        return NAMESPACE + "." + key.name();
    }

    private static final String EXPRESS_CONF = System.getProperty("user.home") + "/.openshift/express.conf";

    public Properties getRhcProperties() {
        return rhcProperties;
    }

}
