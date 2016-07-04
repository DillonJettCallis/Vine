package com.redgear.jpm

import org.apache.ivy.Ivy
import org.apache.ivy.core.IvyContext
import org.apache.ivy.core.module.id.ModuleRevisionId
import org.apache.ivy.core.report.ArtifactDownloadReport
import org.apache.ivy.core.resolve.ResolveOptions
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.file.Files
import java.util.jar.JarFile

class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class)

    public static void main(String[] args) {
        def cli = new CliBuilder(usage: 'jpm [COMMAND]')

        cli.with {
            h longOpt: 'help', 'Show usage information'
            v longOpt: 'verbose', 'Prints more debug information'

            i longOpt: 'install', 'Install artifact "package"'
            n longOpt: 'name', numberOfArgs: 1, argName: 'name', 'Name to give installed package'
            m longOpt: 'main', numberOfArgs: 1, argName: 'main', 'Main method of installed package. Defaults to using manifest inside main .jar'

            u longOpt: 'uninstall', 'Uninstall artifact "package"'
        }

        def options = cli.parse(args)

        if(options.v) {
            System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug")
        }

        log.debug "Arguments: {}", args

        if (options.h) {
            cli.usage()
            return
        }

        log.debug "Running"

        if (options.i) {
            File ivySettings = new File(System.getProperty("user.home") + "/.jpm/ivysettings.xml")

            if (!ivySettings.exists()) {
                createDefaultSettings(ivySettings)
            }

            def ivy = Ivy.newInstance()

            ivy.configure(ivySettings)

            log.info "Installing: "

            String[] confs = ['default'];
            ResolveOptions resolveOptions = new ResolveOptions().setConfs(confs);

            options.arguments().each {

                def split = it.split(":")

                ModuleRevisionId mod = ModuleRevisionId.newInstance(split[0], split[1], split[2])

                log.info "Resolving: {}", mod

                def report = ivy.resolve(mod, resolveOptions, false)

                if (report.hasError()) {
                    throw new RuntimeException(report.allProblemMessages.join(", "))
                }

                def artifacts = report.getAllArtifactsReports().toList()

                def mainArtifact = report.getArtifactsReports(mod)[0]

                def name = options.name ?: mainArtifact.artifact.name

                File libDir = new File(System.getProperty("user.home") + "/.jpm/lib/$name")
                libDir.deleteDir()
                libDir.mkdirs()

                artifacts.each { copy(libDir, it) }

                def main = options.main ?: new JarFile(mainArtifact.localFile).manifest.mainAttributes.getValue("Main-Class")

                if(!main)
                    throw new RuntimeException("No main method specified!")

                createBatch(new File(System.getProperty("user.home") + "/.jpm/bin/${name}.bat"), main, libDir)

                createBash(new File(System.getProperty("user.home") + "/.jpm/bin/${name}"), main, libDir)
            }

            return
        }

        if(options.u) {
            options.arguments().each { name ->
                new File(System.getProperty("user.home") + "/.jpm/lib/$name").deleteDir()
                new File(System.getProperty("user.home") + "/.jpm/bin/${name}.bat").delete()
                new File(System.getProperty("user.home") + "/.jpm/bin/${name}").delete()
            }
        }
    }

    static File copy(File dir, ArtifactDownloadReport report) {
        def localFile = report.localFile
        def fileName = localFile.name

        def newLocal = new File(dir, fileName)

        log.debug "Copying: {}", fileName

        newLocal.delete()

        Files.copy(localFile.toPath(), newLocal.toPath())

        return newLocal
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


    static void createBatch(File location, String main, File libDir) {
        location.parentFile.mkdirs()

        location.delete()

        location << """
@echo off
java -classpath "$libDir/*" $main
"""
    }

    static void createBash(File location, String main, File libDir) {
        location.parentFile.mkdirs()

        location.delete()

        location << """
java -classpath "$libDir/*" $main
"""
    }

}