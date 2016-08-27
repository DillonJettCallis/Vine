package com.redgear.vine.task

import com.fasterxml.jackson.databind.ObjectMapper
import com.redgear.vine.config.Config
import com.redgear.vine.config.EndorsedConfig
import com.redgear.vine.config.EndorsedPackage
import com.redgear.vine.config.InstallType
import com.redgear.vine.config.InstalledData
import com.redgear.vine.exception.VineException
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

    @Override
    void runTask(Config config, Namespace namespace) {
        Repository repo = new AetherRepo(config)
        def workingDir = config.installDir.toPath()
        String nameArg = namespace.getString('name')
        String mainArg = namespace.getString('main')


        log.info 'Installing'


        def coords = parseCoords(config, namespace.getString('coords'))


        def group = coords.groupId
        def artifact = coords.artifactId
        def version = coords.version

        def name = nameArg ?: coords.name ?: artifact

        String additionalArgs = namespace.getString('additional') ?: coords.additionalArgs ?: ''

        log.info 'additionalArgs: {}', additionalArgs

        checkConfig(workingDir, name)


        if(checkForBin(repo, workingDir, name, group, artifact, version))
            return

        Repository.Package mod = repo.resolvePackage(group, artifact, version)



        def main = mainArg ?: coords.main ?: new JarFile(mod.main).manifest.mainAttributes.getValue("Main-Class")

        if(!main)
            throw new VineException("No main method specified!")


        File libDir =  workingDir.resolve("lib/$name").toFile()
        libDir.deleteDir()
        libDir.mkdirs()


        def data = new InstalledData()

        data.installDir = libDir
        data.groupId = group
        data.artifactId = artifact
        data.version = version
        data.name = name
        data.type = InstallType.JAR

        copy(libDir, mod.main)
        mod.dependencies.each { copy(libDir, it) }

        def binDir = workingDir.resolve('bin')

        def batFile = binDir.resolve("${name}.bat").toFile()

        createBatch(batFile, main, libDir, additionalArgs)

        def bashFile = binDir.resolve(name).toFile()

        createBash(bashFile, main, libDir, additionalArgs)

        data.scripts = [batFile, bashFile]

        writeData(workingDir.resolve('data').resolve(name + '.json').toFile(), data)


    }

    static boolean checkForBin(Repository repo, Path workingDir, String name, String group, String artifact, String version) {
        try {
            Repository.Package bin = repo.resolvePackage(group, artifact + ":zip:bin", version)

            def libDir = workingDir.resolve("lib/$name")

            libDir.toFile().mkdirs()

            log.info 'We found a bin: {}', bin

            def zip = new ZipFile(bin.main)

            zip.entries.each { entry ->
                def snippedName = entry.name.substring(entry.name.indexOf('/') + 1)

                log.debug 'Found entry: {}, snipped: {}', entry.name, snippedName

                if(snippedName.isEmpty())
                    return

                if(snippedName.endsWith('/'))
                    libDir.resolve(snippedName).toFile().mkdir()
                else
                    libDir.resolve(snippedName).withOutputStream {
                        it << zip.getInputStream(entry)
                    }
            }

            def data = new InstalledData()

            data.installDir = libDir.toFile()
            data.groupId = group
            data.artifactId = artifact
            data.version = version
            data.name = name
            data.type = InstallType.BIN
            data.scripts = []

            def sourceDir = libDir.resolve('bin')

            if(sourceDir.toFile().exists()) {
                def children = sourceDir.toFile().listFiles()

                if(children) {
                    children.findAll {
                        it.name.endsWith('.cmd') || it.name.endsWith('.bat')
                    }.each {

                        log.debug 'Found batch script: {}', it.name

                        def binDir = workingDir.resolve('bin')
                        def fileName = it.name.substring(0, it.name.lastIndexOf('.'))

                        def batFile = binDir.resolve(fileName + '.bat').toFile()

                        createBinBatch(batFile, it)

                        data.scripts.add(batFile)


                        def bashSource = sourceDir.resolve(fileName).toFile()

                        if(bashSource.exists()) {
                            def bashFile = binDir.resolve(fileName).toFile()

                            log.debug 'Found bash script: {}', fileName

                            createBinBash(bashFile, bashSource)

                            data.scripts.add(bashFile)
                        }
                    }
                }
            }

            writeData(workingDir.resolve('data').resolve(name + '.json').toFile(), data)

        } catch (Exception e) {
            log.info "Couldn't find a bin"
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


    static void createBatch(File location, String main, File libDir, String additionalArgs) {
        location.parentFile.mkdirs()

        location.delete()

        location << """
@echo off
java -classpath "$libDir/*" $main $additionalArgs %*
"""
    }

    static void createBash(File location, String main, File libDir, String additionalArgs) {
        location.parentFile.mkdirs()

        location.delete()

        location << """
java -classpath "$libDir/*" $main $additionalArgs "\$@"
"""
    }


    static void createBinBatch(File location, File libDir) {
        if(location.exists())
            throw new VineException("Batch script ${location} already exists. Have you allready installed this application?")

        location.parentFile.mkdirs()

        location.delete()

        location << """
@echo off
$libDir %*
"""
    }

    static void createBinBash(File location, File libDir) {
        if(location.exists())
            throw new VineException("Bash script ${location} already exists. Have you allready installed this application?")

        location.parentFile.mkdirs()

        location.delete()

        location << """
$libDir
"""
    }


    static void checkConfig(Path workingDir, String name) {
        def file = workingDir.resolve('data').resolve(name).toFile()

        if(file.exists()) {
            throw new VineException("Artifact ${name} is already installed!")
        }

    }

    static void writeData(File location, InstalledData data) {
        location.parentFile.mkdirs()

        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(location, data)
    }

    static EndorsedPackage lookupPackage(Config config, String name) {
        for(uri in config.endorsedConfigs) {

            EndorsedConfig conf = null

            new BufferedReader(new InputStreamReader(uri.toURL().openStream())).withReader {
                conf = new ObjectMapper().readValue(it, EndorsedConfig.class)
            }


            if(conf != null) {
                def pack = conf.packages.find { it.name == name }

                if (pack != null)
                    return pack
            }

        }

        throw new VineException("Tried to look up endorsed package $name but was unable to find it. ")
    }

    static Coords parseCoords(Config config, String coords) {
        def split = coords.split(":")


        switch (split.length) {
            case 1:
                return new Coords(lookupPackage(config, split[0]))
            case 2:
                def coord = new Coords(lookupPackage(config, split[0]))
                coord.version = split[1]
                return coord
            case 3:
                return new Coords(groupId: split[0], artifactId: split[1], version: split[2])
            case 4:
                return new Coords(groupId: split[0], artifactId: split[1], version: split[3])
            default:
                throw new VineException("Invalid Maven Coords: $coords")
        }
    }

}

class Coords {

    Coords() {

    }

    Coords(EndorsedPackage endorsedPackage) {
        this.name = endorsedPackage.name
        this.groupId = endorsedPackage.groupId
        this.artifactId = endorsedPackage.artifactId
        this.main = endorsedPackage.main
        this.additionalArgs = endorsedPackage.additionalArgs
    }

    String name

    String groupId

    String artifactId

    String version

    String main

    String additionalArgs

}

