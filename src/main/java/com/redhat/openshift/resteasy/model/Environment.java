package com.redhat.openshift.resteasy.model;

import java.util.List;

import javax.xml.bind.annotation.XmlElementWrapper;

import com.sun.xml.txw2.annotation.XmlElement;

@XmlElement
public class Environment {
	private long id;
	private String name;
	private String location;
	private String type;
	private String hostName;
	private String tag;
	private String username;
	private String password;
	private String creationDate;
	private String clusterStatus;
	private boolean loadBalanced;
	private String loadBalancerId;
	private String loadBalancerAddress;
	private String guid;
	private String dns;
	private int minCoresPerNode;
	private int minMemoryPerNode;
	private int minVolumeSizePerNode;
	private List<Server> nodes;
	private String cloudAccountName;
	
	@XmlElement
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	
	@XmlElement
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	@XmlElement
	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
	}
	
	@XmlElement
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	
	@XmlElement
	public String getHostName() {
		return hostName;
	}
	public void setHostName(String hostName) {
		this.hostName = hostName;
	}
	
	@XmlElement
	public String getTag() {
		return tag;
	}
	public void setTag(String tag) {
		this.tag = tag;
	}
	
	@XmlElement
	public String getUsername() {
		return username;
	}
	public void setUsername(String userName) {
		this.username = userName;
	}
	
	@XmlElement
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	
	@XmlElement
	public String getCreationDate() {
		return creationDate;
	}
	public void setCreationDate(String creationDate) {
		this.creationDate = creationDate;
	}
	
	@XmlElement
	public String getClusterStatus() {
		return clusterStatus;
	}
	public void setClusterStatus(String clusterStatus) {
		this.clusterStatus = clusterStatus;
	}
	
	@XmlElement
	public boolean isLoadBalanced() {
		return loadBalanced;
	}
	public void setLoadBalanced(boolean loadBalanced) {
		this.loadBalanced = loadBalanced;
	}
	
	@XmlElement
	public String getLoadBalancerId() {
		return loadBalancerId;
	}
	public void setLoadBalancerId(String loadBalancerId) {
		this.loadBalancerId = loadBalancerId;
	}
	
	@XmlElement
	public String getLoadBalancerAddress() {
		return loadBalancerAddress;
	}
	public void setLoadBalancerAddress(String loadBalancerAddress) {
		this.loadBalancerAddress = loadBalancerAddress;
	}
	
	@XmlElement
	public String getGuid() {
		return guid;
	}
	public void setGuid(String guid) {
		this.guid = guid;
	}
	
	@XmlElement
	public String getDns() {
		return dns;
	}
	public void setDns(String dns) {
		this.dns = dns;
	}
	
	@XmlElement
	public int getMinCoresPerNode() {
		return minCoresPerNode;
	}
	public void setMinCoresPerNode(int minCoresPerNode) {
		this.minCoresPerNode = minCoresPerNode;
	}
	
	@XmlElement
	public int getMinMemoryPerNode() {
		return minMemoryPerNode;
	}
	public void setMinMemoryPerNode(int minMemoryPerNode) {
		this.minMemoryPerNode = minMemoryPerNode;
	}
	
	@XmlElement
	public int getMinVolumeSizePerNode() {
		return minVolumeSizePerNode;
	}
	public void setMinVolumeSizePerNode(int minVolumeSizePerNode) {
		this.minVolumeSizePerNode = minVolumeSizePerNode;
	}
	
	@XmlElementWrapper(name="nodes")
	public List<Server> getNodes() {
		return nodes;
	}
	public void setNodes(List<Server> nodes) {
		this.nodes = nodes;
	}
	
	@XmlElement
	public String getCloudAccountName() {
		return cloudAccountName;
	}
	public void setCloudAccountName(String cloudAccountName) {
		this.cloudAccountName = cloudAccountName;
	}
}
