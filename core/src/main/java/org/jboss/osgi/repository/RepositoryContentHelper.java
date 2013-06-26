package org.jboss.osgi.repository;
/*
 * #%L
 * JBossOSGi Repository
 * %%
 * Copyright (C) 2010 - 2012 JBoss by Red Hat
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

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
public final class RepositoryContentHelper {

    public static final String DEFAULT_DIGEST_ALGORITHM = "SHA-256";
    
    // Hide ctor
    private RepositoryContentHelper() {
    }

    /**
     * Get the digest for a given input stream using the default algorithm
     */
    public static String getDigest(InputStream input) throws IOException, NoSuchAlgorithmException {
        return getDigest(input, DEFAULT_DIGEST_ALGORITHM);
    }
    
    /**
     * Get the digest for a given input stream and algorithm
     */
    public static String getDigest(InputStream input, String algorithm) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(algorithm);
        try {
            int nread = 0;
            byte[] dataBytes = new byte[1024];
            while ((nread = input.read(dataBytes)) != -1) {
                md.update(dataBytes, 0, nread);
            }
        } finally {
            input.close();
        }
        StringBuilder builder = new StringBuilder();
        for (byte b : md.digest()) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }
}
