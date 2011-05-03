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
package info.evanchik.eclipse.karaf.wtp.core.runtime;

import info.evanchik.eclipse.karaf.core.KarafPlatformModel;
import info.evanchik.eclipse.karaf.core.KarafPlatformModelRegistry;
import info.evanchik.eclipse.karaf.wtp.core.KarafWtpPluginActivator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.model.RuntimeDelegate;

/**
 * @author Stephen Evanchik (evanchsa@gmail.com
 *
 */
public class KarafRuntime extends RuntimeDelegate {

    @Override
    protected void initialize() {
        super.initialize();
    }

    @Override
    public void setDefaults(final IProgressMonitor monitor) {
        super.setDefaults(monitor);
    }

    /**
     * Determines whether or not this is a valid {@link IRuntime} of a Karaf
     * installation.
     *
     * @return a {@link IStatus} object indicating whether or not this is a
     *         valid Karaf runtime. A valid Karaf Runtime will return
     *         {@link Status#OK_STATUS} otherwise a status based on
     *         {@link IStatus#ERROR}
     */
    @Override
    public IStatus validate() {
        final IPath location = getRuntime().getLocation();

        if (location == null || location.isEmpty()) {
            return new Status(IStatus.ERROR, KarafWtpPluginActivator.PLUGIN_ID, 0, "", null);
        }

        final IStatus status = super.validate();
        if (!status.isOK()) {
            return status;
        }

        KarafPlatformModel karafTargetPlatform;
        try {
            karafTargetPlatform = KarafPlatformModelRegistry.findPlatformModel(location);
            if (karafTargetPlatform != null) {
                return Status.OK_STATUS;
            } else {
                return new Status(
                        IStatus.ERROR,
                        KarafWtpPluginActivator.PLUGIN_ID,
                        0,
                        "Unable to validate Karaf installation",
                        null);
            }
        } catch (final CoreException e) {
            return new Status(
                    IStatus.ERROR,
                    KarafWtpPluginActivator.PLUGIN_ID,
                    0,
                    "Unable to locate Karaf platform",
                    e);
        }
    }
}
