package com.redgear.vine.task

import com.redgear.vine.repo.Repository
import com.redgear.vine.repo.impl.AetherRepo
import net.sourceforge.argparse4j.inf.Namespace
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarFile;

/**
 * Created by LordBlackHole on 8/16/2016.
 */
class InstallTask implements Task {

    private static final Logger log = LoggerFactory.getLogger(InstallTask.class)


    @Override
    void runTask(Path workingDir, Namespace namespace) {
        String nameArg = namespace.getString('name')
        String mainArg = namespace.getString('main')

        List<String> arguments =  namespace.getList('args')


        log.info "Installing"
        Repository repo = new AetherRepo()

        arguments.each {

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

            def name = nameArg ?: artifact

            def main = mainArg ?: new JarFile(mod.main).manifest.mainAttributes.getValue("Main-Class")

            if(!main)
                throw new RuntimeException("No main method specified!")



            File libDir =  workingDir.resolve("lib/$name").toFile()
            libDir.deleteDir()
            libDir.mkdirs()

            copy(libDir, mod.main)
            mod.dependencies.each { copy(libDir, it) }

            def binDir = workingDir.resolve('bin')

            createBatch(binDir.resolve("${name}.bat").toFile(), main, libDir)

            createBash(binDir.resolve(name).toFile(), main, libDir)
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
