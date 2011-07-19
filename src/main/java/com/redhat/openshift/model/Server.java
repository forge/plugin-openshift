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

import java.util.Map;

import com.google.gson.annotations.SerializedName;

/**
 * @author <a href="mailto:kraman+forge@gmail.com">Krishna Raman</a>
 *  
 */
public class Server {
	private String name;
	private Long id;
	@SerializedName("ip-address") private String ipAddress;
	private String status;
	@SerializedName("provider-id") private String providerId;
	@SerializedName("cluster-id") private Long clusterId;
	private Map<String, Link> links;
	
	public void setName(String name) {
		this.name = name;
	}
	public String getName() {
		return name;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Long getId() {
		return id;
	}
	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}
	public String getIpAddress() {
		return ipAddress;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getStatus() {
		return status;
	}
	public void setProviderId(String providerId) {
		this.providerId = providerId;
	}
	public String getProviderId() {
		return providerId;
	}
	public void setClusterId(Long clusterId) {
		this.clusterId = clusterId;
	}
	public Long getClusterId() {
		return clusterId;
	}
	public void setLinks(Map<String, Link> links) {
		this.links = links;
	}
	public Map<String, Link> getLinks() {
		return links;
	}
}
