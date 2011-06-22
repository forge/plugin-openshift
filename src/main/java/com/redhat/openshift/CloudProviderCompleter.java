package com.redhat.openshift;

import javax.inject.Inject;

import org.jboss.forge.shell.Shell;
import org.jboss.forge.shell.completer.SimpleTokenCompleter;

public class CloudProviderCompleter extends SimpleTokenCompleter {

	@Inject
	private Shell shell;
	
	@Override
	public Iterable<?> getCompletionTokens() {
		return null;
	}

}
