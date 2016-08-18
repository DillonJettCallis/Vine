package com.redgear.vine.repo.impl

import com.redgear.vine.repo.Repository
import org.apache.ivy.Ivy
import org.apache.ivy.core.module.id.ModuleRevisionId
import org.apache.ivy.core.resolve.ResolveOptions
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class IvyRepo implements Repository {

    private static final Logger log = LoggerFactory.getLogger(IvyRepo.class)
    private final Ivy ivy = Ivy.newInstance()
    private final ResolveOptions resolveOptions = new ResolveOptions().setConfs('default');

    IvyRepo() {

        File ivySettings = new File(System.getProperty("user.home") + "/.jpm/ivysettings.xml")

        if (!ivySettings.exists()) {
            createDefaultSettings(ivySettings)
        }

        ivy.configure(ivySettings)
    }

    @Override
    Repository.Package resolvePackage(String group, String artifact, String version) {
        ModuleRevisionId mod = ModuleRevisionId.newInstance(group, artifact, version)


        def report = ivy.resolve(mod, resolveOptions, false)

        if (report.hasError()) {
            throw new RuntimeException(report.allProblemMessages.join(", "))
        }

        def artifacts = report.getAllArtifactsReports().toList()

        def mainArtifact = report.getArtifactsReports(mod)[0]

        artifacts.remove(mainArtifact)

        def deps = artifacts.collect{it.localFile}

        def main = mainArtifact.localFile

        return new Repository.Package() {
            @Override
            File getMain() {
                return main
            }

            @Override
            List<File> getDependencies() {
                return deps
            }
        }
    }

    static void createDefaultSettings(File settings) {
        log.info "Creating default ivy settings file"

        settings.parentFile.mkdir()

        settings << '''
<ivysettings>
    <settings defaultResolver="default"/>
    <resolvers>
        <chain name="default">
            <ibiblio name="local-m2" m2compatible="true" root="file:///${user.home}/.m2/repository"/>
            <ibiblio name="central" m2compatible="true"/>
        </chain>
    </resolvers>
</ivysettings>
'''
    }
}
