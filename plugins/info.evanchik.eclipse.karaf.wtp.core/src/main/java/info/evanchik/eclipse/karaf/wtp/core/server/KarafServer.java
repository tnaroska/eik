/**
 * Copyright (c) 2009 Stephen Evanchik
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Stephen Evanchik - initial implementation
 */
package info.evanchik.eclipse.karaf.wtp.core.server;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.model.ServerDelegate;

/**
 * @author Stephen Evanchik (evanchsa@gmail.com)
 *
 */
public class KarafServer extends ServerDelegate {

    public KarafServer() {
        super();
    }

    /**
     * Determines whether or not the list of modules can be added and removed
     * from this server instance. A value of {@link Status#OK_STATUS} is
     * returned if the operation succeeds.
     *
     * @param add
     *            List of {@link IModule}S to be added to this server
     * @param remove
     *            List of {@link IModule}S to be removed from this server
     * @return {@Status#OK_STATUS} if the additions/removals
     *         are successful, {@link IStatus.ERROR} otherwise
     */
    @Override
    public IStatus canModifyModules(final IModule[] add, final IModule[] remove) {
        if (add != null) {

        }

        if (remove != null) {

        }

        return Status.OK_STATUS;
    }

    @Override
    public IModule[] getChildModules(final IModule[] module) {
        return null;
    }

    @Override
    public IModule[] getRootModules(final IModule module) throws CoreException {
        return new IModule[] { module };
    }

    /**
     * Performs the actual modification to the server by adding and/or removing
     * modules.
     */
    @Override
    public void modifyModules(final IModule[] add, final IModule[] remove, final IProgressMonitor monitor) throws CoreException {
        final IStatus status = canModifyModules(add, remove);
        if (status == null || !status.isOK()) {
            throw new CoreException(status);
        }

        if (add != null) {

        }

        if (remove != null) {

        }
    }

    @Override
    public String toString() {
        return "KarafServer"; //$NON-NLS-1$
    }
}
