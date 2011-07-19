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
import com.redhat.openshift.dao.exceptions.ServerError;
import com.redhat.openshift.model.CloudAccount;
import com.redhat.openshift.model.Link;
import com.redhat.openshift.model.ResponseObject;

/**
 * @author <a href="mailto:kraman+forge@gmail.com">Krishna Raman</a>
 *  
 */
public class CloudAccountDao extends RestDao{
	public List<CloudAccount> listClouds(String ssoToken, String flexHost, String flexContext)
			throws InternalClientException, ConnectionException, InvalidCredentialsException, OperationFailedException{
		HttpClient httpClient = super.getHttpClient();
		
		HttpHost targetHost = new HttpHost(flexHost, 443, "https");
		HttpRequestBase method = super.getHttpMethod(httpClient, ssoToken, "GET", flexContext + "/api");
		ResponseObject response = doHttp(httpClient, targetHost, method, 200);
		
		Link link = response.getLinks().get("list-cloud-accounts");
		method = super.getHttpMethod(httpClient, ssoToken, link.getMethod(),
				flexContext + link.getHref());
		response = doHttp(httpClient, targetHost, method, 200);
		return response.getCloudAccounts();
	}

	public CloudAccount registerCloud(String ssoToken, String flexHost, String flexContext,
			String cloudName, String cloudProvider, String account,
			String credentials, String secretKey) 
					throws InternalClientException, ConnectionException, InvalidCredentialsException, OperationFailedException{
		HttpClient httpClient = super.getHttpClient();	
		
		HttpHost targetHost = new HttpHost(flexHost, 443, "https");
		HttpRequestBase method = super.getHttpMethod(httpClient, ssoToken, "GET", flexContext + "/api");
		ResponseObject response = doHttp(httpClient, targetHost, method, 200);
		
		Link link = response.getLinks().get("create-cloud-account");
		method = super.getHttpMethod(httpClient, ssoToken, link.getMethod(),
				flexContext + link.getHref());
		
		List <NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("name", cloudName));
        nvps.add(new BasicNameValuePair("type", cloudProvider));
        nvps.add(new BasicNameValuePair("identity", credentials));
        nvps.add(new BasicNameValuePair("credentials", secretKey));
        nvps.add(new BasicNameValuePair("account-id", account));
    	try {
			((HttpPost)method).setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
		} catch (UnsupportedEncodingException e) {
			throw new InternalClientException(e);
		}

		response = doHttp(httpClient, targetHost, method, 200);
		return response.getCloud();
	}
	
	public void deleteCloud(String ssoToken, String flexHost, String flexContext, CloudAccount cloudAccount) 
			throws InternalClientException, OperationFailedException, ConnectionException, InvalidCredentialsException, ServerError{
		HttpClient httpClient = super.getHttpClient();
		
		HttpHost targetHost = new HttpHost(flexHost, 443, "https");
		
		Link link = cloudAccount.getLinks().get("delete");
		HttpRequestBase method = super.getHttpMethod(httpClient, ssoToken, link.getMethod(),
				flexContext + link.getHref());
		doHttp(httpClient, targetHost, method, 200);
	}

	public ArrayList<String> listSupportedCloudProviders(String ssoCookie, String flexHost, String flexContext) 
			throws ConnectionException, InternalClientException, InvalidCredentialsException, OperationFailedException {
		HttpClient httpClient = super.getHttpClient();
		
		HttpHost targetHost = new HttpHost(flexHost, 443, "https");
		HttpRequestBase method = super.getHttpMethod(httpClient, ssoCookie, "GET", flexContext + "/cloud-providers");
		ResponseObject response = doHttp(httpClient, targetHost, method, 200);
		return response.getCloudProviders();
	}
	
	public ArrayList<String> listSupportedCloudLocations(String ssoCookie, String flexHost, String flexContext, String cloudProvider) 
			throws ConnectionException, InternalClientException, InvalidCredentialsException, OperationFailedException {
		HttpClient httpClient = super.getHttpClient();
		
		HttpHost targetHost = new HttpHost(flexHost, 443, "https");
		HttpRequestBase method = super.getHttpMethod(httpClient, ssoCookie, "GET", flexContext + "/cloud-providers/" + cloudProvider + "/locations");
		ResponseObject response = doHttp(httpClient, targetHost, method, 200);
		return response.getCloudLocations();
	}
}
