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

import com.google.gson.annotations.SerializedName;

/**
 * @author <a href="mailto:kraman+forge@gmail.com">Krishna Raman</a>
 *  
 */
public class Cartridge {
	@SerializedName("component_id") private int componentId;
	private String vendor;
	private String name;
	private String description;
	private String version;
	private String build;
	private String filename;
	private String architecture;
	private String predicate;
	private Boolean isAvailable;
	private ArrayList<Feature> provides;
	private ArrayList<Feature> requires;
	private ArrayList<ArrayList<Feature>> depends;
	private ArrayList<ArrayList<Feature>> conflicts;
	
	public void setComponentId(int componentId) {
		this.componentId = componentId;
	}
	public int getComponentId() {
		return componentId;
	}
	public void setVendor(String vendor) {
		this.vendor = vendor;
	}
	public String getVendor() {
		return vendor;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getName() {
		return name;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getDescription() {
		return description;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public String getVersion() {
		return version;
	}
	public void setBuild(String build) {
		this.build = build;
	}
	public String getBuild() {
		return build;
	}
	public void setArchitecture(String architecture) {
		this.architecture = architecture;
	}
	public String getArchitecture() {
		return architecture;
	}
	public void setIsAvailable(Boolean isAvailable) {
		this.isAvailable = isAvailable;
	}
	public Boolean getIsAvailable() {
		return isAvailable;
	}
	public void setProvides(ArrayList<Feature> provides) {
		this.provides = provides;
	}
	public ArrayList<Feature> getProvides() {
		return provides;
	}
	public void setRequires(ArrayList<Feature> requires) {
		this.requires = requires;
	}
	public ArrayList<Feature> getRequires() {
		return requires;
	}
	public void setDepends(ArrayList<ArrayList<Feature>> depends) {
		this.depends = depends;
	}
	public ArrayList<ArrayList<Feature>> getDepends() {
		return depends;
	}
	public void setConflicts(ArrayList<ArrayList<Feature>> conflicts) {
		this.conflicts = conflicts;
	}
	public ArrayList<ArrayList<Feature>> getConflicts() {
		return conflicts;
	}
	public String getFilename() {
		return filename;
	}
	public void setFilename(String filename) {
		this.filename = filename;
	}
	public String getPredicate() {
		return predicate;
	}
	public void setPredicate(String predicate) {
		this.predicate = predicate;
	}
}
