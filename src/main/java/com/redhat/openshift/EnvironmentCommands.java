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

import java.security.KeyManagementException;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.jboss.forge.shell.ShellColor;
import org.jboss.forge.shell.plugins.PipeOut;

import com.redhat.openshift.dao.ApplicationDao;
import com.redhat.openshift.dao.CloudAccountDao;
import com.redhat.openshift.dao.EnvironmentDao;
import com.redhat.openshift.dao.exceptions.ConnectionException;
import com.redhat.openshift.dao.exceptions.InternalClientException;
import com.redhat.openshift.dao.exceptions.InvalidCredentialsException;
import com.redhat.openshift.dao.exceptions.OperationFailedException;
import com.redhat.openshift.model.CloudAccount;
import com.redhat.openshift.model.Environment;
import com.redhat.openshift.model.Server;
import com.redhat.openshift.utils.Formatter;

/**
 * @author <a href="mailto:kraman+forge@gmail.com">Krishna Raman</a>
 *  
 */
@Singleton
public class EnvironmentCommands {
	@Inject private Provider<Openshift> base;
	@Inject private EnvironmentDao environmentDao;
	@Inject private Formatter formatter;
	@Inject private CloudAccountDao cloudAccountDao;
	@Inject private ApplicationDao applicationDao;

	public void listEnvironments(String in, PipeOut out){
		String ssoCookie = base.get().getSsoCookie();
		
		try{
			out.println("Retrieving list of environments...");
			List<Environment> list = environmentDao.listEnvironments(ssoCookie, base.get().getFlexHost(), base.get().getFlexContext());
			for (Environment environment : list) {
				formatter.printTable(new String[]{"Environment Id", "Name", "DNS", "Load Balanced?", "Location", "State"},
						 new String[]{"Id","Name","Dns", "LoadBalanced", "Location", "ClusterStatus"},
						 new int[]{16,15,40,20,15,10},
						 environment, 0, out);
				if( !environment.getClusterStatus().equalsIgnoreCase("stopped") && 
						!environment.getClusterStatus().equalsIgnoreCase("unresponsive")){
					List<Server> servers = environment.getNodes();
					out.println("    Servers:");
					formatter.printTable(new String[]{"Server Id","Name", "IP Address", "Provider Id", "State"},
						 new String[]{"Id","Name","IpAddress", "ProviderId", "Status"},
						 new int[]{16,15,20,25,10},
						 servers, 1, out);
				}
			}
		}catch (InternalClientException e) {
			e.printStackTrace();
			out.println(ShellColor.RED,"Encountered an unexpected error.");
		}  catch (KeyManagementException e) {
			e.printStackTrace();
			out.println(ShellColor.RED,"Error initlizing HTTPS.");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			out.println(ShellColor.RED,"Error initlizing HTTPS.");		
		} catch (Exception e) {
			e.printStackTrace();
			out.println(ShellColor.RED,"Encountered an unexpected error.");
		}
	}
	
	public void createEnvironment(String in, PipeOut out,
			String environmentName, String adminPassword, String cloudId, String numNodes, 
			String location, String architecture, String isLoadBalanced, String minCoresPerNode, 
			String minVolumeSizePerNode, String minMemoryPerNode){
				
		String ssoCookie = base.get().getSsoCookie();
		
		out.println("Retrieving list of cloud accounts...");
		List<CloudAccount> cloudList=null;
		try {
			cloudList = cloudAccountDao.listClouds(ssoCookie, base.get().getFlexHost(), base.get().getFlexContext());
		}catch (InternalClientException e) {
			out.println(ShellColor.RED,"Encountered an unexpected error. Do you have the latest openshift plugin?");
			e.printStackTrace();
			return;
		}catch (OperationFailedException e ){
			out.println(ShellColor.RED,"Unable to list cloud accounts.");
			e.printStackTrace();
			return;
		} catch (ConnectionException e) {
			out.println(ShellColor.RED,"Unable to connect to Flex server.");
			e.printStackTrace();
			return;
		} catch (InvalidCredentialsException e) {
			out.println(ShellColor.RED,"Invalid credentials");
			e.printStackTrace();
			return;
		}
		
		List<CloudAccount> candidates = new ArrayList<CloudAccount>();
		for (CloudAccount cloudAccount : cloudList) {
			if( cloudAccount.getName().equals(cloudId) || Long.toString(cloudAccount.getId()).equals(cloudId.trim()) ){
				candidates.add(cloudAccount);
			}
		}
		if(candidates.size()>1){
			out.println(ShellColor.RED,"Multiple cloud accounts share the name " + cloudId + ". Please provide the Cloud account ID");
			return;
		}
		if(candidates.size()==0){
			out.println(ShellColor.RED,"Can not find cloud account identified by" + cloudId + ".");
			return;
		}
		
		CloudAccount cloudAccount = candidates.get(0);
        Environment environment = null;
		try{
			out.print("Creating environment " + environmentName + "...");
			environment = environmentDao.createEnvironment(ssoCookie, base.get().getFlexHost(), base.get().getFlexContext(), environmentName, adminPassword, 
					cloudAccount, location, architecture, numNodes, isLoadBalanced, minCoresPerNode, 
					minVolumeSizePerNode, minMemoryPerNode);
			out.println(ShellColor.GREEN,"[OK]");
			out.println();
			
			formatter.printTable(new String[]{"Environment Id", "Name", "DNS", "Load Balanced?", "Location", "State"},
					 new String[]{"Id","Name","Dns", "LoadBalanced", "Location", "ClusterStatus"},
					 new int[]{16,15,40,20,15,10},
					 environment, 0, out);
		}catch (InternalClientException e) {
			out.println(ShellColor.RED,"Encountered an unexpected error. Do you have the latest openshift plugin?");
		} catch (Exception e) {
			out.println(ShellColor.RED,"Internal error. Do you have the latest openshift plugin?");
		}
		
		try{
			out.print("Waiting for DNS to propogate...");
			for(int i=0;i<10;i++){
				boolean pollOk = false;
				try{
					applicationDao.listApplications(environment);
					pollOk = true;
				}catch(Exception e){
					//ignore
				}
				if(pollOk)
					break;
				else{
					Thread.sleep(5000);
					out.print("...");
				}
			}
		}catch(Exception e){
			//ignore
		}
		out.println(".");
		
		base.get().updateCache(out);
        base.get().setLastEnvironmentCreated(environment);
	}
	
	public void deleteEnvironment(String in, PipeOut out, String environmentId){
		String ssoCookie = base.get().getSsoCookie();
		
		try{
			out.println("Retrieving list of environments...");
			List<Environment> environmentList = environmentDao.listEnvironments(ssoCookie, base.get().getFlexHost(), base.get().getFlexContext());
			List<Environment> candidates = new ArrayList<Environment>();
			for (Environment env : environmentList) {
				if( env.getName().equals(environmentId) || Long.toString(env.getId()).equals(environmentId.trim()) ){
					candidates.add(env);
				}
			}
			if(candidates.size()>1){
				out.println(ShellColor.RED,"Multiple environments share the name " + environmentId + ". Please provide the environment ID");
				return;
			}
			if(candidates.size()==0){
				out.println(ShellColor.RED,"Can not find environment identified by" + environmentId + ".");
				return;
			}
			Environment env = candidates.get(0);
			
			out.println("Deleting environment " + env.getName() + "...");
			environmentDao.deleteEnvironment(ssoCookie, base.get().getFlexHost(), base.get().getFlexContext(), env);
			out.println(ShellColor.GREEN,"Environment deleted succesfully.");
		}catch (InternalClientException e) {
			out.println(ShellColor.RED,"Encountered an unexpected error. Do you have the latest openshift plugin?");
			e.printStackTrace();
		}catch (OperationFailedException e ){
			out.println(ShellColor.RED,"Unable to delete environment" + environmentId + ".");
			e.printStackTrace();
		} catch (ConnectionException e) {
			out.println(ShellColor.RED,"Unable to connect to Flex server.");
			e.printStackTrace();
		} catch (InvalidCredentialsException e) {
			out.println(ShellColor.RED,"Invalid credentials");
			e.printStackTrace();
		}
		base.get().updateCache(out);
	}

	public void stopEnvironment(String in, PipeOut out, String environmentId){
				
		String ssoCookie = base.get().getSsoCookie();
		
		try{
			out.println("Retrieving list of environments...");
			List<Environment> environmentList = environmentDao.listEnvironments(ssoCookie, base.get().getFlexHost(), base.get().getFlexContext());
			List<Environment> candidates = new ArrayList<Environment>();
			for (Environment env : environmentList) {
				if( env.getName().equals(environmentId) || Long.toString(env.getId()).equals(environmentId.trim()) ){
					candidates.add(env);
				}
			}
			if(candidates.size()>1){
				out.println(ShellColor.RED,"Multiple environments share the name " + environmentId + ". Please provide the environment ID");
				return;
			}
			if(candidates.size()==0){
				out.println(ShellColor.RED,"Can not find environment identified by" + environmentId + ".");
				return;
			}
			Environment env = candidates.get(0);
			
			out.println("Stopping environment " + env.getName() + "...");
			environmentDao.stopEnvironment(ssoCookie, base.get().getFlexHost(), base.get().getFlexContext(), env);
			out.println(ShellColor.GREEN,"Environment stopped succesfully.");
		}catch (InternalClientException e) {
			out.println(ShellColor.RED,"Encountered an unexpected error. Do you have the latest openshift plugin?");
		}catch (OperationFailedException e ){
			out.println(ShellColor.RED,"Unable to stop environment" + environmentId + ".");
		} catch (ConnectionException e) {
			out.println(ShellColor.RED,"Unable to connect to Flex server.");
			e.printStackTrace();
		} catch (InvalidCredentialsException e) {
			out.println(ShellColor.RED,"Invalid credentials");
			e.printStackTrace();
		}
	}
	
	public void startEnvironment( String in, PipeOut out, String environmentId){
		String ssoCookie = base.get().getSsoCookie();
		
		try{
			out.println("Retrieving list of environments...");
			List<Environment> environmentList = environmentDao.listEnvironments(ssoCookie, base.get().getFlexHost(), base.get().getFlexContext());
			List<Environment> candidates = new ArrayList<Environment>();
			for (Environment env : environmentList) {
				if( env.getName().equals(environmentId) || Long.toString(env.getId()).equals(environmentId.trim()) ){
					candidates.add(env);
				}
			}
			if(candidates.size()>1){
				out.println(ShellColor.RED,"Multiple environments share the name " + environmentId + ". Please provide the environment ID");
				return;
			}
			if(candidates.size()==0){
				out.println(ShellColor.RED,"Can not find environment identified by" + environmentId + ".");
				return;
			}
			Environment env = candidates.get(0);
			
			out.println("Starting environment " + env.getName() + "...");
			environmentDao.startEnvironment(ssoCookie, base.get().getFlexHost(), base.get().getFlexContext(), env);
			out.println(ShellColor.GREEN,"Environment started succesfully.");
		}catch (InternalClientException e) {
			out.println(ShellColor.RED,"Encountered an unexpected error. Do you have the latest openshift plugin?");
		}catch (OperationFailedException e ){
			out.println(ShellColor.RED,"Unable to start environment" + environmentId + ".");
		} catch (ConnectionException e) {
			out.println(ShellColor.RED,"Unable to connect to Flex server.");
			e.printStackTrace();
		} catch (InvalidCredentialsException e) {
			out.println(ShellColor.RED,"Invalid credentials");
			e.printStackTrace();
		}
	}
	
	public void scaleUpEnvironment(String in, PipeOut out, String environmentId, String numNodes, 
			String minCoresPerNode, String minVolumeSizePerNode, String minMemoryPerNode){
				
		String ssoCookie = base.get().getSsoCookie();
		
		try{
			out.println("Retrieving list of environments...");
			List<Environment> environmentList = environmentDao.listEnvironments(ssoCookie, base.get().getFlexHost(), base.get().getFlexContext());
			List<Environment> candidates = new ArrayList<Environment>();
			for (Environment env : environmentList) {
				if( env.getName().equals(environmentId) || Long.toString(env.getId()).equals(environmentId.trim()) ){
					candidates.add(env);
				}
			}
			if(candidates.size()>1){
				out.println(ShellColor.RED,"Multiple environments share the name " + environmentId + ". Please provide the environment ID");
				return;
			}
			if(candidates.size()==0){
				out.println(ShellColor.RED,"Can not find environment identified by" + environmentId + ".");
				return;
			}
			Environment env = candidates.get(0);
			
			out.println("Scaling up environment " + env.getName() + " by " + numNodes + " servers...");
			environmentDao.scaleUp(ssoCookie, base.get().getFlexHost(), base.get().getFlexContext(), env, numNodes, minCoresPerNode, minVolumeSizePerNode, minMemoryPerNode);
			out.println(ShellColor.GREEN,"Environment scaled up succesfully.");
		}catch (InternalClientException e) {
			out.println(ShellColor.RED,"Encountered an unexpected error. Do you have the latest openshift plugin?");
		}catch (OperationFailedException e ){
			out.println(ShellColor.RED,"Unable to start environment" + environmentId + ".");
		} catch (ConnectionException e) {
			out.println(ShellColor.RED,"Unable to connect to Flex server.");
			e.printStackTrace();
		} catch (InvalidCredentialsException e) {
			out.println(ShellColor.RED,"Invalid credentials");
			e.printStackTrace();
		}
	}
}
