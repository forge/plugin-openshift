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

import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.jboss.forge.shell.ShellColor;
import org.jboss.forge.shell.ShellPrompt;
import org.jboss.forge.shell.plugins.PipeOut;

import com.redhat.openshift.dao.RhSsoDao;
import com.redhat.openshift.dao.exceptions.ConnectionException;
import com.redhat.openshift.dao.exceptions.InternalClientException;
import com.redhat.openshift.dao.exceptions.InvalidCredentialsException;

/**
 * @author <a href="mailto:kraman+forge@gmail.com">Krishna Raman</a>
 * 
 */
@Singleton
public class LoginCommands {
    @Inject
    private Provider<Openshift> base;
    @Inject
    private RhSsoDao ssoDao;

    public String login(String in, PipeOut out, Properties rhcProperties,
	    ShellPrompt prompt, String login, String password) {
	// check properties
	if (login == null || login.trim().equals("")) {
	    login = rhcProperties.getProperty("rhlogin", null);
	    if (login != null) {
		// remove extra quotes around login name (Express CLI seem to
		// add this)
		if (login.trim().startsWith("\"")) {
		    login = login.trim();
		    login = login.substring(1, login.length() - 1);
		}
	    }
	}

	// if still null, prompt user
	if (login == null || login.trim().equals("")) {
	    login = prompt.prompt("Login name");
	}

	if (password == null || password.trim().equals("")) {
	    password = prompt.promptSecret("Password");
	}

	String loginServer = rhcProperties.getProperty("login_server",
		"https://www.redhat.com");
	out.print(ShellColor.BOLD, "Logging into Openshift Flex as " + login
		+ "\n");
	try {
	    String ssoCookie = ssoDao.login(loginServer, login, password);
	    out.println(ShellColor.GREEN, "Logged in succesfully");
	    base.get().setUsername(login);
	    return ssoCookie;
	} catch (InvalidCredentialsException e) {
	    out.println(ShellColor.RED, "Invalid credentials");
	} catch (InternalClientException e) {
	    out.println(ShellColor.RED,
		    "Encountered an unexpected error while logging in.");
	} catch (ConnectionException e) {
	    out.println(ShellColor.RED, "Unable to connect to login server.");
	}
	return null;
    }
}
