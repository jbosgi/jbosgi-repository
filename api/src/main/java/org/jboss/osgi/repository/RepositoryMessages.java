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

import javax.xml.stream.Location;

import org.jboss.logging.Cause;
import org.jboss.logging.Message;
import org.jboss.logging.MessageBundle;
import org.jboss.logging.Messages;
import org.jboss.osgi.resolver.XResource;

/**
 * Logging Id ranges: 20500-20599
 *
 * https://docs.jboss.org/author/display/JBOSGI/JBossOSGi+Logging
 *
 * @author Thomas.Diesler@jboss.com
 */
@MessageBundle(projectCode = "JBOSGI")
public interface RepositoryMessages {

    RepositoryMessages MESSAGES = Messages.getBundle(RepositoryMessages.class);

    @Message(id = 20500, value = "%s is null")
    IllegalArgumentException illegalArgumentNull(String name);

    @Message(id = 20501, value = "Resource already exists: %s")
    IllegalStateException illegalStateResourceAlreadyExists(XResource res);

    @Message(id = 20502, value = "Cannot initialize repository reader")
    IllegalStateException illegalStateCannotInitializeRepositoryReader(@Cause Throwable th);

    @Message(id = 20503, value = "Cannot read repository element: %s")
    IllegalStateException illegalStateCannotReadRepositoryElement(@Cause Throwable th, Location location);

    @Message(id = 20504, value = "Cannot read resource element: %s")
    IllegalStateException storageCannotReadResourceElement(@Cause Throwable th, Location location);

    @Message(id = 20505, value = "Cannot obtain content capability: %s")
    RepositoryStorageException storageCannotObtainContentCapablility(XResource res);

    @Message(id = 20506, value = "Cannot obtain content URL: %s")
    RepositoryStorageException storageCannotObtainContentURL(XResource res);

    @Message(id = 20507, value = "Cannot access content URL: %s")
    RepositoryStorageException storageCannotAccessContentURL(@Cause Throwable th, String contentURL);

    @Message(id = 20508, value = "Invalid content URL: %s")
    RepositoryStorageException storageInvalidContentURL(String contentURL);

    @Message(id = 20509, value = "No such digest algorithm: %s")
    RepositoryStorageException storageNoSuchAlgorithm(@Cause Throwable th, String algorithm);

    @Message(id = 20510, value = "Cannot add resource to storage: %s")
    RepositoryStorageException storageCannotAddResourceToStorage(@Cause Throwable th, String mime);

    @Message(id = 20511, value = "Cannot obtain input stream for: %s")
    RepositoryStorageException storageCannotObtainInputStream(@Cause Throwable th, XResource res);

    @Message(id = 20512, value = "Cannot initialize repository writer")
    IllegalStateException illegalStateCannotInitializeRepositoryWriter(@Cause Throwable th);

    @Message(id = 20513, value = "Cannot write repository element")
    IllegalStateException illegalStateCannotWriteRepositoryElement(@Cause Throwable th);

    @Message(id = 20514, value = "Invalid filter directive: %s")
    IllegalArgumentException illegalArgumentInvalidFilterDirective(String filter);

    @Message(id = 20515, value = "Cannot obtain RepositoryStorageFactory service")
    IllegalStateException illegalStateCannotObtainRepositoryStorageFactory();
}
