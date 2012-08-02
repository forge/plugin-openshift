package com.redhat.openshift.core;

import com.openshift.client.IOpenShiftConnection;
import com.openshift.client.OpenShiftConnectionFactory;
import com.openshift.client.OpenShiftException;

public class OpenShiftServiceFactory {

   private static final String ID = "com.redhat.openshift.forge";
   private static final String DEFAULT_BASE_URL = "https://openshift.redhat.com";

   public static IOpenShiftConnection create(String username, String password, String baseUrl) throws OpenShiftException {
	   if (baseUrl == null) {
		   baseUrl = DEFAULT_BASE_URL;
	   }
	   
	   final IOpenShiftConnection service = new OpenShiftConnectionFactory().getConnection(
				ID,
				username,
				password,
				baseUrl);


	   // only verify SSL certificates when the default host is used
	   service.setEnableSSLCertChecks(DEFAULT_BASE_URL.equals(baseUrl));

       return service;
   }

//   public static IOpenShiftConnection create() {
//	   return create(DEFAULT_BASE_URL);
//   }
   
}
