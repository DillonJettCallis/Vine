package com.redgear.vine.task

import com.fasterxml.jackson.databind.ObjectMapper
import com.redgear.vine.config.*
import com.redgear.vine.exception.VineException
import com.redgear.vine.repo.Repository
import com.redgear.vine.repo.impl.AetherRepo
import org.apache.commons.compress.archivers.zip.ZipFile
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarFile

/**
 * Task for installing a maven package into the VINE_HOME/bin dir.
 *
 * Example: 'vine install com.redgear:vine:0.1.0'
 *
 * Optional arguments:
 * -n or --name: The name of the package, used in the name of the startup script,
 * the folder and config files and will be the name used to remove or upgrade the artifact later.
 * Defaults to the artifactId.
 *
 * -m or --main: The main class of the package to be used in the startup script.
 * Defaults to the Main-Class in the package's manifest. If neither of these are supplied,
 * the install will fail.
 *
 * -a or --additional: Extra arguments to be put into the startup script and passed
 * to the JVM every time the project is run. Used for classes like leiningen which needs to run
 * Clojure's main method and then needs to pass in the extra args '-m leiningen.core.main' to tell
 * Clojure to run leiningen. In theory you should be able to pass in JVM args to configure memory or GC
 * if you wanted, or to set other params.
 *
 *
 * Maven Coords:
 *
 * Maven coordinates have several different combinations. The last argument will be split by ':' and the number
 * of results to this determine what will happen next.
 *
 * One argument:
 * Example: vine install vine
 * name: vine
 *
 * 'vine' will be interpreted as the short-name of the package, and the endorsed configs will be checked to find
 * the groupId and artifactId. Then the version lookup will be run and will install the latest version found.
 *
 * Two arguments:
 * Example: vine install vine:0.1.0
 * name: vine
 * version: 0.1.0
 *
 * Like with one arg, 'vine' will be considered a short-name, and the endorsed configs will be checked. Unlike one are
 * though, the version will be taken and used directly.
 *
 * Three arguments:
 * Example: vine install com.redgear:vine:0.1.0
 * groupId: com.redgear
 * artifactId: vine
 * version: 0.1.0
 *
 * Here no assumptions are made, the groupId, artifactId and version are taken literally.
 *
 * Four arguments:
 * Example: vine install com.redgear:vine:jar:0.1.0
 * groupId: com.redgear
 * artifactId: vine
 * version: 0.1.0
 *
 * Just like three args, but the third one is skipped, assumed to be the extension which isn't useful to us.
 *
 * @author Dillon Jett Callis
 * @version 0.1.0
 * @since 2016-8-16
 */
class InstallTask implements Task {

    private static final Logger log = LoggerFactory.getLogger(InstallTask.class)

    /**
     * Main entry point to this Task from outside.
     * @param config Parsed Config file.
     * @param namespace ArgParse options.
     */
    @Override
    void runTask(Config config, Options options) {
        //TODO: Look into breaking this task down further. Some parts could be re-used elsewhere.

        Repository repo = new AetherRepo(config)
        def workingDir = config.installDir.toPath()
        String nameArg = options.name

        log.info 'Installing'


        def coords = parseCoords(config, options.coords)

        def name = nameArg ?: coords.name ?: coords.artifactId

        checkConfig(workingDir, name)

        if(coords.ext == 'zip') {
            installBin(repo, workingDir, name, coords)
        } else {
            installJar(repo, coords, workingDir, name, options)
        }


    }

    private static void installJar(Repository repo, Coords coords, Path workingDir, String name, Options options) {
        Repository.Package mod = repo.resolvePackage(coords)

        String mainArg = options.main
        String additionalArgs = options.additional ?: coords.additionalArgs ?: ''

        def main = mainArg ?: coords.main ?: new JarFile(mod.main).manifest.mainAttributes.getValue("Main-Class")

        if (!main)
            throw new VineException("No main method specified!")


        File libDir = workingDir.resolve("lib/$name").toFile()
        libDir.deleteDir()
        libDir.mkdirs()


        def data = new InstalledData()

        data.installDir = libDir
        data.groupId = coords.groupId
        data.artifactId = coords.artifactId
        data.version = coords.version
        data.name = name
        data.type = InstallType.JAR
        data.main = main

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

    /**
     * Checks maven for a binary zip of this package, and if it finds one will will install that, then
     * make short-cut scripts to all of that package's scripts inside it's bin folder.
     * @param repo The source Repo we're using.
     * @param workingDir The VINE_HOME dir.
     * @param name The name of this package, used for generating configs and folders for it.
     * @param group The package's groupId
     * @param artifact The package's artifactId
     * @param version The package's version
     * @return true if the bin was found, false if not.
     */
    static void installBin(Repository repo, Path workingDir, String name, Coords coords) {

        Repository.Package bin = repo.resolvePackage(coords)

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
        data.groupId = coords.groupId
        data.artifactId = coords.artifactId
        data.version = coords.version
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
    }

    /**
     * Copies second arg into the folder of the first arg.
     * @param dir Folder to copy file into.
     * @param localFile File to copy into dir.
     * @return The resulting copied file.
     */
    static File copy(File dir, File localFile) {
        def fileName = localFile.name

        def newLocal = new File(dir, fileName)

        log.debug "Copying: {}", fileName

        newLocal.delete()

        Files.copy(localFile.toPath(), newLocal.toPath())

        return newLocal
    }

    /**
     * Creates a .bat file at the location, passing in the main class, the libDir as -classpath and additionalArgs
     * @param location The file to create.
     * @param main The main class of the package.
     * @param libDir The dir to be used as the classpath.
     * @param additionalArgs And extra args we want to add to this script.
     */
    static void createBatch(File location, String main, File libDir, String additionalArgs) {
        location.parentFile.mkdirs()

        location.delete()

        location << """
@echo off
java -classpath "$libDir/*" $main $additionalArgs %*
"""
    }

    /**
     * Creates a shell script at the location, passing in the main class, the libDir as -classpath and additionalArgs
     * @param location The file to create.
     * @param main The main class of the package.
     * @param libDir The dir to be used as the classpath.
     * @param additionalArgs And extra args we want to add to this script.
     */
    static void createBash(File location, String main, File libDir, String additionalArgs) {
        location.parentFile.mkdirs()

        location.delete()

        location << """
java -classpath "$libDir/*" $main $additionalArgs "\$@"
"""

        location.setExecutable(true, true)
    }

    /**
     * Creates a .bat file that directs all it's output to another .bat file.
     * @param location The new file to create.
     * @param libDir The file to direct the new file to.
     * @throws VineException if a script with this name already exists in the target dir.
     */
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

    /**
     * Creates a shell script that directs all it's output to another script.
     * @param location The new file to create.
     * @param libDir The file to direct the new file to.
     * @throws VineException if a script with this name already exists in the target dir.
     */
    static void createBinBash(File location, File libDir) {
        if(location.exists())
            throw new VineException("Bash script ${location} already exists. Have you allready installed this application?")

        location.parentFile.mkdirs()

        location.delete()

        location << """
$libDir "\$@"
"""
        location.setExecutable(true, true)
    }

    /**
     * Checks to see if there is already a config file for this package and if so throws and exception.
     * @param workingDir The VINE_HOME path.
     * @param name The name of the package to check.
     * @throws VineException if there is already a package with this name.
     */
    static void checkConfig(Path workingDir, String name) {
        def file = workingDir.resolve('data').resolve(name).toFile()

        if(file.exists()) {
            throw new VineException("Artifact ${name} is already installed!")
        }

    }

    /**
     * Writes out the InstalledData file for a package.
     * @param location The File to put the install data.
     * @param data The Data object to serialize to Json.
     */
    static void writeData(File location, InstalledData data) {
        location.parentFile.mkdirs()

        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(location, data)
    }

    /**
     * Tries to look up an Endorsed package by name.
     * @param config
     * @param name
     * @throws VineException if no such short-name can be found.
     * @return The Endorsed Package that contains the data on this package.
     */
    static Coords lookupPackage(Config config, String name) {
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

    /**
     * Parses out the Maven coordinates from the raw string.
     * @param config Config file, needed to look up Endorsed Packages.
     * @param coords The raw maven coords: 'com.redgear:vine:0.1.0'
     * @return Parsed Maven Coords.
     * @throws VineException if the coords couldn't be parsed or were invalid.
     */
    static Coords parseCoords(Config config, String coords) {
        def split = coords.split(":")


        switch (split.length) {
            case 1:
                return lookupPackage(config, split[0])
            case 2:
                def coord = lookupPackage(config, split[0])
                coord.version = split[1]
                return coord
            case 3:
                return new Coords(groupId: split[0], artifactId: split[1], version: split[2])
            case 4:
                return new Coords(groupId: split[0], artifactId: split[1], ext: split[2], version: split[3])
            case 5:
                return new Coords(groupId: split[0], artifactId: split[1], ext: split[2], classifier: split[3], version: split[4])
            default:
                throw new VineException("Invalid Maven Coords: $coords")
        }
    }

}



