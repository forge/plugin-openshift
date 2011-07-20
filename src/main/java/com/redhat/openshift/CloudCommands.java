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
package com.redhat.openshift;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.jboss.forge.shell.ShellColor;
import org.jboss.forge.shell.plugins.Command;
import org.jboss.forge.shell.plugins.PipeOut;

import com.redhat.openshift.dao.CloudAccountDao;
import com.redhat.openshift.dao.exceptions.ConnectionException;
import com.redhat.openshift.dao.exceptions.InternalClientException;
import com.redhat.openshift.dao.exceptions.InvalidCredentialsException;
import com.redhat.openshift.dao.exceptions.OperationFailedException;
import com.redhat.openshift.dao.exceptions.ServerError;
import com.redhat.openshift.model.CloudAccount;
import com.redhat.openshift.model.Environment;
import com.redhat.openshift.utils.Formatter;

/**
 * @author <a href="mailto:kraman+forge@gmail.com">Krishna Raman</a>
 *  
 */
@Singleton
public class CloudCommands {
	@Inject private Provider<Openshift> base;
	@Inject private CloudAccountDao cloudAccountDao;
	@Inject private Formatter formatter;
	

	public void registerCloud(String in, PipeOut out, String cloudName, String cloudProvider, String account, 
			String credentials, String secretKey){
		String ssoCookie = base.get().getSsoCookie();
		
		CloudAccount cloudAccount=null;
		try{
			out.print("Creating cloud account...");
			cloudAccount = cloudAccountDao.registerCloud(ssoCookie, base.get().getFlexHost(), base.get().getFlexContext(), cloudName, cloudProvider, account, credentials, secretKey);
			out.println(ShellColor.GREEN, "[OK]");
			out.println();
		}catch (InternalClientException e) {
			out.println(ShellColor.RED,"Encountered an unexpected error while registering cloud account.");
			e.printStackTrace();			
		} catch (ConnectionException e) {
			out.println(ShellColor.RED,"Unable to connect to Flex server.");
			e.printStackTrace();			
		} catch (InvalidCredentialsException e) {
			out.println(ShellColor.RED,"Invalid credentials");
			e.printStackTrace();			
		} catch (OperationFailedException e) {
			out.println(ShellColor.RED,"Error occured while procesing the request: " + e.getMessage());
			e.printStackTrace();
		}
		
		if( cloudAccount != null ){
			formatter.printTable(new String[]{"Cloud Id", "Name", "Type"},
				 new String[]{"Id","Name","Type"},
				 new int[]{10,15,20},
				 cloudAccount, 0, out);
		}
        base.get().setLastCloudCreated(cloudAccount);
		base.get().updateCache(out);
	}
	
	public void listClouds(String in, PipeOut out){			
		String ssoCookie = base.get().getSsoCookie();
		
		try{
			out.println("Retrieving list of cloud accounts...");
			List<CloudAccount> list = cloudAccountDao.listClouds(ssoCookie, base.get().getFlexHost(), base.get().getFlexContext());
			for (CloudAccount cloudAccount : list) {
				formatter.printTable(new String[]{"Cloud Id", "Name", "Type"},
						 new String[]{"Id","Name","Type"},
						 new int[]{10,15,20},
						 cloudAccount, 0, out);
				List<Environment> environments = cloudAccount.getEnvironments();
				if(environments.size() > 0){
					out.println("    Environments:");
					try {
						formatter.printTable(new String[]{"Environment Id", "Name", "DNS", "Load Balanced?", "Location", "State"},
								 new String[]{"Id","Name","Dns", "LoadBalanced", "Location", "ClusterStatus"},
								 new int[]{16,15,40,20,15,10},
								 environments, 1, out);
					} catch (Exception e) {
						out.println(ShellColor.RED,"Encountered an unexpected error while listing cloud accounts.");
						e.printStackTrace();
					}
				}else{
					out.println("    No Environments for this cloud account");
				}
			}
		}catch (InternalClientException e) {
			out.println(ShellColor.RED,"Encountered an unexpected error.");
			e.printStackTrace();
		}catch (OperationFailedException e ){
			out.println(ShellColor.RED,"Unable to list cloud accounts.");
			e.printStackTrace();
		} catch (ConnectionException e) {
			out.println(ShellColor.RED,"Unable to connect to Flex server.");
			e.printStackTrace();
		} catch (InvalidCredentialsException e) {
			out.println(ShellColor.RED,"Invalid credentials");
			e.printStackTrace();			
		}
	}
	
	@Command("deregister-cloud")
	public void deregisterClouds(String in, PipeOut out, String cloudId){
				
		String ssoCookie = base.get().getSsoCookie();
		
		try{
			out.println("Retrieving list of cloud accounts...");
			List<CloudAccount> cloudList = cloudAccountDao.listClouds(ssoCookie, base.get().getFlexHost(), base.get().getFlexContext());
			List<CloudAccount> cloudsToDelete = new ArrayList<CloudAccount>();
			for (CloudAccount cloudAccount : cloudList) {
				if( cloudAccount.getName().equals(cloudId) || Long.toString(cloudAccount.getId()).equals(cloudId.trim()) ){
					cloudsToDelete.add(cloudAccount);
				}
			}
			if(cloudsToDelete.size()>1){
				out.println(ShellColor.RED,"Multiple cloud accounts share the name " + cloudId + ". Please provide the cloud ID");
				return;
			}
			if(cloudsToDelete.size()==0){
				out.println(ShellColor.RED,"Can not find cloud account identified by" + cloudId + ".");
				return;
			}
			
			CloudAccount accountToDelete = cloudsToDelete.get(0);
			
			out.println("Deleting cloud account " + accountToDelete.getName());
			cloudAccountDao.deleteCloud(ssoCookie, base.get().getFlexHost(), base.get().getFlexContext(), accountToDelete);
		}catch (InternalClientException e) {
			out.println(ShellColor.RED,"Encountered an unexpected error.");
			e.printStackTrace();
		}catch (OperationFailedException e ){
			out.println(ShellColor.RED,"Unable to delete cloud account" + cloudId + ".");
			e.printStackTrace();
		} catch (ConnectionException e) {
			out.println(ShellColor.RED,"Unable to connect to Flex server.");
			e.printStackTrace();
		} catch (InvalidCredentialsException e) {
			out.println(ShellColor.RED,"Invalid credentials");
			e.printStackTrace();			
		} catch (ServerError e) {
			out.println(ShellColor.RED,"Error occured while procesing the request: " + e.getMessage());
			e.printStackTrace();
		}
		base.get().updateCache(out);
	}
}
