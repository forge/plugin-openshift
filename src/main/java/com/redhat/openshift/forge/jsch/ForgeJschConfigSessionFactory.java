/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package com.redhat.openshift.forge.jsch;

import javax.inject.Inject;

import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig.Host;
import org.eclipse.jgit.util.FS;

import com.jcraft.jsch.IdentityRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.agentproxy.AgentProxyException;
import com.jcraft.jsch.agentproxy.Connector;
import com.jcraft.jsch.agentproxy.ConnectorFactory;
import com.jcraft.jsch.agentproxy.RemoteIdentityRepository;

/**
 * @author <a href="mailto:benevides@redhat.com">Rafael Benevides</a>
 * 
 */
public class ForgeJschConfigSessionFactory extends JschConfigSessionFactory {

    @Inject
    private ForgeUserInfo forgeUserInfo;

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.jgit.transport.JschConfigSessionFactory#configure(org.eclipse
     * .jgit.transport.OpenSshConfig.Host, com.jcraft.jsch.Session)
     */
    @Override
    protected void configure(Host hc, Session session) {
    	
    	try {
	        session.setUserInfo(forgeUserInfo);
	        
	        JSch jsch = this.getJSch(hc, FS.DETECTED);
	        JSch.setConfig("PreferredAuthentications", "publickey");

	    Connector con;
	    try {
		ConnectorFactory cf = ConnectorFactory.getDefault();
		con = cf.createConnector();
		if (con != null) {
		    IdentityRepository irepo = new RemoteIdentityRepository(con);
		    jsch.setIdentityRepository(irepo);
		}
	    } catch (AgentProxyException e) {
		System.out.println(e);
	    }
	        //Properties config = new Properties();
	        //config.put("StrictHostKeyChecking", "no");
			//config.put("GSSAPIAuthentication", "no");
			//config.put("PreferredAuthentications", "publickey");
			//jsch.setConfig(config);
			
    	} catch (Exception e){
    		e.printStackTrace();
    	}
    }
}
