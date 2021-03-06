/**
 * Copyright (c) 2010 Stephen Evanchik
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Stephen Evanchik - initial implementation
 */
package info.evanchik.eclipse.karaf.workbench.factory;

import info.evanchik.eclipse.karaf.ui.workbench.KarafWorkbenchService;
import info.evanchik.eclipse.karaf.ui.workbench.KarafWorkbenchServiceFactory;
import info.evanchik.eclipse.karaf.workbench.internal.JMXWorkbenchService;

/**
 * @author Stephen Evanchik (evanchsa@gmail.com)
 *
 */
public class JMXWorkbenchServiceFactory implements KarafWorkbenchServiceFactory {

    private static final JMXWorkbenchService jmxWorkbenchService =
        new JMXWorkbenchService();

    @Override
    public KarafWorkbenchService getWorkbenchService() {
        return jmxWorkbenchService;
    }

}
