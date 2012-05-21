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
package org.jboss.osgi.repository.core;

import static org.jboss.osgi.repository.RepositoryMessages.MESSAGES;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.osgi.repository.RepositoryStorage;
import org.jboss.osgi.repository.RepositoryStorageException;
import org.jboss.osgi.repository.URLResourceBuilderFactory;
import org.jboss.osgi.repository.spi.MemoryRepositoryStorage;
import org.jboss.osgi.resolver.XCapability;
import org.jboss.osgi.resolver.XResource;
import org.jboss.osgi.resolver.XResourceBuilder;
import org.jboss.osgi.resolver.XResourceConstants;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.service.repository.ContentNamespace;
import org.osgi.service.repository.RepositoryContent;

/**
 * A simple {@link RepositoryStorage} that uses
 * the local file system.
 *
 * @author thomas.diesler@jboss.com
 * @since 16-Jan-2012
 */
public class FileBasedRepositoryStorage extends MemoryRepositoryStorage {

    private final File repository;

    public FileBasedRepositoryStorage(File repository) {
        this.repository = repository;
    }

    @Override
    public XResource addResource(String mime, InputStream input) throws RepositoryStorageException {
        XResourceBuilder builder = createResourceInternal(input, mime, true);
        return addResource(builder.getResource());
    }

    @Override
    public XResource addResource(XResource res) throws RepositoryStorageException {
        List<Capability> ccaps = res.getCapabilities(ContentNamespace.CONTENT_NAMESPACE);
        if (ccaps.isEmpty())
            throw MESSAGES.storageCannotObtainContentCapablility(res);

        XCapability ccap = (XCapability) ccaps.get(0);
        URL contentURL = (URL) ccap.getAttribute(ContentNamespace.CAPABILITY_URL_ATTRIBUTE);
        if (contentURL == null)
            throw MESSAGES.storageCannotObtainContentURL(res);

        XResource result;

        // Copy the resource to this storage, if the content URL does not match
        if (contentURL.getPath().startsWith(getBaseURL().getPath()) == false) {
            InputStream input = ((RepositoryContent) res).getContent();
            String mime = (String) ccap.getAttribute(ContentNamespace.CAPABILITY_MIME_ATTRIBUTE);
            XResourceBuilder builder = createResourceInternal(input, mime, false);
            for (Capability cap : res.getCapabilities(null)) {
                String namespace = cap.getNamespace();
                if (!namespace.equals(ContentNamespace.CONTENT_NAMESPACE)) {
                    builder.addGenericCapability(namespace, cap.getAttributes(), cap.getDirectives());
                }
            }
            for (Requirement req : res.getRequirements(null)) {
                String namespace = req.getNamespace();
                builder.addGenericRequirement(namespace, req.getAttributes(), req.getDirectives());
            }
            result = builder.getResource();
        } else {
            result = res;
        }

        result = super.addResource(result);
        return result;
    }

    @Override
    public boolean removeResource(XResource res) throws RepositoryStorageException {
        XCapability ccap = (XCapability) res.getCapabilities(ContentNamespace.CONTENT_NAMESPACE).get(0);
        URL fileURL = (URL) ccap.getAttribute(ContentNamespace.CAPABILITY_URL_ATTRIBUTE);
        File contentFile = new File(fileURL.getPath());
        boolean result = true;
        if (contentFile.exists()) {
            result = deleteRecursive(contentFile.getParentFile());
        }
        return result && super.removeResource(res);
    }

    private XResourceBuilder createResourceInternal(InputStream input, String mime, boolean loadMetadata) {
        Map<String, Object> atts = new HashMap<String, Object>();
        if (mime != null) {
            atts.put(ContentNamespace.CAPABILITY_MIME_ATTRIBUTE, mime);
        }
        try {
            String contentPath = addResourceContent(input, atts);
            return URLResourceBuilderFactory.create(getBaseURL(), contentPath, atts, loadMetadata);
        } catch (IOException ex) {
            throw MESSAGES.storageCannotAddResourceToStorage(ex, mime);
        }
    }

    private String addResourceContent(InputStream input, Map<String, Object> atts) throws IOException {
        synchronized (repository) {
            // Copy the input stream to temporary storage
            File tempFile = new File(repository.getAbsolutePath() + File.separator + "temp-content");
            Long size = copyResourceContent(input, tempFile);
            atts.put(ContentNamespace.CAPABILITY_SIZE_ATTRIBUTE, size);
            // Calculate the SHA-256
            String algorithm = "SHA-256";
            String sha256;
            try {
                sha256 = getDigest(tempFile, algorithm);
                atts.put(ContentNamespace.CONTENT_NAMESPACE, sha256);
            } catch (NoSuchAlgorithmException ex) {
                throw MESSAGES.storageNoSuchAlgorithm(ex, algorithm);
            }
            // Move the content to storage location
            String contentPath = sha256.substring(0, 2) + File.separator + sha256.substring(2) + File.separator + "content";
            atts.put(XResourceConstants.CAPABILITY_PATH_ATTRIBUTE, contentPath);
            File targetFile = new File(repository.getAbsolutePath() + File.separator + contentPath);
            targetFile.getParentFile().mkdirs();
            tempFile.renameTo(targetFile);
            return contentPath;
        }
    }

    private long copyResourceContent(InputStream input, File targetFile) throws IOException {
        int len = 0;
        long total = 0;
        byte[] buf = new byte[4096];
        targetFile.getParentFile().mkdirs();
        OutputStream out = new FileOutputStream(targetFile);
        while ((len = input.read(buf)) >= 0) {
            out.write(buf, 0, len);
            total += len;
        }
        input.close();
        out.close();
        return total;
    }

    private String getDigest(File sourceFile, String algorithm) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(algorithm);
        FileInputStream fis = new FileInputStream(sourceFile);

        int nread = 0;
        byte[] dataBytes = new byte[1024];
        while ((nread = fis.read(dataBytes)) != -1) {
            md.update(dataBytes, 0, nread);
        }
        ;
        byte[] mdbytes = md.digest();

        // Convert the byte to hex format method 2
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < mdbytes.length; i++) {
            result.append(Integer.toHexString(0xFF & mdbytes[i]));
        }

        return result.toString();
    }

    private URL getBaseURL() {
        try {
            return repository.toURI().toURL();
        } catch (MalformedURLException e) {
            // ignore
            return null;
        }
    }

    private boolean deleteRecursive(File file) {
        if (file.isDirectory()) {
            for (File aux : file.listFiles())
                return deleteRecursive(aux);
        }
        return file.delete();
    }
}
