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
package org.jboss.osgi.repository.spi;

import static org.jboss.osgi.repository.RepositoryMessages.MESSAGES;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.osgi.repository.Namespace100.Attribute;
import org.jboss.osgi.repository.RepositoryContentHelper;
import org.jboss.osgi.repository.RepositoryReader;
import org.jboss.osgi.repository.RepositoryStorage;
import org.jboss.osgi.repository.RepositoryStorageException;
import org.jboss.osgi.repository.RepositoryWriter;
import org.jboss.osgi.repository.RepositoryXMLReader;
import org.jboss.osgi.repository.RepositoryXMLWriter;
import org.jboss.osgi.repository.URLResourceBuilderFactory;
import org.jboss.osgi.repository.XRepository;
import org.jboss.osgi.repository.spi.MavenDelegateRepository.ConfigurationPropertyProvider;
import org.jboss.osgi.resolver.XCapability;
import org.jboss.osgi.resolver.XResource;
import org.jboss.osgi.resolver.XResourceBuilder;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
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

    public static final String REPOSITORY_XML_NAME = "repository.xml";

    private final File storageDir;
    private final File repoFile;

    public FileBasedRepositoryStorage(XRepository repository, File storageDir, ConfigurationPropertyProvider propProvider) {
        super(repository);
        if (storageDir == null)
            throw MESSAGES.illegalArgumentNull("storageDir");
        if (propProvider == null)
            throw MESSAGES.illegalArgumentNull("propProvider");

        this.storageDir = storageDir;

        String filename = propProvider.getProperty(XRepository.PROPERTY_REPOSITORY_STORAGE_FILE, REPOSITORY_XML_NAME);
        repoFile = new File(storageDir.getAbsolutePath() + File.separator + filename).getAbsoluteFile();

        // Initialize repository content
        if (repoFile.exists()) {
            RepositoryReader reader;
            try {
                reader = RepositoryXMLReader.create(new FileInputStream(repoFile));
            } catch (IOException ex) {
                throw MESSAGES.cannotInitializeRepositoryReader(ex);
            }
            String incatt = reader.getRepositoryAttributes().get(Attribute.INCREMENT.getLocalName());
            Long increment = new Long(incatt != null ? incatt : "0");
            XResource res = reader.nextResource();
            while(res != null) {
                addResourceInternal(res, false);
                res = reader.nextResource();
            }
            long delta = increment - getResourceIndex().get();
            getResourceIndex().addAndGet(delta);
            reader.close();
        }
    }

    @Override
    public XResource addResource(XResource res) throws RepositoryStorageException {
        return addResourceInternal(res, true);
    }

    private synchronized XResource addResourceInternal(XResource res, boolean writeXML) throws RepositoryStorageException {
        if (res.isAbstract()) {
            return addAbstractResource(res, writeXML);
        } else {
            return addContentResource(res, writeXML);
        }
    }

    private XResource addContentResource(XResource res, boolean writeXML) throws RepositoryStorageException {
        List<Capability> ccaps = res.getCapabilities(ContentNamespace.CONTENT_NAMESPACE);
        if (ccaps.isEmpty())
            throw MESSAGES.cannotObtainContentCapablility(res);

        XCapability ccap = (XCapability) ccaps.get(0);
        String contentURL = (String) ccap.getAttribute(ContentNamespace.CAPABILITY_URL_ATTRIBUTE);
        if (contentURL == null)
            throw MESSAGES.cannotObtainContentURL(res);

        XResource result;

        // Copy the resource to this storage, if the content URL does not match
        if (contentURL.startsWith(getBaseURL().toExternalForm()) == false) {
            XResourceBuilder<XResource> builder = createResourceInternal(res, false);
            for (Capability cap : res.getCapabilities(null)) {
                if (!ContentNamespace.CONTENT_NAMESPACE.equals(cap.getNamespace())) {
                    builder.addCapability(cap.getNamespace(), cap.getAttributes(), cap.getDirectives());
                }
            }
            for (Requirement req : res.getRequirements(null)) {
                String namespace = req.getNamespace();
                builder.addRequirement(namespace, req.getAttributes(), req.getDirectives());
            }
            result = builder.getResource();
        } else {
            result = res;
        }
        result = super.addResource(result);
        if (writeXML == true) {
            writeRepositoryXML();
        }
        return result;
    }

    private XResource addAbstractResource(XResource res, boolean writeXML) throws RepositoryStorageException {
        XResource result = super.addResource(res);
        if (writeXML == true) {
            writeRepositoryXML();
        }
        return result;
    }

    @Override
    public boolean removeResource(XResource res) throws RepositoryStorageException {
        return removeResourceInternal(res, true);
    }

    private synchronized boolean removeResourceInternal(XResource res, boolean writeXML) {
        boolean result = true;
        List<Capability> ccaps = res.getCapabilities(ContentNamespace.CONTENT_NAMESPACE);
        if (!ccaps.isEmpty()) {
            XCapability ccap = (XCapability) ccaps.iterator().next();
            String fileURL = (String) ccap.getAttribute(ContentNamespace.CAPABILITY_URL_ATTRIBUTE);
            File contentFile = new File(fileURL.substring("file:".length()));
            if (contentFile.exists()) {
                result = deleteRecursive(contentFile.getParentFile());
            }
        }
        result &= super.removeResource(res);
        if (writeXML == true) {
            writeRepositoryXML();
        }
        return result;
    }

    private XResourceBuilder<XResource> createResourceInternal(XResource resource, boolean loadMetadata) {
        XResourceBuilder<XResource> factory = null;
        for (Capability cap : resource.getCapabilities(ContentNamespace.CONTENT_NAMESPACE)) {
            XCapability ccap = (XCapability)cap;
            Map<String, Object> contentAtts = new HashMap<String, Object>();
            String mimeType = (String) ccap.getAttribute(ContentNamespace.CAPABILITY_MIME_ATTRIBUTE);
            if (mimeType != null) {
                contentAtts.put(ContentNamespace.CAPABILITY_MIME_ATTRIBUTE, mimeType);
            }
            InputStream input = getResourceContent(ccap);
            try {
                URL contentURL = addResourceContent(input, contentAtts);
                if (factory == null) {
                    factory = URLResourceBuilderFactory.create(contentURL, contentAtts, loadMetadata);
                } else {
                    factory.addCapability(ContentNamespace.CONTENT_NAMESPACE, contentAtts, null);
                }
            } catch (IOException ex) {
                throw MESSAGES.cannotAddResourceToStorage(ex, mimeType);
            }
        }
        return factory;
    }

    private InputStream getResourceContent(XCapability ccap) {
        InputStream input;
        Resource resource = ccap.getResource();
        Capability defaultContent = resource.getCapabilities(ContentNamespace.CONTENT_NAMESPACE).get(0);
        if (defaultContent == ccap && resource instanceof RepositoryContent) {
            input = ((RepositoryContent) resource).getContent();
        } else {
            String contentURL = (String) ccap.getAttribute(ContentNamespace.CAPABILITY_URL_ATTRIBUTE);
            try {
                input = new URL(contentURL).openStream();
            } catch (IOException ex) {
                throw MESSAGES.cannotAccessContentURL(ex, contentURL);
            }
        }
        return input;
    }

    private URL addResourceContent(InputStream input, Map<String, Object> atts) throws IOException {
        synchronized (storageDir) {
            // Copy the input stream to temporary storage
            File tempFile = new File(storageDir.getAbsolutePath() + File.separator + "temp-content");
            Long size = copyResourceContent(input, tempFile);
            atts.put(ContentNamespace.CAPABILITY_SIZE_ATTRIBUTE, size);
            // Calculate the SHA-256
            String sha256;
            String algorithm = RepositoryContentHelper.DEFAULT_DIGEST_ALGORITHM;
            try {
                sha256 = RepositoryContentHelper.getDigest(new FileInputStream(tempFile), algorithm);
                atts.put(ContentNamespace.CONTENT_NAMESPACE, sha256);
            } catch (NoSuchAlgorithmException ex) {
                throw MESSAGES.noSuchAlgorithm(ex, algorithm);
            }
            // Move the content to storage location
            String contentPath = sha256.substring(0, 2) + File.separator + sha256.substring(2) + File.separator + "content";
            File targetFile = new File(storageDir.getAbsolutePath() + File.separator + contentPath);
            targetFile.getParentFile().mkdirs();
            tempFile.renameTo(targetFile);
            URL url = targetFile.toURI().toURL();
            atts.put(ContentNamespace.CAPABILITY_URL_ATTRIBUTE, url.toExternalForm());
            return url;
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

    private URL getBaseURL() {
        try {
            return storageDir.toURI().toURL();
        } catch (MalformedURLException e) {
            // ignore
            return null;
        }
    }

    private void writeRepositoryXML() {
        RepositoryWriter writer;
        try {
            repoFile.getParentFile().mkdirs();
            writer = RepositoryXMLWriter.create(new FileOutputStream(repoFile));
        } catch (IOException ex) {
            throw MESSAGES.cannotInitializeRepositoryWriter(ex);
        }
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(Attribute.NAME.getLocalName(), getRepository().getName());
        attributes.put(Attribute.INCREMENT.getLocalName(), new Long(getResourceIndex().get()).toString());
        writer.writeRepositoryElement(attributes);
        RepositoryReader reader = getRepositoryReader();
        XResource resource = reader.nextResource();
        while(resource != null) {
            writer.writeResource(resource);
            resource = reader.nextResource();
        }
        writer.close();
    }

    private boolean deleteRecursive(File file) {
        boolean result = true;
        if (file.isDirectory()) {
            for (File aux : file.listFiles())
                result &= deleteRecursive(aux);
        }
        result &= file.delete();
        return result;
    }
}
