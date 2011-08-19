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
package com.redhat.openshift.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.annotations.SerializedName;

/**
 * @author <a href="mailto:kraman+forge@gmail.com">Krishna Raman</a>
 *  
 */
public class ResponseObject {
	private int status;
	@SerializedName("cloud-account") private CloudAccount cloud;
	@SerializedName("cloud-accounts") private List<CloudAccount> cloudAccounts;
	@SerializedName("cloud-providers") private ArrayList<String> cloudProviders;
	@SerializedName("locations") private ArrayList<String> cloudLocations;
	
	@SerializedName("cluster") private Environment environment;
	@SerializedName("clusters") private List<Environment> environments;
	
	@SerializedName("cartridges") private List<Cartridge> cartridges;
	
	private Application application;
	private List<Application> applications;
	
	private Map<String,Link> links;
	
	private String message;
	private String version;
	
	public void setStatus(int status) {
		this.status = status;
	}
	public int getStatus() {
		return status;
	}
	public void setCloud(CloudAccount cloud) {
		this.cloud = cloud;
	}
	public CloudAccount getCloud() {
		return cloud;
	}
	public void setCloudAccounts(List<CloudAccount> cloudAccounts) {
		this.cloudAccounts = cloudAccounts;
	}
	public List<CloudAccount> getCloudAccounts() {
		return cloudAccounts;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}
	public Environment getEnvironment() {
		return environment;
	}
	public void setEnvironments(List<Environment> environments) {
		this.environments = environments;
	}
	public List<Environment> getEnvironments() {
		return environments;
	}
	public void setApplication(Application application) {
		this.application = application;
	}
	public Application getApplication() {
		return application;
	}
	public List<Application> getApplications() {
		return applications;
	}
	public void setApplications(List<Application> applications) {
		this.applications = applications;
	}
	public void setLinks(Map<String,Link> links) {
		this.links = links;
	}
	public Map<String,Link> getLinks() {
		return links;
	}
	public ArrayList<String> getCloudProviders() {
		return cloudProviders;
	}
	public ArrayList<String> getCloudLocations() {
		return cloudLocations;
	}
	public void setCloudLocations(ArrayList<String> cloudLocations) {
		this.cloudLocations = cloudLocations;
	}
	public void setCartridges(List<Cartridge> cartridges) {
		this.cartridges = cartridges;
	}
	public List<Cartridge> getCartridges() {
		return cartridges;
	}
	public Float getVersion() {
		return Float.parseFloat(version);
	}
	public void setVersion(String version) {
		this.version = version;
	}

	
}
