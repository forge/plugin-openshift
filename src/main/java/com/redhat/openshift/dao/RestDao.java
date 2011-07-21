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
import java.io.InputStreamReader;
import java.io.Reader;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import com.google.gson.Gson;
import com.redhat.openshift.dao.exceptions.ConnectionException;
import com.redhat.openshift.dao.exceptions.InternalClientException;
import com.redhat.openshift.dao.exceptions.InvalidCredentialsException;
import com.redhat.openshift.dao.exceptions.OperationFailedException;
import com.redhat.openshift.model.ResponseObject;

/**
 * @author <a href="mailto:kraman+forge@gmail.com">Krishna Raman</a>
 *  
 */
@SuppressWarnings("deprecation")
public abstract class RestDao {
	
	protected DefaultHttpClient getHttpClient() throws ConnectionException{
		DefaultHttpClient httpClient;
		try {
			SSLSocketFactory sslFactory = null;
			SSLContext ctx = SSLContext.getInstance("TLS");
			if(true){
			    X509TrustManager tm = new X509TrustManager() {

			        public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {
			        }

			        public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException {
			        }

			        public X509Certificate[] getAcceptedIssuers() {
			            return null;
			        }
			    };
				ctx.init(null, new TrustManager[]{tm}, null);
			}
			sslFactory = new SSLSocketFactory(ctx);
			sslFactory.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
			HttpParams params = new BasicHttpParams();
			ConnManagerParams.setMaxTotalConnections(params, 3);
			ConnManagerParams.setTimeout(params, 60000);
			SchemeRegistry schemeRegistry = new SchemeRegistry();
			schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
			schemeRegistry.register(new Scheme("https", sslFactory, 443));
			ClientConnectionManager cm = new ThreadSafeClientConnManager(params, schemeRegistry);
			httpClient = new DefaultHttpClient(cm, params);
			HttpParams httpParams = httpClient.getParams();
			HttpConnectionParams.setConnectionTimeout(httpParams, 20000);
			HttpConnectionParams.setSoTimeout(httpParams, 5*60*1000);
		} catch (Exception e) {
			throw new ConnectionException(e);
		}
	    
	    return httpClient;
	}
	
	protected HttpRequestBase getHttpMethod(HttpClient httpClient, String ssoToken,String httpMethod, String uri){
		HttpRequestBase method = null;
		if(httpMethod.equalsIgnoreCase("post"))
			method = new HttpPost(uri);
		if(httpMethod.equalsIgnoreCase("get"))
			method = new HttpGet(uri);
		if(httpMethod.equalsIgnoreCase("delete"))
			method = new HttpDelete(uri);
		if(httpMethod.equalsIgnoreCase("put"))
			method = new HttpPut(uri);
		method.setHeader("Cookie", "rh_sso=" + ssoToken);
		return method;
	}
	
	protected ResponseObject doHttp(HttpClient httpClient, HttpHost targetHost, HttpRequestBase method, int expectedCode)
			throws InternalClientException, ConnectionException, InvalidCredentialsException, OperationFailedException{
		HttpResponse response;
		try {
			//System.err.println("URI:" + targetHost + method.getURI());
			response = httpClient.execute(targetHost,method);
		} catch (ClientProtocolException e) {
			throw new InternalClientException(e);
		} catch (IOException e) {
			throw new ConnectionException(e);
		}
		int code = response.getStatusLine().getStatusCode();
		switch(code){
		case 401:
		case 302:
			throw new InvalidCredentialsException();
		}
		
		HttpEntity entity = response.getEntity();
		Reader in;
		if( entity == null )
			return null;
		
		//System.err.println("Http code:" + code);
		
		try {
			in = new InputStreamReader(entity.getContent());
			Gson gson = new Gson();
			ResponseObject obj = gson.fromJson(in, ResponseObject.class);
			if( code != expectedCode ){
				throw new OperationFailedException(obj.getMessage());
			}
			return obj; 
		} catch (IllegalStateException e) {
			throw new InternalClientException(e);
		} catch (IOException e) {
			throw new ConnectionException(e);
		}
	}
}
