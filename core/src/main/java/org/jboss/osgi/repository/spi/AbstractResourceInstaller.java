/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.osgi.repository.spi;


import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

import org.jboss.osgi.repository.RepositoryMessages;
import org.jboss.osgi.repository.ResourceInstaller;
import org.jboss.osgi.resolver.XBundle;
import org.jboss.osgi.resolver.XIdentityCapability;
import org.jboss.osgi.resolver.XResource;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.service.repository.RepositoryContent;

/**
 * A repository resource installer
 *
 * @author Thomas.Diesler@jboss.com
 * @since 10-May-2013
 */
public class AbstractResourceInstaller implements ResourceInstaller {

    @Override
    public XBundle installResource(BundleContext context, XResource res) throws BundleException {
        XBundle bundle;
        XIdentityCapability icap = res.getIdentityCapability();
        String namespace = icap.getNamespace();
        if (namespace.equals(IdentityNamespace.IDENTITY_NAMESPACE) && res instanceof RepositoryContent) {
            bundle = installBundleResource(context, res);
        } else {
            bundle = installModuleResource(context, res);
        }
        return bundle;
    }

    @Override
    public XBundle installBundleResource(BundleContext context, XResource res) throws BundleException {
        XIdentityCapability icap = res.getIdentityCapability();
        if (!icap.getNamespace().equals(IdentityNamespace.IDENTITY_NAMESPACE))
            throw RepositoryMessages.MESSAGES.unsupportedResource(res);

        if (!(res instanceof RepositoryContent))
            throw RepositoryMessages.MESSAGES.unsupportedResource(res);

        InputStream input = ((RepositoryContent) res).getContent();
        try {
            return (XBundle) context.installBundle(icap.getName(), input);
        } finally {
            safeClose(input);
        }
    }

    @Override
    public XBundle installModuleResource(BundleContext context, XResource res) throws BundleException {
        throw new UnsupportedOperationException();
    }
    
    private void safeClose(Closeable input) {
        try {
            input.close();
        } catch (IOException e) {
            // ignore
        }
    }
}