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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import com.redhat.openshift.dao.exceptions.ConnectionException;
import com.redhat.openshift.dao.exceptions.InternalClientException;
import com.redhat.openshift.dao.exceptions.InvalidCredentialsException;

/**
 * @author <a href="mailto:kraman+forge@gmail.com">Krishna Raman</a>
 *  
 */
public class RhSsoDao extends RestDao{
	public String login(String loginServer, String login, String password) throws InternalClientException, InvalidCredentialsException, ConnectionException {
		HttpClient httpClient = this.getHttpClient();
		
		HttpPost httppost = new HttpPost(loginServer+"/wapps/streamline/login.html");
		List <NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("login", login));
        nvps.add(new BasicNameValuePair("password", password));
        HttpResponse postResponse = null;
        try {
			httppost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
			postResponse = httpClient.execute(httppost);
        } catch (UnsupportedEncodingException e) {
        	throw new InternalClientException(e);
		} catch (ClientProtocolException e) {
			throw new InternalClientException(e);
		} catch (IOException e) {
			throw new InternalClientException(e);
		}
		Header[] cookieHeaders = postResponse.getHeaders("Set-Cookie");
		if(cookieHeaders.length == 0){
			throw new InvalidCredentialsException();
		}
        
		String ssoCookie = (String) cookieHeaders[0].getValue();
		String cookieValue = ssoCookie.split(";")[0].split("=")[1];
		//System.out.println(cookieValue);
		return cookieValue;
	}
}
