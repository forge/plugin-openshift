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

import java.util.List;
import java.util.Map;

import com.google.gson.annotations.SerializedName;

/**
 * @author <a href="mailto:kraman+forge@gmail.com">Krishna Raman</a>
 *  
 */
public class Environment implements RestObject{
	private long id;
	private String name;
	private String location;
	private String type;
	@SerializedName("ip-address") private String hostName;
	private String tag;
	private String username;
	private String password;
	private String creationDate;
	@SerializedName("cluster-status") private String clusterStatus;
	@SerializedName("loadbalanced") private boolean loadBalanced;
	private String guid;
	private String dns;
	@SerializedName("min-cores-per-node") private int minCoresPerNode;
	@SerializedName("min-cores-per-node") private int minMemoryPerNode;
	@SerializedName("min-volume-size-per-node") private int minVolumeSizePerNode;
	private List<Server> nodes;
	@SerializedName("cloud-account-name") private String cloudAccountName;
	private Map<String,Link> links;	
	
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
	}
	
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	
	public String getHostName() {
		return hostName;
	}
	public void setHostName(String hostName) {
		this.hostName = hostName;
	}
	
	public String getTag() {
		return tag;
	}
	public void setTag(String tag) {
		this.tag = tag;
	}
	
	public String getUsername() {
		return username;
	}
	public void setUsername(String userName) {
		this.username = userName;
	}
	
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	
	public String getCreationDate() {
		return creationDate;
	}
	public void setCreationDate(String creationDate) {
		this.creationDate = creationDate;
	}
	
	public String getClusterStatus() {
		return clusterStatus;
	}
	public void setClusterStatus(String clusterStatus) {
		this.clusterStatus = clusterStatus;
	}
	
	public boolean isLoadBalanced() {
		return loadBalanced;
	}
	public boolean getLoadBalanced() {
		return loadBalanced;
	}
	public void setLoadBalanced(boolean loadBalanced) {
		this.loadBalanced = loadBalanced;
	}
	
	public String getGuid() {
		return guid;
	}
	public void setGuid(String guid) {
		this.guid = guid;
	}
	
	public String getDns() {
		return dns;
	}
	public void setDns(String dns) {
		this.dns = dns;
	}
	
	public int getMinCoresPerNode() {
		return minCoresPerNode;
	}
	public void setMinCoresPerNode(int minCoresPerNode) {
		this.minCoresPerNode = minCoresPerNode;
	}
	
	public int getMinMemoryPerNode() {
		return minMemoryPerNode;
	}
	public void setMinMemoryPerNode(int minMemoryPerNode) {
		this.minMemoryPerNode = minMemoryPerNode;
	}
	
	public int getMinVolumeSizePerNode() {
		return minVolumeSizePerNode;
	}
	public void setMinVolumeSizePerNode(int minVolumeSizePerNode) {
		this.minVolumeSizePerNode = minVolumeSizePerNode;
	}
	
	public List<Server> getNodes() {
		return nodes;
	}
	public void setNodes(List<Server> nodes) {
		this.nodes = nodes;
	}
	
	public String getCloudAccountName() {
		return cloudAccountName;
	}
	public void setCloudAccountName(String cloudAccountName) {
		this.cloudAccountName = cloudAccountName;
	}
	public void setLinks(Map<String,Link> links) {
		this.links = links;
	}
	public Map<String,Link> getLinks() {
		return links;
	}
}
