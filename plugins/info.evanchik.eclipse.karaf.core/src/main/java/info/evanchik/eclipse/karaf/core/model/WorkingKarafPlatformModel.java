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
package info.evanchik.eclipse.karaf.core.model;

import info.evanchik.eclipse.karaf.core.KarafPlatformModel;
import info.evanchik.eclipse.karaf.core.KarafWorkingPlatformModel;

import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.pde.core.plugin.IPluginModelBase;

/**
 * @author Stephen Evanchik (evanchsa@gmail.com)
 *
 */
public class WorkingKarafPlatformModel extends AbstractKarafPlatformModel implements KarafWorkingPlatformModel {

    private final IPath location;

    private final KarafPlatformModel parentKarafModel;

    /**
     * Constructs a {@link KarafPlatformModel} that delegates all information
     * about the underlying plugins that comprise this model while handling all
     * configuration methods.
     *
     * @param thisModelLocation
     *            the {@link IPath} to the working copy of the {@code
     *            KarafPlatformModel}
     * @param source
     *            the source of all non-configuration information for this Karaf
     *            platform.
     */
    public WorkingKarafPlatformModel(final IPath thisModelLocation, final KarafPlatformModel source) {
        this.location = thisModelLocation;
        this.parentKarafModel = source;
    }

    @Override
    public boolean containsPlugin(final IPluginModelBase plugin) {
        return parentKarafModel.containsPlugin(plugin);
    }

    @Override
    public Object getAdapter(@SuppressWarnings("rawtypes") final Class adapter) {
        final KarafPlatformModel parentModel = getParentKarafModel();
        final Object adaptedObject = parentModel.getAdapter(adapter);
        return adaptedObject;
    }

    @Override
    public List<String> getBootClasspath() {
        return parentKarafModel.getBootClasspath();
    }

    @Override
    public IPath getConfigurationFile(final String key) {
        final IPath origFile = parentKarafModel.getConfigurationFile(key);
        final int matchingSegments = origFile.matchingFirstSegments(parentKarafModel
                        .getConfigurationDirectory());

        final IPath configFile = origFile.removeFirstSegments(matchingSegments - 1);

        return location.append(configFile);
    }

    @Override
    public KarafPlatformModel getParentKarafModel() {
        return parentKarafModel;
    }

    @Override
    public IPath getPluginRootDirectory() {
        return parentKarafModel.getPluginRootDirectory();
    }

    @Override
    public IPath getRootDirectory() {
        return location;
    }

    @Override
    public State getState() {
        return parentKarafModel.getState();
    }

    @Override
    public boolean isFrameworkPlugin(final IPluginModelBase model) {
        return parentKarafModel.isFrameworkPlugin(model);
    }

    /**
     * While the source {@link KarafPlatformModel} may be read-only this working
     * copy is not.
     *
     * @return true in all cases
     */
    @Override
    public boolean isReadOnly() {
        return false;
    }

    /**
     * This working copy is always valid (unless something outside of Eclipse
     * mutates it).
     *
     * @return true in all cases
     */
    public boolean isValid() {
        return true;
    }

    @Override
    protected List<URL> getPlatformBundles() {
        return Collections.emptyList();
    }
}
