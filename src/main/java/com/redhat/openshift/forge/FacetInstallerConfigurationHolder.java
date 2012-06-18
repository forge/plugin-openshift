package com.redhat.openshift.forge;

import org.jboss.forge.shell.project.ProjectScoped;

@ProjectScoped
public class FacetInstallerConfigurationHolder {
   
   private String rhLogin;
   private String name;
   
   public String getRhLogin() {
      return rhLogin;
   }
   public void setRhLogin(String rhLogin) {
      this.rhLogin = rhLogin;
   }
   public String getName() {
      return name;
   }
   public void setName(String name) {
      this.name = name;
   }
   
   public void clear() {
      this.name = null;
      this.rhLogin = null;
   }

}
