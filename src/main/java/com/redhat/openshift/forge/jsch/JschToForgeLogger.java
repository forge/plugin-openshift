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

import org.jboss.forge.shell.Shell;
import org.jboss.forge.shell.ShellMessages;

/**
 * @author <a href="mailto:benevides@redhat.com">Rafael Benevides</a>
 * 
 */
public class JschToForgeLogger implements com.jcraft.jsch.Logger {

    @Inject
    private Shell shell;

    /*
     * (non-Javadoc)
     * 
     * @see com.jcraft.jsch.Logger#isEnabled(int)
     */
    @Override
    public boolean isEnabled(int level) {
        if (level == DEBUG || level == INFO) {
            return shell.isVerbose();
        }
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.jcraft.jsch.Logger#log(int, java.lang.String)
     */
    @Override
    public void log(int level, String message) {
        switch (level) {
            case DEBUG:
            	ShellMessages.info(shell, message);
            	break;
            case INFO:
                ShellMessages.info(shell, message);
                break;
            case WARN:
                ShellMessages.warn(shell, message);
                break;
            case ERROR:
            	ShellMessages.error(shell, message);
                break;
            case FATAL:
                ShellMessages.error(shell, message);
                break;
        }

    }
}
