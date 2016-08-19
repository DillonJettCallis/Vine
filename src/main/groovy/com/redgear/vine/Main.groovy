package com.redgear.vine

import com.fasterxml.jackson.databind.ObjectMapper
import com.redgear.vine.config.Config
import com.redgear.vine.exception.VineException
import com.redgear.vine.task.InstallTask
import com.redgear.vine.task.ListTask
import com.redgear.vine.task.RemoveTask
import com.redgear.vine.task.ResolveTask
import com.redgear.vine.task.Task
import net.sourceforge.argparse4j.ArgumentParsers
import net.sourceforge.argparse4j.impl.Arguments
import net.sourceforge.argparse4j.inf.Namespace
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.file.Path
import java.nio.file.Paths

class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class)
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
     * @param args
     */
    public static void main(String[] args) {

        try {

            def result = parseArgs(args)

            if (result.getBoolean('debug')) {
                System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "all")
            }

            if (result.getBoolean('quiet')) {
                System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "none")
            }

            def config = readConfigs(result.getString('config'))

            def task = (Task) result.get(taskKey)

            task.runTask(config, result)
        } catch (VineException e) {
            log.error e.message
            System.exit(100)
        } catch (Exception e) {
            log.error 'Unexpected error! Please check with https://github.com/DillonJettCallis/Vine/issues and if this issue is not listed, please report it. ', e
            System.exit(999)
        }
    }

    static Namespace parseArgs(String[] args) {
        def parser = ArgumentParsers.newArgumentParser('vine')

        parser.addArgument('-c', '--config').nargs('?').help('Alternative config file location')

        def logLevel = parser.addMutuallyExclusiveGroup('logLevel')

        logLevel.addArgument('-d', '--debug').action(Arguments.storeTrue()).help('Activate debug output')
        logLevel.addArgument('-q', '--quiet').action(Arguments.storeTrue()).help('Silences all logging, only results will be printed (this includes hiding errors!)')

        def subParsers = parser.addSubparsers().metavar("command")

        def install = subParsers.addParser('install').setDefault(taskKey, new InstallTask()).help('Install an application from maven coordinates')

        install.addArgument('-n', '--name').nargs('?').help('Set an alternative name for the program (default is artifactId)')
        install.addArgument('-m', '--main').nargs('?').help('Provide the Main class for this program (default is to look in Manifest)')
        install.addArgument('coords').metavar('coordinates').required(true).help('The groupId:artifactId:version of the application in Maven')

        def remove = subParsers.addParser('remove').setDefault(taskKey, new RemoveTask()).help('Remove an application installed with vine')
        remove.addArgument('name').metavar('name').required(true).help('Name of the application to remove')


        def list = subParsers.addParser('list').setDefault(taskKey, new ListTask()).help('List all applications installed by vine')
        list.addArgument('-v', '--verbose').action(Arguments.storeTrue()).help('Will print extra data on applications, including full maven coordinates')

        def resolve = subParsers.addParser('resolve').setDefault(taskKey, new ResolveTask())
                .help('''Download maven artifacts into local cache and return a ';' denoted string of all their full file paths ''')
        resolve.addArgument('-p', '--pretty').action(Arguments.storeTrue())setDefault(false)
        resolve.addArgument('args').metavar('args').nargs('+').help('A list of groupId:artifactId:version maven coordinates')


        return parser.parseArgsOrFail(args)

    }


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

    static Config readConfigFile(Path location) {
        log.debug "Reading config file: ${location}"

        return new ObjectMapper().readValue(location.toFile(), Config.class)
    }

    static Config generateDefaultConfigs(Path location) {
        log.debug "Generating default config file at: ${location}"

        Config config = new Config();

        config.installDir = location.parent.toFile()

        config.installDir.mkdirs()

        new ObjectMapper().writeValue(location.toFile(), config)

        return config
    }

}