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
package com.redhat.openshift.dao;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import com.redhat.openshift.dao.exceptions.ConnectionException;
import com.redhat.openshift.dao.exceptions.InternalClientException;
import com.redhat.openshift.dao.exceptions.InvalidCredentialsException;
import com.redhat.openshift.dao.exceptions.OperationFailedException;
import com.redhat.openshift.model.CloudAccount;
import com.redhat.openshift.model.Environment;
import com.redhat.openshift.model.Link;
import com.redhat.openshift.model.ResponseObject;

/**
 * @author <a href="mailto:kraman+forge@gmail.com">Krishna Raman</a>
 *  
 */
public class EnvironmentDao extends RestDao{
	public List<Environment> listEnvironments(String ssoToken, String flexHost, String flexContext) 
			throws ConnectionException, InternalClientException, InvalidCredentialsException, OperationFailedException{
		HttpClient httpClient = super.getHttpClient();
		
		HttpHost targetHost = new HttpHost(flexHost, 443, "https");
		HttpRequestBase method = super.getHttpMethod(httpClient, ssoToken, "GET", flexContext + "/api");
		ResponseObject response = doHttp(httpClient, targetHost, method, 200);
		
		Link link = response.getLinks().get("list-clusters");
		method = super.getHttpMethod(httpClient, ssoToken, link.getMethod(),
				flexContext + link.getHref());
		response = doHttp(httpClient, targetHost, method, 200);
		return response.getEnvironments();
	}

	public Environment createEnvironment(String ssoToken, String flexHost, String flexContext, 
			String environmentName, String adminPassword, CloudAccount cloudAccount, String location, 
			String architecture, String numNodes, String isLoadBalanced, 
			String minCoresPerNode, String minVolumeSizePerNode, String minMemoryPerNode) 
					throws ConnectionException, InternalClientException, InvalidCredentialsException, OperationFailedException {
		
		HttpClient httpClient = super.getHttpClient();	
		
		HttpHost targetHost = new HttpHost(flexHost, 443, "https");
		HttpRequestBase method = super.getHttpMethod(httpClient, ssoToken, "GET", flexContext + "/api");
		ResponseObject response = doHttp(httpClient, targetHost, method, 200);
		
		Link link = response.getLinks().get("create-cluster");
		method = super.getHttpMethod(httpClient, ssoToken, link.getMethod(),
				flexContext + link.getHref());
		
		List <NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("name", environmentName));
        nvps.add(new BasicNameValuePair("admin-password", adminPassword));
        nvps.add(new BasicNameValuePair("cloud-account-id", Long.toString(cloudAccount.getId())));
        nvps.add(new BasicNameValuePair("location", location));
        nvps.add(new BasicNameValuePair("architecture", architecture));
        nvps.add(new BasicNameValuePair("number-of-nodes", numNodes));
        nvps.add(new BasicNameValuePair("loadbalanced", isLoadBalanced));
        nvps.add(new BasicNameValuePair("min-cores-per-node", minCoresPerNode));
        nvps.add(new BasicNameValuePair("min-volume-size-per-node", minVolumeSizePerNode));
        nvps.add(new BasicNameValuePair("min-memory-per-node", minMemoryPerNode));
        
        try {
			((HttpPost)method).setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
		} catch (UnsupportedEncodingException e) {
			throw new InternalClientException(e);
		}

		response = doHttp(httpClient, targetHost, method, 200);
		return response.getEnvironment();
	}
	
	
	public void deleteEnvironment(String ssoToken, String flexHost, String flexContext, Environment environment) 
			throws ConnectionException, InternalClientException, InvalidCredentialsException, OperationFailedException{
		HttpClient httpClient = super.getHttpClient();
		
		HttpHost targetHost = new HttpHost(flexHost, 443, "https");
		
		Link link = environment.getLinks().get("delete");
		HttpRequestBase method = super.getHttpMethod(httpClient, ssoToken, link.getMethod(),
				flexContext + link.getHref());
		doHttp(httpClient, targetHost, method, 200);
	}
	
	public void stopEnvironment(String ssoToken, String flexHost, String flexContext, Environment environment) 
			throws ConnectionException, InternalClientException, InvalidCredentialsException, OperationFailedException {
		HttpClient httpClient = super.getHttpClient();
		
		HttpHost targetHost = new HttpHost(flexHost, 443, "https");
		
		Link link = environment.getLinks().get("stop");
		HttpRequestBase method = super.getHttpMethod(httpClient, ssoToken, /*link.getMethod()*/"POST",
				flexContext + link.getHref());
		
		List <NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("state", "stopped"));
        
        try {
			((HttpPost)method).setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
		} catch (UnsupportedEncodingException e) {
			throw new InternalClientException(e);
		}
		
		doHttp(httpClient, targetHost, method, 200);
	}
	
	public void startEnvironment(String ssoToken, String flexHost, String flexContext, Environment environment) 
			throws ConnectionException, InternalClientException, InvalidCredentialsException, OperationFailedException {
		HttpClient httpClient = super.getHttpClient();
		HttpHost targetHost = new HttpHost(flexHost, 443, "https");
		
		Link link = environment.getLinks().get("stop");
		HttpRequestBase method = super.getHttpMethod(httpClient, ssoToken, /*link.getMethod()*/"POST",
				flexContext + link.getHref());
		
		List <NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("state", "started"));
        
        try {
			((HttpPost)method).setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
		} catch (UnsupportedEncodingException e) {
			throw new InternalClientException(e);
		}
		
		doHttp(httpClient, targetHost, method, 200);
	}
	
	public Environment scaleUp(String ssoToken, String flexHost, String flexContext, Environment environment, 
			String numNodes, String minCoresPerNode, String minVolumeSizePerNode, String minMemoryPerNode) 
					throws ConnectionException, InternalClientException, InvalidCredentialsException, OperationFailedException{
		HttpClient httpClient = super.getHttpClient();
		HttpHost targetHost = new HttpHost(flexHost, 443, "https");
		
		Link link = environment.getLinks().get("scale-up");
		HttpRequestBase method = super.getHttpMethod(httpClient, ssoToken, link.getMethod(),
				flexContext + link.getHref());
		
		List <NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("number-of-nodes", numNodes));
        nvps.add(new BasicNameValuePair("min-cores-per-node", minCoresPerNode));
        nvps.add(new BasicNameValuePair("min-volume-size-per-node", minVolumeSizePerNode));
        nvps.add(new BasicNameValuePair("min-memory-per-node", minMemoryPerNode));
        
        try {
			((HttpPost)method).setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
		} catch (UnsupportedEncodingException e) {
			throw new InternalClientException(e);
		}

		ResponseObject response = doHttp(httpClient, targetHost, method, 200);
		return response.getEnvironment();
	}
}
