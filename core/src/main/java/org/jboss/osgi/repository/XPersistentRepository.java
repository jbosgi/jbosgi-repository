package org.jboss.osgi.repository;

import java.util.List;

/**
 * An extension of the {@link XRepository} that provides capability persistence.
 *
 * @author thomas.diesler@jboss.com
 * @since 11-May-2012
 */
public interface XPersistentRepository extends XRepository {

    void addRepositoryDelegate(XRepository delegate);

    void removeRepositoryDelegate(XRepository delegate);

    List<XRepository> getRepositoryDelegates();
}
