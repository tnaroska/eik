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
package info.evanchik.eclipse.karaf.core;

import info.evanchik.eclipse.karaf.core.equinox.BundleEntry;
import info.evanchik.eclipse.karaf.core.internal.KarafCorePluginActivator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.commons.collections.Predicate;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.Bundle;

/**
 * @author Stephen Evanchik (evanchsa@gmail.com)
 *
 */
public final class KarafCorePluginUtils {

    private static final String INCLUDES_PROPERTY = "${includes}"; //$NON-NLS-1$

    /**
     * Copies the source file to the destination file
     *
     * @param src
     *            the source file
     * @param dst
     *            the destination file
     * @throws IOException
     *             if there is a problem during the file copy
     */
    public static void copyFile(final File src, final File dst) throws IOException {
        if (!src.exists()) {
            throw new FileNotFoundException("File does not exist: " + src.getAbsolutePath());
        }

        if (!dst.exists()) {
            dst.createNewFile();
        }

        FileChannel source = null;
        FileChannel destination = null;
        try {
            source = new FileInputStream(src).getChannel();
            destination = new FileOutputStream(dst).getChannel();

            destination.transferFrom(source, 0, source.size());
        } finally {
            if (source != null) {
                source.close();
            }

            if (destination != null) {
                destination.close();
            }
        }
    }

    /**
     * Create a JVM system property argument (e.g -DpropertyName=propertyValue).
     *
     * @param name
     *            the name of the system property
     * @param value
     *            the value of the system property
     * @return the fully constructed system property string
     */
    public static String constructSystemProperty(final String name, final String value) {
        final StringBuilder sb = new StringBuilder("-D"); //$NON-NLS-1$
        sb.append(name);
        sb.append("="); //$NON-NLS-1$
        sb.append(value);

        return sb.toString();
    }

    /**
     * Determines if a Karaf platform model points to a ServiceMix Kernel based
     * distribution.
     *
     * @param model
     *            the candidate {@link KarafPlatformModel}
     * @return true if it is a ServiceMix Kernel based model, false if it is
     *         Apache Felix Karaf model
     * @throws IllegalArgumentException
     *             if the Karaf platform model does not contain the appropriate
     *             sentry file
     */
    public static boolean isServiceMix(final KarafPlatformModel model) {
        if (   model.getConfigurationFile(IKarafConstants.ORG_APACHE_SERVICEMIX_MANAGEMENT_CFG_FILENAME).toFile().exists()
            || model.getConfigurationFile(IKarafConstants.ORG_APACHE_SERVICEMIX_FEATURES_CFG_FILENAME).toFile().exists())
        {
            return true;
        } else if (model instanceof KarafWorkingPlatformModel) {
            final KarafWorkingPlatformModel workingModel = (KarafWorkingPlatformModel) model;
            return workingModel.getConfigurationFile(IKarafConstants.ORG_APACHE_SERVICEMIX_MANAGEMENT_CFG_FILENAME).toFile().exists();
        } else {
            return false;
        }
    }

    /**
     *
     * @param model
     * @return
     */
    public static boolean isFelixKaraf(final KarafPlatformModel model) {
        if (   model.getConfigurationFile(IKarafConstants.ORG_APACHE_FELIX_KARAF_MANAGEMENT_CFG_FILENAME).toFile().exists()) {
            return true;
        } else if (model instanceof KarafWorkingPlatformModel) {
            final KarafWorkingPlatformModel workingModel = (KarafWorkingPlatformModel) model;
            return workingModel.getConfigurationFile(IKarafConstants.ORG_APACHE_FELIX_KARAF_MANAGEMENT_CFG_FILENAME).toFile().exists();
        } else {
            return false;
        }
    }

    /**
     *
     * @param model
     * @return
     */
    public static boolean isKaraf(final KarafPlatformModel model) {
        if (model.getConfigurationFile(IKarafConstants.ORG_APACHE_KARAF_MANAGEMENT_CFG_FILENAME).toFile().exists()) {
            return true;
        } else if (model instanceof KarafWorkingPlatformModel) {
            final KarafWorkingPlatformModel workingModel = (KarafWorkingPlatformModel) model;
            return workingModel.getConfigurationFile(IKarafConstants.ORG_APACHE_KARAF_MANAGEMENT_CFG_FILENAME).toFile().exists();
        } else {
            return false;
        }
    }

    /**
     * Filter a list based on a {@link Predicate}
     *
     * @param <T>
     * @param target
     * @param predicate
     * @return
     */
    public static <T> List<T> filterList(final Collection<T> target, final Predicate predicate) {
        final List<T> result = new ArrayList<T>();
        for (final T element: target) {
            if (predicate.evaluate(element)) {
                result.add(element);
            }
        }
        return result;
    }

    /**
     * Helper method that returns the {@link Bundle}'s location based on its
     * symbolic name
     *
     * @param bundleSymbolicName
     *            the symbolic name of the {@code Bundle}
     * @return the location of the {@code Bundle} or {@code null}
     */
    public static String getBundleLocation(final String bundleSymbolicName) {
        final Bundle bundle = Platform.getBundle(bundleSymbolicName);
        if (bundle == null) {
            KarafCorePluginActivator.getLogger().error("Unable to locate bundle with symbolic name: " + bundleSymbolicName);
            return null;
        }

        try {
            return FileLocator.getBundleFile(bundle).getAbsolutePath();
        } catch(final IOException e) {
            KarafCorePluginActivator.getLogger().error("Unable to locate bundle with symbolic name: " + bundleSymbolicName, e);
            return null;
        }
    }

    /**
     * Construct a {@link List} of {@link BundleEntry} objects from the
     * specified string. The string is one typically found in the {@code
     * osgi.bundles} property of an Eclipse {@code config.ini}
     *
     * @param osgiBundles
     *            a string is typically found in the {@code osgi.bundles}
     *            property of an Eclipse {@code config.ini}
     * @return a {@code List} of {@code BundleEntry} objects
     */
    public static List<BundleEntry> getEquinoxBundles(final String osgiBundles) {
        final String[] bundles = osgiBundles.split(",");

        final List<BundleEntry> entries = new ArrayList<BundleEntry>();
        for (final String s : bundles) {
            entries.add(BundleEntry.fromString(s.trim()));
        }

        return entries;
    }

    /**
     * Reads a specified MANIFEST.MF entry from the JAR
     *
     * @param src
     *            the JAR file
     * @param manifestHeader
     *            the name of the header to read
     * @return the header as read from the MANIFEST.MF or null if it does not
     *         exist or there was a problem reading the JAR
     * @throws IOException
     *             if there is a problem reading the JAR
     */
    public static String getJarManifestHeader(final File src, final String manifestHeader) throws IOException {
        final JarFile jar = new JarFile(src);
        final Manifest mf = jar.getManifest();

        return (String) mf.getMainAttributes().get(manifestHeader);
    }

    /**
     * Searches a directory to the specified depth for library files.<br>
     * <br>
     * This method is recursive so be careful with the maximum depth
     *
     * @param dir
     *            the directory to being the search
     * @param list
     *            the list of libraries found
     * @param maxDepth
     *            the current maximum depth
     */
    public static void getJarFileList(final File dir, final List<File> list, final int maxDepth) {
    	getFileList(dir, ".jar", list, maxDepth);
    }

	/**
	 * Searches a directory to the specified depth for library files.<br>
	 * <br>
	 * This method is recursive so be careful with the maximum depth
	 *
	 * @param dir
	 *            the directory to being the search
	 * @param extension
	 *            the extension to search for
	 * @param list
	 *            the list of libraries found
	 * @param maxDepth
	 *            the current maximum depth
	 */
    public static void getFileList(final File dir, final String extension, final List<File> list, final int maxDepth) {
        if (dir == null) {
            throw new IllegalArgumentException("Directory must not be null");
        }

        final File[] files = dir.listFiles();
        if (files == null) {
            return;
        }

        for (final File file : files) {
            if (file.isDirectory() && maxDepth > 0) {
                getFileList(file, extension, list, maxDepth - 1);
            } else if (file.getAbsolutePath().endsWith(extension) || file.getAbsolutePath().endsWith(".zip")) {
                list.add(file);
            }
        }
    }

    /**
     * Extracts the last path component from a URL or filename such as an Maven2
     * URL:<br> {@code
     * mvn:org/apache/servicemix/group/artifactId/version/filename-version.jar}
     *
     * @param pathString
     *            An URL or File path such as an Apache ServiceMix Maven2 URL of
     *            the form: {@code
     *            mvn:org/apache/servicemix/group/artifactId/version/filename-version
     *            .jar}
     * @return The filename component of the URL, e.g. {@code
     *         filename-version.jar}
     */
    public static String getLastPathComponent(final String pathString) {
        return pathString.substring(pathString.lastIndexOf('/') + 1);
    }

    /**
     * Implementation of join using a {@link StringBuffer}
     *
     * @param items
     *            the {@link Collection} of items that will be joined together
     * @param glue
     *            the string to act as glue in the concatenation
     * @return the concatenation of the specified items
     */
    public static String join(final Collection<? extends Object> items, final String glue) {
        final StringBuffer buffer = new StringBuffer();
        for (final Object o : items) {
            if (buffer.length() > 0) {
                buffer.append(glue);
            }

            buffer.append(o.toString());
        }

        return buffer.toString();
    }

    /**
     * Loads a properties file as a resource from the specified {@link Bundle}
     *
     * @param theBundle
     *            the {@link Bundle} to use as the source of the properties file
     * @param propertiesFile
     *            the path, relative to the root of the bundle, to the
     *            properties file
     * @return a {@link Properties} object or null if there was an error
     */
    public static Properties loadProperties(final Bundle theBundle, final String propertiesFile) {
        final Properties properties = new Properties();

        final URL entryUrl = theBundle.getEntry(propertiesFile);

        if (entryUrl == null) {
            return null;
        }

        try {
            final InputStream in = entryUrl.openStream();

            properties.load(in);

            in.close();

            return properties;
        } catch (final IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Loads a configuration file relative to the specified base
     * directory
     *
     * @param base
     *            the directory containing the file
     * @param filename
     *            the relative path to the properties file
     * @return the {@link Properties} object created from the contents of
     *         configuration file
     * @throws CoreException
     *             if there is a problem loading the file
     */
    public static Properties loadProperties(final File base, final String filename) throws CoreException {
        final File f = new File(base, filename);

        try {
            final Properties p = new Properties();
            p.load(new FileInputStream(f));

            return p;
        } catch (final IOException e) {
            final String message = "Unable to load configuration file from configuration directory: " + f.getAbsolutePath();
            throw new CoreException(new Status(IStatus.ERROR, KarafCorePluginActivator.PLUGIN_ID, IStatus.OK, message, e));
        }

    }

    /**
     * Loads a configuration file relative to the specified base directory. This
     * method also processes any include directives that import other properties
     * files relative to the specified property file.
     *
     * @param base
     *            the directory containing the file
     * @param filename
     *            the relative path to the properties file
     * @param processIncludes
     *            true if {@link #INCLUDES_PROPERTY} statements should be
     *            followed; false otherwise.
     * @return the {@link Properties} object created from the contents of
     *         configuration file
     * @throws CoreException
     *             if there is a problem loading the file
     */
    public static Properties loadProperties(final File base, final String filename, final boolean processIncludes) throws CoreException {
        final File f = new File(base, filename);

        try {
            final Properties p = new Properties();
            p.load(new FileInputStream(f));

            final String includes = p.getProperty(INCLUDES_PROPERTY);
            if (includes != null) {
                final StringTokenizer st = new StringTokenizer(includes, "\" ", true);
                if (st.countTokens() > 0) {
                    String location;
                    do {
                        location = nextLocation(st);
                        if (location != null) {
                            final Properties includeProps = loadProperties(base, location);
                            p.putAll(includeProps);
                        }
                    } while (location != null);
                }
                p.remove(INCLUDES_PROPERTY);
            }

            return p;
        } catch (final IOException e) {
            final String message = "Unable to load configuration file from configuration directory: " + f.getAbsolutePath();
            throw new CoreException(new Status(IStatus.ERROR, KarafCorePluginActivator.PLUGIN_ID, IStatus.OK, message, e));
        }

    }

    private KarafCorePluginUtils() {
        throw new AssertionError("Cannot instantiate " + KarafCorePluginUtils.class.getName());
    }

    private static String nextLocation(final StringTokenizer st) {
        String retVal = null;

        if (st.countTokens() > 0) {
            String tokenList = "\" ";
            StringBuffer tokBuf = new StringBuffer(10);
            String tok = null;
            boolean inQuote = false;
            boolean tokStarted = false;
            boolean exit = false;
            while (st.hasMoreTokens() && !exit) {
                tok = st.nextToken(tokenList);
                if (tok.equals("\"")) {
                    inQuote = !inQuote;
                    if (inQuote) {
                        tokenList = "\"";
                    } else {
                        tokenList = "\" ";
                    }

                } else if (tok.equals(" ")) {
                    if (tokStarted) {
                        retVal = tokBuf.toString();
                        tokStarted = false;
                        tokBuf = new StringBuffer(10);
                        exit = true;
                    }
                } else {
                    tokStarted = true;
                    tokBuf.append(tok.trim());
                }
            }

            // Handle case where end of token stream and
            // still got data
            if (!exit && tokStarted) {
                retVal = tokBuf.toString();
            }
        }

        return retVal;
    }
}
