package org.carlspring.strongbox.services;

import org.carlspring.maven.commons.util.ArtifactUtils;
import org.carlspring.strongbox.client.ArtifactTransportException;
import org.carlspring.strongbox.config.CommonConfig;
import org.carlspring.strongbox.config.StorageApiConfig;
import org.carlspring.strongbox.configuration.ConfigurationManager;
import org.carlspring.strongbox.providers.ProviderImplementationException;
import org.carlspring.strongbox.resource.ConfigurationResourceResolver;
import org.carlspring.strongbox.resource.ResourceCloser;
import org.carlspring.strongbox.storage.ArtifactStorageException;
import org.carlspring.strongbox.storage.repository.Repository;
import org.carlspring.strongbox.storage.repository.RepositoryTypeEnum;
import org.carlspring.strongbox.testing.TestCaseWithArtifactGenerationWithIndexing;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import static org.junit.Assert.*;

/**
 * @author mtodorov
 */
@RunWith(SpringJUnit4ClassRunner.class)
public class ArtifactManagementServiceImplTest
        extends TestCaseWithArtifactGenerationWithIndexing
{

    private static final File STORAGE_BASEDIR = new File(ConfigurationResourceResolver.getVaultDirectory() + "/storages/storage0");

    private static final File REPOSITORY_BASEDIR = new File(STORAGE_BASEDIR, "/amsi-releases");

    @org.springframework.context.annotation.Configuration
    @Import({ CommonConfig.class, StorageApiConfig.class })
    public static class SpringConfig { }

    @Autowired
    private ArtifactManagementService artifactManagementService;

    @Autowired
    private ConfigurationManager configurationManager;


    @Before
    public void init()
            throws NoSuchAlgorithmException,
                   XmlPullParserException,
                   IOException,
                   JAXBException
    {
        /*
        if (!INITIALIZED)
        {
            //noinspection ResultOfMethodCallIgnored
            INDEX_DIR.mkdirs();

            String gavtc = "org.carlspring.strongbox:strongbox-utils::jar";

            generateArtifact(REPOSITORY_BASEDIR.getAbsolutePath(),
                             gavtc,
                             new String[] { "7.0" // Used by testForceDelete()
                                          });
            generateArtifact(STORAGE_BASEDIR.getAbsolutePath() + "/releases-with-trash", gavtc, new String[] {"7.2"});
            generateArtifact(STORAGE_BASEDIR.getAbsolutePath() + "/releases-with-redeployment", gavtc, new String[] {"7.3"});

            INITIALIZED = true;
        }
        */

        // Used by testDeploymentToRepositoryWithForbiddenDeployments()
        Repository repositoryWithoutDelete = new Repository("amsi-releases-without-delete");
        repositoryWithoutDelete.setStorage(configurationManager.getConfiguration().getStorage(STORAGE0));
        repositoryWithoutDelete.setAllowsDelete(false);

        createTestRepositoryWithArtifacts(repositoryWithoutDelete,
                                          "org.carlspring.strongbox:strongbox-utils",
                                          "8.0");

        // Used by testRedeploymentToRepositoryWithForbiddenRedeployments()
        Repository repositoryWithoutRedeployments = new Repository("amsi-releases-without-redeployment");
        repositoryWithoutRedeployments.setStorage(configurationManager.getConfiguration().getStorage(STORAGE0));
        repositoryWithoutRedeployments.setAllowsRedeployment(false);

        createTestRepositoryWithArtifacts(repositoryWithoutRedeployments,
                                          "org.carlspring.strongbox:strongbox-utils",
                                          "8.1");

        // Used by testDeletionFromRepositoryWithForbiddenDeletes()
        Repository repositoryWithoutDeletes = new Repository("amsi-releases-without-delete");
        repositoryWithoutDeletes.setStorage(configurationManager.getConfiguration().getStorage(STORAGE0));
        repositoryWithoutDeletes.setAllowsDelete(false);

        createTestRepositoryWithArtifacts(repositoryWithoutDeletes,
                                          "org.carlspring.strongbox:strongbox-utils",
                                          "8.2");

        // Used by:
        // - testForceDelete()
        // - testArtifactResolutionFromGroup()
        createTestRepositoryWithArtifacts(STORAGE0,
                                          "amsi-releases",
                                          "org.carlspring.strongbox:strongbox-utils",
                                          "7.0", // Used by testForceDelete()
                                          "7.3"  // Used by testArtifactResolutionFromGroup()
                                          );

        Repository repositoryGroup = new Repository("amsi-releases-group");
        repositoryGroup.setStorage(configurationManager.getConfiguration().getStorage(STORAGE0));
        repositoryGroup.setType(RepositoryTypeEnum.GROUP.getType());
        repositoryGroup.addRepositoryToGroup("amsi-releases");

        createRepository(repositoryGroup);

        // Used by testForceDelete()
        Repository repositoryWithTrash = new Repository("amsi-releases-with-trash");
        repositoryWithTrash.setStorage(configurationManager.getConfiguration().getStorage(STORAGE0));
        repositoryWithTrash.setTrashEnabled(true);

        createTestRepositoryWithArtifacts(repositoryWithTrash,
                                          "org.carlspring.strongbox:strongbox-utils",
                                          "7.2");
    }

    @Override
    public Map<String, String> getRepositoriesToClean()
    {
        Map<String, String> repositories = new LinkedHashMap<>();
        repositories.put(STORAGE0, "amsi-releases");
        repositories.put(STORAGE0, "amsi-releases-group");
        repositories.put(STORAGE0, "amsi-releases-with-trash");
        repositories.put(STORAGE0, "amsi-releases-with-deployment");
        repositories.put(STORAGE0, "amsi-releases-with-redeployment");

        return repositories;
    }

    @Test
    public void testDeploymentToRepositoryWithForbiddenDeployments()
            throws NoSuchAlgorithmException,
                   XmlPullParserException,
                   IOException,
                   ProviderImplementationException
    {
        InputStream is = null;

        //noinspection EmptyCatchBlock
        try
        {
            String repositoryId = "amsi-releases-without-delete";
            String gavtc = "org.carlspring.strongbox:strongbox-utils:8.0:jar";

            File repositoryDir = new File(STORAGE_BASEDIR, repositoryId);
            is = generateArtifactInputStream(repositoryDir.getAbsolutePath(), repositoryId, gavtc, true);

            Artifact artifact = ArtifactUtils.getArtifactFromGAVTC(gavtc);
            artifactManagementService.store(STORAGE0,
                                            repositoryId,
                                            ArtifactUtils.convertArtifactToPath(artifact),
                                            is);

            fail("Failed to deny artifact operation for repository with disallowed deployments.");
        }
        catch (ArtifactStorageException e)
        {
            // This is the expected correct behavior
        }
        finally
        {
            ResourceCloser.close(is, null);
        }
    }

    @Test
    public void testRedeploymentToRepositoryWithForbiddenRedeployments()
            throws NoSuchAlgorithmException,
                   XmlPullParserException,
                   IOException,
                   ProviderImplementationException
    {
        InputStream is = null;

        //noinspection EmptyCatchBlock
        try
        {
            String repositoryId = "amsi-releases-without-redeployment";
            String gavtc = "org.carlspring.strongbox:strongbox-utils:8.1:jar";

            generateArtifact(new File(STORAGE_BASEDIR, repositoryId).getAbsolutePath(), gavtc);

            File repositoryDir = new File(STORAGE_BASEDIR, repositoryId);
            is = generateArtifactInputStream(repositoryDir.getAbsolutePath(), repositoryId, gavtc, true);

            Artifact artifact = ArtifactUtils.getArtifactFromGAVTC(gavtc);
            artifactManagementService.store(STORAGE0,
                                            repositoryId,
                                            ArtifactUtils.convertArtifactToPath(artifact),
                                            is);

            fail("Failed to deny artifact operation for repository with disallowed re-deployments.");
        }
        catch (ArtifactStorageException e)
        {
            // This is the expected correct behavior
        }
        finally
        {
            ResourceCloser.close(is, null);
        }
    }

    @Test
    public void testDeletionFromRepositoryWithForbiddenDeletes()
            throws NoSuchAlgorithmException,
                   XmlPullParserException,
                   IOException
    {
        //noinspection EmptyCatchBlock
        try
        {
            String repositoryId = "amsi-releases-without-delete";
            String gavtc = "org.carlspring.strongbox:strongbox-utils:8.2:jar";

            Artifact artifact = ArtifactUtils.getArtifactFromGAVTC(gavtc);
            artifactManagementService.delete(STORAGE0,
                                             repositoryId,
                                             ArtifactUtils.convertArtifactToPath(artifact),
                                             false);

            fail("Failed to deny artifact operation for repository with disallowed deletions.");
        }
        catch (ArtifactStorageException e)
        {
            // This is the expected correct behavior
        }
    }

//    @Test
//    public void testDeploymentRedeploymentAndDeletionAgainstGroupRepository()
//            throws NoSuchAlgorithmException,
//                   XmlPullParserException,
//                   IOException, ProviderImplementationException
//    {
//        InputStream is = null;
//
//        String repositoryId = "group-releases";
//        String gavtc = "org.carlspring.strongbox:strongbox-utils:8.3:jar";
//
//        Artifact artifact = ArtifactUtils.getArtifactFromGAVTC(gavtc);
//
//        //noinspection EmptyCatchBlock
//        try
//        {
//            File repositoryDir = new File(STORAGE_BASEDIR, repositoryId);
//            is = generateArtifactInputStream(repositoryDir.getAbsolutePath(), repositoryId, gavtc, true);
//
//            artifactManagementService.store(STORAGE0,
//                                            repositoryId,
//                                            ArtifactUtils.convertArtifactToPath(artifact),
//                                            is);
//
//            fail("Failed to deny artifact operation for repository with disallowed deployments.");
//        }
//        catch (ArtifactStorageException e)
//        {
//            // This is the expected correct behavior
//        }
//        finally
//        {
//            ResourceCloser.close(is, null);
//        }
//
//        //noinspection EmptyCatchBlock
//        try
//        {
//            // Generate the artifact on the file-system anyway so that we could achieve
//            // the state of having it there before attempting a re-deployment
//            generateArtifact(new File(STORAGE_BASEDIR, repositoryId).getAbsolutePath(), gavtc);
//            artifactManagementService.store(STORAGE0,
//                                            repositoryId,
//                                            ArtifactUtils.convertArtifactToPath(artifact),
//                                            is);
//
//            fail("Failed to deny artifact operation for repository with disallowed re-deployments.");
//        }
//        catch (ArtifactStorageException e)
//        {
//            // This is the expected correct behavior
//        }
//        finally
//        {
//            ResourceCloser.close(is, null);
//        }
//
//        // Delete: Case 1: No forcing
//        //noinspection EmptyCatchBlock
//        try
//        {
//            artifactManagementService.delete(STORAGE0,
//                                             repositoryId,
//                                             ArtifactUtils.convertArtifactToPath(artifact),
//                                             false);
//
//            fail("Failed to deny artifact operation for repository with disallowed deletions (non-forced test).");
//        }
//        catch (ArtifactStorageException e)
//        {
//            // This is the expected correct behavior
//        }
//        finally
//        {
//            ResourceCloser.close(is, null);
//        }
//
//        // Delete: Case 2: Force delete
//        //noinspection EmptyCatchBlock
//        try
//        {
//            artifactManagementService.delete(STORAGE0,
//                                             repositoryId,
//                                             ArtifactUtils.convertArtifactToPath(artifact),
//                                             true);
//
//            fail("Failed to deny artifact operation for repository with disallowed deletions (forced test).");
//        }
//        catch (ArtifactStorageException e)
//        {
//            // This is the expected correct behavior
//        }
//        finally
//        {
//            ResourceCloser.close(is, null);
//        }
//    }

    @Test
    public void testArtifactResolutionFromGroup()
            throws IOException,
                   NoSuchAlgorithmException,
                   ArtifactTransportException,
                   ProviderImplementationException
    {
        InputStream is = artifactManagementService.resolve(STORAGE0,
                                                           "amsi-releases-group",
                                                           "org/carlspring/strongbox/strongbox-utils/7.3/strongbox-utils-7.3.jar");

        assertFalse("Failed to resolve artifact from group repository!", is == null);
        assertTrue("Failed to resolve artifact from group repository!", is.available() > 0);

        is.close();
    }

    @Test
    public void testForceDelete()
            throws IOException
    {
        final String artifactPath1 = "org/carlspring/strongbox/strongbox-utils/7.0/strongbox-utils-7.0.jar";
        artifactManagementService.delete(STORAGE0,
                                         "amsi-releases",
                                         artifactPath1,
                                         true);

        assertFalse("Failed to delete artifact during a force delete operation!",
                    new File(REPOSITORY_BASEDIR, artifactPath1).exists());

        final String artifactPath2 = "org/carlspring/strongbox/strongbox-utils/7.2/strongbox-utils-7.2.jar";
        artifactManagementService.delete(STORAGE0,
                                         "amsi-releases-with-trash",
                                         artifactPath2,
                                         true);

        final File repositoryDir = new File(STORAGE_BASEDIR, "amsi-releases-with-trash/.trash");

        assertTrue("Should have moved the artifact to the trash during a force delete operation, " +
                   "when allowsForceDeletion is not enabled!",
                   new File(repositoryDir, artifactPath2).exists());
    }

}
