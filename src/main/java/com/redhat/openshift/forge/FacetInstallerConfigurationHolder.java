package com.redhat.openshift.forge;

import org.jboss.forge.shell.project.ProjectScoped;

@ProjectScoped
public class FacetInstallerConfigurationHolder {

    private String rhLogin;
    private String name;
    private String gitRemoteRepo;
    private boolean scaling;

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

    public String getGitRemoteRepo() {
        return gitRemoteRepo;
    }

    public void setGitRemoteRepo(String gitRemoteRepo) {
        this.gitRemoteRepo = gitRemoteRepo;
    }

    public boolean isScaling() {
        return scaling;
    }

    public void setScaling(boolean scaling) {
        this.scaling = scaling;
    }

    public void clear() {
        this.name = null;
        this.rhLogin = null;
        this.gitRemoteRepo = null;
        this.scaling = false;
    }

}
