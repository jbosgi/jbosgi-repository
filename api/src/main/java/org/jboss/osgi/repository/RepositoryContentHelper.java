/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.osgi.repository;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Helpers for repository content
 *
 * @author thomas.diesler@jboss.com
 * @since 31-May-2012
 */
public class RepositoryContentHelper {

    public static final String UNKNOWN_DIGEST = "unknown-digest";

    // Hide ctor
    private RepositoryContentHelper() {
    }

    /**
     * Get the digest for a given input stream and algorithm
     */
    public static String getDigest(InputStream input, String algorithm) throws IOException, NoSuchAlgorithmException {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);

            int nread = 0;
            byte[] dataBytes = new byte[1024];
            while ((nread = input.read(dataBytes)) != -1) {
                md.update(dataBytes, 0, nread);
            }
            byte[] mdbytes = md.digest();

            StringBuilder builder = new StringBuilder();
            for (byte b : mdbytes) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } finally {
            input.close();
        }
    }
}
