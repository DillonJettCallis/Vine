package com.redgear.vine.repo.impl

import com.redgear.vine.repo.Repository
import org.apache.maven.repository.internal.MavenRepositorySystemUtils
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.RepositorySystemSession
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory
import org.eclipse.aether.graph.Dependency
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.impl.DefaultServiceLocator
import org.eclipse.aether.repository.LocalRepository
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory
import org.eclipse.aether.util.artifact.JavaScopes
import org.eclipse.aether.util.filter.DependencyFilterUtils
import org.eclipse.aether.util.graph.selector.AndDependencySelector
import org.eclipse.aether.util.graph.selector.OptionalDependencySelector
import org.slf4j.Logger
import org.slf4j.LoggerFactory;

/**
 * Created by LordBlackHole on 7/4/2016.
 */
class AetherRepo implements Repository {

    private static final Logger log = LoggerFactory.getLogger(AetherRepo.class)
    private final RepositorySystem system
    private final RepositorySystemSession session

    AetherRepo() {
        system = newRepositorySystem();

        session = newRepositorySystemSession( system );
    }


    @Override
    Repository.Package resolvePackage(String group, String artifactId, String version) {

        def mod = "$group:$artifactId:$version"

        log.info "Resolving: {}", mod

        Artifact artifact = new DefaultArtifact(mod);

        DependencyFilter classpathFlter = DependencyFilterUtils.classpathFilter( JavaScopes.COMPILE,  );

        DependencyFilterUtils.andFilter()

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot( new Dependency( artifact, JavaScopes.COMPILE ) );
        collectRequest.setRepositories(newRepositories( system, session ) );

        DependencyRequest dependencyRequest = new DependencyRequest( collectRequest, classpathFlter );

        def dependencyResult = system.resolveDependencies( session, dependencyRequest )

        def mainFile = dependencyResult.root.artifact.getFile()

        List<File> artifactResults = dependencyResult.getArtifactResults().collect{it.artifact.file}

        artifactResults.remove(mainFile)

        return new Repository.Package() {
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





    public static List<RemoteRepository> newRepositories(RepositorySystem system, RepositorySystemSession session )
    {
        return new ArrayList<RemoteRepository>( Arrays.asList( newCentralRepository() ) );
    }

    private static RemoteRepository newCentralRepository()
    {
        return new RemoteRepository.Builder( "central", "default", "https://repo1.maven.org/maven2/" ).build();
    }

    public static DefaultRepositorySystemSession newRepositorySystemSession(RepositorySystem system )
    {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

        LocalRepository localRepo = new LocalRepository(System.getProperty("user.home") + "/.m2/repository");
        session.setLocalRepositoryManager( system.newLocalRepositoryManager( session, localRepo ) );

        session.setDependencySelector(new AndDependencySelector(session.getDependencySelector(), new OptionalDependencySelector().deriveChildSelector(null)))

        return session;
    }



    public static RepositorySystem newRepositorySystem()
    {
        /*
         * Aether's components implement org.eclipse.aether.spi.locator.Service to ease manual wiring and using the
         * prepopulated DefaultServiceLocator, we only need to register the repository connector and transporter
         * factories.
         */
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService( RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class );
        locator.addService( TransporterFactory.class, FileTransporterFactory.class );
        locator.addService( TransporterFactory.class, HttpTransporterFactory.class );

        locator.setErrorHandler( new DefaultServiceLocator.ErrorHandler()
        {
            @Override
            public void serviceCreationFailed( Class<?> type, Class<?> impl, Throwable exception )
            {
                exception.printStackTrace();
            }
        } );

        return locator.getService( RepositorySystem.class );
    }
}
