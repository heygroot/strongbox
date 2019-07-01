package org.carlspring.strongbox.repository;

import org.carlspring.strongbox.storage.repository.RepositoryData;

import java.io.IOException;

/**
 * @author carlspring
 */
public interface RepositoryManagementStrategy
{

    void createRepository(String storageId,
                          String repositoryId)
            throws IOException, RepositoryManagementStrategyException;

    void createRepositoryStructure(RepositoryData repository)
            throws IOException;

    void removeRepository(String storageId,
                          String repositoryId)
            throws IOException;

    void removeDirectoryStructure(String storageId,
                                  String repositoryId)
            throws IOException;

}
