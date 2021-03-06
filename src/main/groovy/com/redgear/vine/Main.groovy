package com.redgear.vine

import com.fasterxml.jackson.databind.ObjectMapper
import com.redgear.vine.config.Config
import com.redgear.vine.config.Options
import com.redgear.vine.exception.VineException
import com.redgear.vine.task.*
import net.sourceforge.argparse4j.ArgumentParsers
import net.sourceforge.argparse4j.impl.Arguments
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.file.Path
import java.nio.file.Paths

/**
 *
 * Main class and entry point to the application.
 *
 * @author Dillon Jett Callis
 * @version 0.1.0
 * @since 2016-7-4
 */
class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class)

    /** Used with ArgParse as key. **/
    private static final String taskKey = 'task'

    /**
     * Configs:
     *
     * Find config file ...
     *
     * command line argument pointing directly to config file. - Fail if config is not in specified location
     * %VINE_HOME%/config.json - if env is supplied, use. If config is missing, gen it there and then use.
     * user.home/.vine/config.json - If missing, gen there and then use.
     *
     * @param args Entry arguments
     */
    public static void main(String[] args) {

        try {
            def result = parseArgs(args)

            //TODO: This doesn't actually seem to work. We need to figure out how to configure simple or get a new slf4j logger we can configure programmatically.
            if (result.debug) {
                System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "all")
            }

            if (result.quiet) {
                System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "none")
            }

            def config = readConfigs(result.config)

            def task = result.task

            task.runTask(config, result)
        } catch (VineException e) {
            log.error e.message
            System.exit(100)
        } catch (Exception e) {
            log.error 'Unexpected error! Please check with https://github.com/RedGear/Vine/issues and if this issue is not listed, please report it. ', e
            System.exit(999)
        }
    }

    /**
     * Parses the input arguments and returns the ArgParse Namespace.
     *
     * @param args Input arguments
     * @return ArgParse Namespace object containing parsed arguments.
     */
    static Options parseArgs(String[] args) {

        def parser = ArgumentParsers.newArgumentParser('vine')

        parser.addArgument('-c', '--config').nargs('?').help('Alternative config file location')

        def logLevel = parser.addMutuallyExclusiveGroup('logLevel')

        logLevel.addArgument('-d', '--debug').action(Arguments.storeTrue()).help('Activate debug output')
        logLevel.addArgument('-q', '--quiet').action(Arguments.storeTrue()).help('Silences all logging, only results will be printed (this includes hiding errors!)')

        def subParsers = parser.addSubparsers().metavar("command")

        def install = subParsers.addParser('install').setDefault(taskKey, new InstallTask()).help('Install an application from maven coordinates')

        install.addArgument('-n', '--name').nargs('?').help('Set an alternative name for the program (default is artifactId)')
        install.addArgument('-m', '--main').nargs('?').help('Provide the Main class for this program (default is to look in Manifest)')
        install.addArgument('-a', '--additional').nargs('?').help('Provide a set of additional arguments to be inserted into the Run script for this program, which will pass them along to the JVM')
        install.addArgument('coords').metavar('coordinates').required(true).help('The groupId:artifactId:version of the application in Maven')

        def remove = subParsers.addParser('remove').setDefault(taskKey, new RemoveTask()).help('Remove an application installed with vine')
        remove.addArgument('name').metavar('name').required(true).help('Name of the application to remove')


        def list = subParsers.addParser('list').setDefault(taskKey, new ListTask()).help('List all applications installed by vine')
        list.addArgument('-v', '--verbose').action(Arguments.storeTrue()).help('Will print extra data on applications, including full maven coordinates')

        def resolve = subParsers.addParser('resolve').setDefault(taskKey, new ResolveTask())
                .help('''Download maven artifacts into local cache and return a ';' denoted string of all their full file paths ''')
        resolve.addArgument('-p', '--pretty').action(Arguments.storeTrue()).setDefault(false)
        resolve.addArgument('args').metavar('args').nargs('+').help('A list of groupId:artifactId:version maven coordinates')

        def cleanCache = subParsers.addParser('cleanCache').setDefault(taskKey, new CleanCacheTask()).help('Remove junk from your local maven cache.')

        cleanCache.addArgument('-l', '--locals').nargs('?').action(Arguments.storeTrue()).setDefault(false)
        cleanCache.addArgument('-s', '--snapshots').nargs('?').action(Arguments.storeTrue()).setDefault(false)
        cleanCache.addArgument('-j', '--jarMissing').nargs('?').action(Arguments.storeTrue()).setDefault(false)
        cleanCache.addArgument('-p', '--pomMissing').nargs('?').action(Arguments.storeTrue()).setDefault(false)
        cleanCache.addArgument('-e', '--empty').nargs('?').action(Arguments.storeFalse()).setDefault(true)

        //TODO: Enable this when rename is fixed.
//        def rename = subParsers.addParser('rename').setDefault(taskKey, new RenameTask()).help('Rename an application installed with vine')
//        rename.addArgument('name').metavar('name').required(true).help('Old name of the application')
//        rename.addArgument('newName').metavar('newName').required(true).help('New name of the application')

        def info = subParsers.addParser('info').setDefault(taskKey, new InfoTask()).help('Display information of an application installed with vine')
        info.addArgument('name').metavar('name').required(true).help('Name of the application to display')

        return (parser.parseArgsOrFail(args)).attrs as Options
    }

    /**
     * Reads in the config file, checking first the passed argument,
     * then the VINE_HOME system property,
     * and finally looking in ~/.vine
     *
     * If not found there, it will generate the default config.
     *
     * @param config File name of the Config file to load, or null to use the next level of defaults.
     * @return The config file that has been either loaded or generated
     * @throws VineException if a config file was passed in, but not found.
     */
    static Config readConfigs(String config) {
        log.debug 'Reading configs'

        if(config) {
            def file = Paths.get(config)

            if(file.toFile().exists()) {
                log.debug 'Config file was passed in as argument'
                return readConfigFile(file)
            } else {
                throw new VineException("Config file: ${config} not found!")
            }
        }

        String home = System.getProperty('VINE_HOME')

        if(home) {
            log.debug 'Found VINE_HOME, using that'

            def path = Paths.get(home, '/config.json')

            if(path.toFile().exists()) {
                return readConfigFile(path)
            } else {
                return generateDefaultConfigs(path)
            }
        }

        def path = Paths.get(System.getProperty('user.home'), '/.vine/config.json')

        if(path.toFile().exists()) {
            return readConfigFile(path)
        }
        else {
            return generateDefaultConfigs(path)
        }
    }

    /**
     * Reads the config file from the given path.
     * @param location Path of the config file.
     * @return The loaded config object.
     */
    static Config readConfigFile(Path location) {
        log.debug "Reading config file: ${location}"

        return new ObjectMapper().readValue(location.toFile(), Config.class)
    }

    /**
     * Writes out the default config to the given path.
     * @param location Path where to create the config.
     * @return The generated default config.
     */
    static Config generateDefaultConfigs(Path location) {
        log.debug "Generating default config file at: ${location}"

        Config config = new Config();

        config.installDir = location.parent.toFile()

        config.installDir.mkdirs()

        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(location.toFile(), config)

        return config
    }

}