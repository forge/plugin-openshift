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
package com.redhat.openshift.completer;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.jboss.forge.shell.completer.SimpleTokenCompleter;

import com.redhat.openshift.Openshift;
import com.redhat.openshift.model.Environment;

/**
 * @author <a href="mailto:kraman+forge@gmail.com">Krishna Raman</a>
 *  
 */
public class EnvIdListCompleter extends SimpleTokenCompleter {
	
	@Inject
	private Openshift base;
	
	@Override
	public Iterable<?> getCompletionTokens() {
		List<String> environments = new ArrayList<String>();
		for (Environment e : base.getCachedEnvironmentList()) {
			environments.add(e.getName());
			environments.add(Long.toString(e.getId()));
		}
		return environments;
	}
}