package com.redgear.vine.task

import com.redgear.vine.repo.Repository
import com.redgear.vine.repo.impl.AetherRepo
import net.sourceforge.argparse4j.inf.Namespace
import org.apache.commons.compress.archivers.zip.ZipFile
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarFile

/**
 * Created by LordBlackHole on 8/16/2016.
 */
class InstallTask implements Task {

    private static final Logger log = LoggerFactory.getLogger(InstallTask.class)

    private final Repository repo = new AetherRepo()

    @Override
    void runTask(Path workingDir, Namespace namespace) {
        String nameArg = namespace.getString('name')
        String mainArg = namespace.getString('main')

        List<String> arguments =  namespace.getList('args')


        log.info "Installing"

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

            def name = nameArg ?: artifact

            if(checkForBin(workingDir, name, group, artifact, version))
                return

            Repository.Package mod = repo.resolvePackage(group, artifact, version)



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

    boolean checkForBin(Path workingDir, String name, String group, String artifact, String version) {
        try {
            Repository.Package bin = repo.resolvePackage(group, artifact + ":zip:bin", version)

            def libDir = workingDir.resolve("lib/$name")

            libDir.toFile().mkdirs()

            log.info 'We found a bin: {}', bin

            def zip = new ZipFile(bin.main)

            zip.entries.each { entry ->
                def snippedName = entry.name.substring(entry.name.indexOf('/') + 1)

                log.info 'Found entry: {}, snipped: {}', entry.name, snippedName

                if(snippedName.isEmpty())
                    return

                if(snippedName.endsWith('/'))
                    libDir.resolve(snippedName).toFile().mkdir()
                else
                    libDir.resolve(snippedName).withOutputStream {
                        it << zip.getInputStream(entry)
                    }
            }

            def sourceDir = libDir.resolve('bin')

            if(sourceDir.toFile().exists()) {
                def children = sourceDir.toFile().listFiles()

                if(children) {
                    children.findAll {
                        it.name.endsWith('.cmd') || it.name.endsWith('.bat')
                    }.each {

                        log.info 'fileName: {}', it.name

                        def binDir = workingDir.resolve('bin')
                        def fileName = it.name.substring(0, it.name.lastIndexOf('.'))

                        createBinBatch(binDir.resolve(fileName + '.bat').toFile(), it)
                        createBinBash(binDir.resolve(fileName).toFile(), sourceDir.resolve(fileName).toFile())
                    }
                }
            }


        } catch (Exception e) {
            log.info "Couldn't find a bin", e
            return false
        }

        return true
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


    static void createBinBatch(File location, File libDir) {
        location.parentFile.mkdirs()

        location.delete()

        location << """
@echo off
$libDir %*
"""
    }

    static void createBinBash(File location, File libDir) {
        location.parentFile.mkdirs()

        location.delete()

        location << """
$libDir
"""
    }

}