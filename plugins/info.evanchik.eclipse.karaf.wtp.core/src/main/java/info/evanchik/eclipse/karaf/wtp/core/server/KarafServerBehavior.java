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

import info.evanchik.eclipse.karaf.workbench.MBeanProvider;
import info.evanchik.eclipse.karaf.wtp.core.KarafServerLaunchConfigurationInitializer;
import info.evanchik.eclipse.karaf.wtp.core.KarafWtpPluginActivator;

import java.io.IOException;

import javax.management.ObjectName;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.jmx.framework.FrameworkMBean;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * @author Stephen Evanchik (evanchsa@gmail.com)
 *
 */
public class KarafServerBehavior extends ServerBehaviourDelegate {

    /**
     *
     * @author Stephen Evanchik (evanchsa@gmail.com)
     *
     */
    private final class MBeanProviderServiceTracker implements ServiceTrackerCustomizer {

        private final ServiceTracker serviceTracker;

        public MBeanProviderServiceTracker() {
            final BundleContext context = KarafWtpPluginActivator.getDefault().getBundle().getBundleContext();
            serviceTracker = new ServiceTracker(context, MBeanProvider.class.getName(), this);
            serviceTracker.open();
        }

        /**
         * Adds the {@link MBeanProvider} service to this server instance. This
         * service is how the workbench interacts with the running server. It
         * also serves as a sentry that indicates the server is operational.
         *
         * @param reference
         */
        @Override
        public Object addingService(final ServiceReference reference) {
            final String serviceMemento = (String) reference.getProperty(MBeanProvider.KARAF_WORKBENCH_SERVICES_ID);

            if (serviceMemento.equals(memento)) {
                final Object o = reference.getBundle().getBundleContext().getService(reference);

                setServerState(IServer.STATE_STARTED);

                mbeanProvider = (MBeanProvider) o;

                return o;
            }

            return null;
        }

        public void close() {
            serviceTracker.close();
        }

        @Override
        public void modifiedService(final ServiceReference reference, final Object service) {
        }

        @Override
        public void removedService(final ServiceReference reference, final Object service) {
            final String serviceMemento = (String) reference.getProperty(MBeanProvider.KARAF_WORKBENCH_SERVICES_ID);

            if (serviceMemento.equals(memento)) {
                mbeanProvider = null;

                terminate();
            }
        }
    }

    private static final ObjectName FRAMEWORK;

    static {
        /*
         * If this throws an exception we're in trouble because it means that the
         * constants are invalid
         */
        try {
            FRAMEWORK = new ObjectName("osgi.core:type=framework,version=1.5");
        } catch (final Exception e) {
            throw new IllegalStateException("The OSGi JMX implementation references an invalid ObjectName", e);
        }
    }

    private volatile MBeanProvider mbeanProvider;

    private final MBeanProviderServiceTracker mbeanProviderServiceTracker = new MBeanProviderServiceTracker();

    private volatile String memento;

    private final int SERVER_TERMINATE_JOB_SCHEDULE_DELAY = 5000;;

    /**
     *
     * @param launch
     * @param launchMode
     * @param monitor
     * @throws CoreException
     */
    public void configureLaunch(final ILaunch launch, final String launchMode, final IProgressMonitor monitor) throws CoreException {
        setServerRestartState(false);
        setServerState(IServer.STATE_STARTING);
        setMode(launchMode);

        monitor.worked(1);

        memento = launch.getLaunchConfiguration().getMemento();

        monitor.worked(1);
    }

    @Override
    public void setupLaunchConfiguration(final ILaunchConfigurationWorkingCopy workingCopy, IProgressMonitor monitor) throws CoreException {
        super.setupLaunchConfiguration(workingCopy, monitor);

        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }

        KarafServerLaunchConfigurationInitializer.initializeConfiguration(workingCopy);

        monitor.worked(10);
    }

    @Override
    public void stop(final boolean force) {
        mbeanProviderServiceTracker.close();

        if (force) {
            terminate();
            return;
        }

        final int state = getServer().getServerState();

        if (state == IServer.STATE_STOPPED || state == IServer.STATE_STOPPING) {
            return;
        } else if (state == IServer.STATE_STARTING) {
            terminate();
            return;
        } else {
            setServerState(IServer.STATE_STOPPING);

            try {
                if (mbeanProvider != null && mbeanProvider.isOpen()) {
                    mbeanProvider.getMBean(FRAMEWORK, FrameworkMBean.class).shutdownFramework();
                    mbeanProvider.close();
                }
            } catch (final IOException e) {
            }

            final Job j = new Job("Waiting for server to stop...") {

                @Override
                protected IStatus run(final IProgressMonitor monitor) {
                    try {
                        final ILaunch launch = getServer().getLaunch();
                        if (launch != null) {
                            launch.terminate();
                        }
                    } catch (final DebugException e) {
                        // Do nothing
                    }

                    setServerState(IServer.STATE_STOPPED);

                    return Status.OK_STATUS;
                }
            };

            j.setSystem(true);
            j.schedule(SERVER_TERMINATE_JOB_SCHEDULE_DELAY);
        }
    }

    @Override
    protected void publishServer(final int kind, final IProgressMonitor monitor) throws CoreException {
        if (getServer().getRuntime() == null) {
            return;
        }

        monitor.done();

        setServerPublishState(IServer.PUBLISH_STATE_NONE);
    }

    /**
     * Terminates the launcher forcibly without regard to application state.
     */
    protected void terminate() {
        if (getServer().getServerState() == IServer.STATE_STOPPED) {
            return;
        }

        try {
            setServerState(IServer.STATE_STOPPING);

            final ILaunch launch = getServer().getLaunch();
            if (launch != null) {
                launch.terminate();
            }

        } catch (final Exception e) {
            // Ignore as this is forcibly terminating the server
        } finally {
            setServerState(IServer.STATE_STOPPED);
        }
    }
}
