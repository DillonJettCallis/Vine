package com.redgear.jpm

import com.redgear.jpm.repos.Repository
import com.redgear.jpm.repos.impl.AetherRepo
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.file.Files
import java.util.jar.JarFile

class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class)

    public static void main(String[] args) {
        def cli = new CliBuilder(usage: 'jpm [COMMAND]')

        log.info "Starting"

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

        log.info "Running"

        if (options.i) {

            log.info "Installing"
            Repository repo = new AetherRepo()

            options.arguments().each {

                def split = it.split(":")

                if(split.length < 3 || split.length > 4) {
                    throw new RuntimeException("Invalid Maven Coords: $it")
                }

                def group = split[0]
                def artifact = split[1]
                def version = split[2]

                //So we can take the ones that have group:artifact:jar:version.
                if(split.length == 4) {
                    artifact = split[2]
                    version = split[3]
                }


                Repository.Package mod = repo.resolvePackage(group, artifact, version)

                def name = options.name ?: artifact

                def main = options.main ?: new JarFile(mod.main).manifest.mainAttributes.getValue("Main-Class")

                if(!main)
                    throw new RuntimeException("No main method specified!")

                File libDir = new File(System.getProperty("user.home") + "/.jpm/lib/$name")
                libDir.deleteDir()
                libDir.mkdirs()

                copy(libDir, mod.main)
                mod.dependencies.each { copy(libDir, it) }

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

    static File copy(File dir, File localFile) {
        def fileName = localFile.name

        def newLocal = new File(dir, fileName)

        log.debug "Copying: {}", fileName

        newLocal.delete()

        Files.copy(localFile.toPath(), newLocal.toPath())

        return newLocal
    }


    static void createBatch(File location, String main, File libDir) {
        location.parentFile.mkdirs()

        location.delete()

        location << """
@echo off
java -classpath "$libDir/*" $main %*
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