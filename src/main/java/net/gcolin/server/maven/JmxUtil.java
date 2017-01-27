/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package net.gcolin.server.maven;

import java.lang.management.ManagementFactory;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.StandardMBean;

/**
 * Utility for publishing Jmx beans.
 *
 * @author Gaël COLIN
 * @since 1.0
 */
public final class JmxUtil {

    /**
     * Logger.
     */
    private static final Logger LOG = Logger.getLogger(JmxUtil.class.getName());

    /**
     * Utility class unused constructor.
     */
    private JmxUtil() {
    }

    /**
     * Publish a JmxBean.
     *
     * @param <T> interface type
     * @param jmxname JMX path
     * @param implementation the implementation of type bean
     * @param mbeanInterface the interface accessible through a JMX console
     */
    public static <T> void publish(final String jmxname,
            final T implementation, final Class<T> mbeanInterface) {
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            ObjectName name = new ObjectName(jmxname);
            if (!mbs.isRegistered(name)) {
                StandardMBean mbean
                        = new StandardMBean(implementation, mbeanInterface);
                mbs.registerMBean(mbean, name);
            }
        } catch (InstanceAlreadyExistsException ex) {
            report(ex);
        } catch (MBeanRegistrationException ex) {
            report(ex);
        } catch (NotCompliantMBeanException ex) {
            report(ex);
        } catch (MalformedObjectNameException ex) {
            report(ex);
        }
    }

    /**
     * Remove a JmxBean.
     *
     * @param jmxname JMX path
     */
    public static void unpublish(final String jmxname) {
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            ObjectName name = new ObjectName(jmxname);
            if (mbs.isRegistered(name)) {
                mbs.unregisterMBean(name);
            }
        } catch (MBeanRegistrationException ex) {
            report(ex);
        } catch (MalformedObjectNameException ex) {
            report(ex);
        } catch (InstanceNotFoundException ex) {
            report(ex);
        }
    }

    /**
     * Log an error.
     *
     * @param ex exception
     */
    private static void report(final Exception ex) {
        LOG.log(Level.SEVERE, "cannot register mbean", ex);
    }

}
