package com.redgear.vine.repo.impl

import com.redgear.vine.config.Config
import com.redgear.vine.config.Coords
import com.redgear.vine.exception.VineException
import com.redgear.vine.repo.Repository
import org.apache.maven.repository.internal.MavenRepositorySystemUtils
import org.eclipse.aether.DefaultRepositorySystemSession
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.RepositorySystemSession
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.collection.CollectRequest
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory
import org.eclipse.aether.graph.Dependency
import org.eclipse.aether.graph.DependencyFilter
import org.eclipse.aether.impl.DefaultServiceLocator
import org.eclipse.aether.repository.LocalRepository
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.resolution.DependencyRequest
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory
import org.eclipse.aether.spi.connector.transport.TransporterFactory
import org.eclipse.aether.transport.file.FileTransporterFactory
import org.eclipse.aether.transport.http.HttpTransporterFactory
import org.eclipse.aether.util.artifact.JavaScopes
import org.eclipse.aether.util.filter.DependencyFilterUtils
import org.eclipse.aether.util.graph.selector.AndDependencySelector
import org.eclipse.aether.util.graph.selector.OptionalDependencySelector
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static com.redgear.vine.repo.Repository.*

/**
 * Created by LordBlackHole on 7/4/2016.
 */
class AetherRepo implements Repository {

    private static final Logger log = LoggerFactory.getLogger(AetherRepo.class)
    private final RepositorySystem system
    private final RepositorySystemSession session
    private final List<RemoteRepository> repos;

    AetherRepo(Config config) {
        system = newRepositorySystem();

        session = newRepositorySystemSession( system, config.localCache );

        repos = config.repos.collect {
            new RemoteRepository.Builder( it.name, "default", it.uri.toString() ).build()
        }
    }


    @Override
    Package resolvePackage(Coords coords) {

        def mod = "$coords.groupId:$coords.artifactId:${coords.ext?:'jar'}:${coords.classifier ? coords.classifier + ":" : ''}$coords.version"

        log.info "Resolving: {}", mod

        Artifact artifact = new DefaultArtifact(mod);

        DependencyFilter classpathFlter = DependencyFilterUtils.classpathFilter( JavaScopes.COMPILE, JavaScopes.RUNTIME, JavaScopes.PROVIDED );

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot( new Dependency( artifact, JavaScopes.COMPILE ) );
        collectRequest.setRepositories(repos);

        DependencyRequest dependencyRequest = new DependencyRequest( collectRequest, classpathFlter );

        def dependencyResult = system.resolveDependencies( session, dependencyRequest )

        def mainFile = dependencyResult.root.artifact.getFile()

        List<File> artifactResults = dependencyResult.getArtifactResults().collect{it.artifact.file}

        artifactResults.remove(mainFile)

        return new Package() {
            @Override
            File getMain() {
                return mainFile
            }

            @Override
            List<File> getDependencies() {
                return artifactResults
            }

            @Override
            String toString() {
                return mainFile.toString()
            }
        }
    }

    public static DefaultRepositorySystemSession newRepositorySystemSession(RepositorySystem system, File localRepoFile ) {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

        LocalRepository localRepo = new LocalRepository(localRepoFile);
        session.setLocalRepositoryManager( system.newLocalRepositoryManager( session, localRepo ) );

        session.setDependencySelector(new AndDependencySelector(session.getDependencySelector(), new OptionalDependencySelector().deriveChildSelector(null)))

        return session;
    }

    public static RepositorySystem newRepositorySystem() {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService( RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class );
        locator.addService( TransporterFactory.class, FileTransporterFactory.class );
        locator.addService( TransporterFactory.class, HttpTransporterFactory.class );

        locator.setErrorHandler( new DefaultServiceLocator.ErrorHandler() {
            @Override
            public void serviceCreationFailed( Class<?> type, Class<?> impl, Throwable exception ) {
                throw new VineException("Failed to resolve artifact: ", exception)
            }
        } );

        return locator.getService( RepositorySystem.class );
    }
}
