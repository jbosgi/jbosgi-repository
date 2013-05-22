package org.jboss.osgi.repository;

/**
 * An extension of the {@link XRepository} that provides capability persistence.
 *
 * @author thomas.diesler@jboss.com
 * @since 11-May-2012
 */
public interface XPersistentRepository extends XRepository {

    /**
     * Get the associated repository storage
     */
    RepositoryStorage getRepositoryStorage();
}
