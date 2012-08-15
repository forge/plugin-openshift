package com.redhat.openshift.forge;

import static org.jboss.forge.env.ConfigurationScope.PROJECT;

import java.io.FileReader;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.jboss.forge.env.Configuration;
import org.jboss.forge.env.ConfigurationScope;
import org.jboss.forge.shell.ShellMessages;
import org.jboss.forge.shell.ShellPrintWriter;
import org.jboss.forge.shell.project.ProjectScoped;

@ProjectScoped
public class OpenShiftConfiguration
{

   private static final String NAMESPACE = "forge.openshift";

   private enum Key
   {
      NAME,
      RHLOGIN
   }

   private Properties rhcProperties;

   @Inject
   private ShellPrintWriter out;

   @Inject
   private Configuration persistentConfiguration;

   @PostConstruct
   public void load()
   {
      rhcProperties = new Properties();
      try
      {
         rhcProperties.load(new FileReader(EXPRESS_CONF));
         ShellMessages.info(out, "Loaded OpenShift configuration from "
                  + EXPRESS_CONF);
      }
      catch (Exception e)
      {
         // Swallow
      }
   }

   public String getName()
   {
      return persistentConfiguration.getScopedConfiguration(PROJECT).getString(createKey(Key.NAME));
   }

   public void setName(String name)
   {
      this.persistentConfiguration.getScopedConfiguration(PROJECT).setProperty(createKey(Key.NAME), name);
   }

   public String getRhLogin()
   {
      return persistentConfiguration.getScopedConfiguration(ConfigurationScope.USER).getString(createKey(Key.RHLOGIN));
   }

   public void setRhLogin(String rhLogin)
   {
      persistentConfiguration.getScopedConfiguration(ConfigurationScope.USER).setProperty(createKey(Key.RHLOGIN),
               rhLogin);
   }

   private String createKey(Key key)
   {
      return NAMESPACE + "." + key.name();
   }

   private static final String EXPRESS_CONF = System.getProperty("user.home")
            + "/.openshift/express.conf";

   public Properties getRhcProperties()
   {
      return rhcProperties;
   }

}
