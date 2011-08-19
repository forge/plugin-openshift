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

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.redhat.openshift.dao.exceptions.ConnectionException;
import com.redhat.openshift.dao.exceptions.InternalClientException;
import com.redhat.openshift.dao.exceptions.InvalidCredentialsException;
import com.redhat.openshift.dao.exceptions.OperationFailedException;
import com.redhat.openshift.dao.exceptions.UnsupportedEnvironmentVersionException;
import com.redhat.openshift.model.Application;
import com.redhat.openshift.model.Cartridge;
import com.redhat.openshift.model.Environment;
import com.redhat.openshift.model.Link;
import com.redhat.openshift.model.ResponseObject;

/**
 * @author <a href="mailto:kraman+forge@gmail.com">Krishna Raman</a>
 *  
 */
public class ApplicationDao extends RestDao{
	public List<Application> listApplications(Environment environment) 
			throws InternalClientException, ConnectionException, InvalidCredentialsException, OperationFailedException, UnsupportedEnvironmentVersionException {
		HttpHost targetHost = new HttpHost(environment.getDns(), 4242, "https"); 
		HttpRequestBase method = super.getHttpMethod(getHttpClient(), "", "GET", "/api");

        String usernameAndPassword = environment.getUsername() + ":" + environment.getPassword();
        String usernameAndPasswordEncoded = new String(Base64.encodeBase64(usernameAndPassword.getBytes()));
        method.addHeader("Authorization", "Basic "+ usernameAndPasswordEncoded);
		ResponseObject response = doHttp(getHttpClient(), targetHost, method, 200);
		if(response.getVersion() < 3.0f)
			throw new UnsupportedEnvironmentVersionException();
		
		Link link = response.getLinks().get("list-applications");
		method = super.getHttpMethod(getHttpClient(), "", link.getMethod(), link.getHref());
		method.addHeader("Authorization", "Basic "+ usernameAndPasswordEncoded);
		
		response = doHttp(getHttpClient(), targetHost, method, 200);
		List<Application> applications = response.getApplications();
		for (Application application : applications) {
			application.setEnvironment(environment);
		}
		return applications;
	}
	
	public void deleteApplication(Environment environment, Application app)
			throws ConnectionException, InternalClientException, InvalidCredentialsException, OperationFailedException{
		HttpHost targetHost = new HttpHost(environment.getDns(), 4242, "https"); 
		
		Link link = app.getLinks().get("delete");
		HttpRequestBase method = super.getHttpMethod(getHttpClient(), "", link.getMethod(), link.getHref());

        String usernameAndPassword = environment.getUsername() + ":" + environment.getPassword();
        String usernameAndPasswordEncoded = new String(Base64.encodeBase64(usernameAndPassword.getBytes()));
        method.addHeader("Authorization", "Basic "+ usernameAndPasswordEncoded);
        
		doHttp(getHttpClient(), targetHost, method, 200);
	}

	public void stopApplication(Environment environment, Application app)
			throws ConnectionException, InternalClientException, InvalidCredentialsException, OperationFailedException{
		HttpHost targetHost = new HttpHost(environment.getDns(), 4242, "https"); 
		
		Link link = app.getLinks().get("stop");
		HttpRequestBase method = super.getHttpMethod(getHttpClient(), "", link.getMethod(), link.getHref());

        String usernameAndPassword = environment.getUsername() + ":" + environment.getPassword();
        String usernameAndPasswordEncoded = new String(Base64.encodeBase64(usernameAndPassword.getBytes()));
        method.addHeader("Authorization", "Basic "+ usernameAndPasswordEncoded);
        
        List <NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("state", "stopped"));
        
        try {
			((HttpEntityEnclosingRequest)method).setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
		} catch (UnsupportedEncodingException e) {
			throw new InternalClientException(e);
		}
        
		doHttp(getHttpClient(), targetHost, method, 200);
	}

	public void startApplication(Environment environment, Application app)
			throws ConnectionException, InternalClientException, InvalidCredentialsException, OperationFailedException{
		HttpHost targetHost = new HttpHost(environment.getDns(), 4242, "https"); 
		
		Link link = app.getLinks().get("start");
		HttpRequestBase method = super.getHttpMethod(getHttpClient(), "", link.getMethod(), link.getHref());

        String usernameAndPassword = environment.getUsername() + ":" + environment.getPassword();
        String usernameAndPasswordEncoded = new String(Base64.encodeBase64(usernameAndPassword.getBytes()));
        method.addHeader("Authorization", "Basic "+ usernameAndPasswordEncoded);
        
        List <NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("state", "started"));
        
        try {
			((HttpEntityEnclosingRequest)method).setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
		} catch (UnsupportedEncodingException e) {
			throw new InternalClientException(e);
		}
        
		doHttp(getHttpClient(), targetHost, method, 200);
	}
	
	public void restartApplication(Environment environment, Application app)
			throws ConnectionException, InternalClientException, InvalidCredentialsException, OperationFailedException{
		HttpHost targetHost = new HttpHost(environment.getDns(), 4242, "https"); 
		
		Link link = null;
		if(app.getLinks().containsKey("restart"))
			link = app.getLinks().get("restart");
		else if(app.getLinks().containsKey("start")){
			startApplication(environment, app);
			return;
		}
		
		HttpRequestBase method = super.getHttpMethod(getHttpClient(), "", link.getMethod(), link.getHref());

        String usernameAndPassword = environment.getUsername() + ":" + environment.getPassword();
        String usernameAndPasswordEncoded = new String(Base64.encodeBase64(usernameAndPassword.getBytes()));
        method.addHeader("Authorization", "Basic "+ usernameAndPasswordEncoded);
        
        List <NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("operation", "restart"));
        
        try {
			((HttpEntityEnclosingRequest)method).setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
		} catch (UnsupportedEncodingException e) {
			throw new InternalClientException(e);
		}
        
		doHttp(getHttpClient(), targetHost, method, 200);
	}
	
	public void deployWar(Environment environment, Application app, File warFile)
			throws ConnectionException, InternalClientException, InvalidCredentialsException, OperationFailedException{
		HttpHost targetHost = new HttpHost(environment.getDns(), 4242, "https"); 
		String usernameAndPassword = environment.getUsername() + ":" + environment.getPassword();
		String usernameAndPasswordEncoded = new String(Base64.encodeBase64(usernameAndPassword.getBytes()));
		
		Link link = new Link("POST", app.getLinks().get("get").getHref() + "/tree/");
		HttpRequestBase method = super.getHttpMethod(getHttpClient(), "", link.getMethod(), link.getHref());

        method.addHeader("Authorization", "Basic "+ usernameAndPasswordEncoded);
        try {
        	MultipartEntity entity = new MultipartEntity();
        	entity.addPart("operation", new StringBody("extract", Charset.forName("UTF-8")));
        	FileBody fileBody = new FileBody(warFile);
        	entity.addPart("archive", fileBody);
        	((HttpEntityEnclosingRequest) method).setEntity(entity);
		} catch (UnsupportedEncodingException e) {
			throw new InternalClientException(e);
		}
		doHttp(getHttpClient(), targetHost, method, 200);
		
		link = app.getLinks().get("deploy");
		method = super.getHttpMethod(getHttpClient(), "", link.getMethod(), link.getHref());
        method.addHeader("Authorization", "Basic "+ usernameAndPasswordEncoded);
        List <NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("operation", "deploy"));
        try {
			((HttpEntityEnclosingRequest)method).setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
		} catch (UnsupportedEncodingException e) {
			throw new InternalClientException(e);
		}
		doHttp(getHttpClient(), targetHost, method, 200);
	}

	public Application createApplication(Environment environment, String appName, String appVersion) 
			throws ConnectionException, InternalClientException, InvalidCredentialsException, OperationFailedException {
		HttpHost targetHost = new HttpHost(environment.getDns(), 4242, "https"); 
		HttpRequestBase method = super.getHttpMethod(getHttpClient(), "", "GET", "/api");

        String usernameAndPassword = environment.getUsername() + ":" + environment.getPassword();
        String usernameAndPasswordEncoded = new String(Base64.encodeBase64(usernameAndPassword.getBytes()));
        method.addHeader("Authorization", "Basic "+ usernameAndPasswordEncoded);
		ResponseObject response = doHttp(getHttpClient(), targetHost, method, 200);
		
		Link link = response.getLinks().get("create-application");
		method = super.getHttpMethod(getHttpClient(), "", link.getMethod(), link.getHref());
        method.addHeader("Authorization", "Basic "+ usernameAndPasswordEncoded);
        
        List <NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("name", appName));
		nvps.add(new BasicNameValuePair("version", appVersion));
        
        try {
			((HttpEntityEnclosingRequest)method).setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
		} catch (UnsupportedEncodingException e) {
			throw new InternalClientException(e);
		}
        
		response = doHttp(getHttpClient(), targetHost, method, 201);
		return response.getApplication();
	}

	public List<Cartridge> getAvailableCartridges(Environment environment) 
			throws InternalClientException, ConnectionException, InvalidCredentialsException, OperationFailedException {
		HttpHost targetHost = new HttpHost(environment.getDns(), 4242, "https"); 
		HttpRequestBase method = super.getHttpMethod(getHttpClient(), "", "GET", "/api");

        String usernameAndPassword = environment.getUsername() + ":" + environment.getPassword();
        String usernameAndPasswordEncoded = new String(Base64.encodeBase64(usernameAndPassword.getBytes()));
        method.addHeader("Authorization", "Basic "+ usernameAndPasswordEncoded);
		ResponseObject response = doHttp(getHttpClient(), targetHost, method, 200);
		
		Link link = response.getLinks().get("list-cartridges");
		method = super.getHttpMethod(getHttpClient(), "", link.getMethod(), link.getHref());
        method.addHeader("Authorization", "Basic "+ usernameAndPasswordEncoded);
		response = doHttp(getHttpClient(), targetHost, method, 200);
		
		return response.getCartridges();
	}
	
	public List<Cartridge> getCartridges(Environment environment, Application app) 
			throws ConnectionException, InternalClientException, InvalidCredentialsException, OperationFailedException {
		HttpHost targetHost = new HttpHost(environment.getDns(), 4242, "https"); 

        String usernameAndPassword = environment.getUsername() + ":" + environment.getPassword();
        String usernameAndPasswordEncoded = new String(Base64.encodeBase64(usernameAndPassword.getBytes()));
		
		Link link = app.getLinks().get("list-cartridges");
		HttpRequestBase method = super.getHttpMethod(getHttpClient(), "", link.getMethod(), link.getHref()); 
        method.addHeader("Authorization", "Basic "+ usernameAndPasswordEncoded);
		ResponseObject response = doHttp(getHttpClient(), targetHost, method, 200);
		
		return response.getCartridges();
	}

	public void setCartridges(Environment environment, Application app, List<Cartridge> cartridges) 
			throws InternalClientException, ConnectionException, InvalidCredentialsException, OperationFailedException {
		HttpHost targetHost = new HttpHost(environment.getDns(), 4242, "https"); 

        String usernameAndPassword = environment.getUsername() + ":" + environment.getPassword();
        String usernameAndPasswordEncoded = new String(Base64.encodeBase64(usernameAndPassword.getBytes()));
		
        //Update control file and install new cartridges
		Link link = app.getLinks().get("update-cartridges");
		HttpRequestBase method = super.getHttpMethod(getHttpClient(), "", link.getMethod(), link.getHref()); 

		Gson gson = new GsonBuilder().serializeNulls().create();
		List <NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("cartridges", gson.toJson(cartridges)));
        try {
			((HttpEntityEnclosingRequest)method).setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
		} catch (UnsupportedEncodingException e) {
			throw new InternalClientException(e);
		}
        method.addHeader("Authorization", "Basic "+ usernameAndPasswordEncoded);
		@SuppressWarnings("unused")
		ResponseObject response = doHttp(getHttpClient(), targetHost, method, 200);
		
		//Commit configuration
		link = app.getLinks().get("files");
		//TODO: Hardcoded
		link.setMethod("POST");
		method = super.getHttpMethod(getHttpClient(), "", link.getMethod(), link.getHref() + "/.vostok/revisions");
		method.addHeader("Authorization", "Basic "+ usernameAndPasswordEncoded);
		nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("untracked", "true"));
        try {
			((HttpEntityEnclosingRequest)method).setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
		} catch (UnsupportedEncodingException e) {
			throw new InternalClientException(e);
		}
		response = doHttp(getHttpClient(), targetHost, method, 201);
	}
}
