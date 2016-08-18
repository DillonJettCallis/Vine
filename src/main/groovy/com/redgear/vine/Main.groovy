package com.redgear.vine

import com.fasterxml.jackson.databind.ObjectMapper
import com.redgear.vine.config.Config
import com.redgear.vine.task.InstallTask
import com.redgear.vine.task.ListTask
import com.redgear.vine.task.RemoveTask
import com.redgear.vine.task.ResolveTask
import com.redgear.vine.task.Task
import net.sourceforge.argparse4j.ArgumentParsers
import net.sourceforge.argparse4j.impl.Arguments
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.file.Path
import java.nio.file.Paths

class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class)

    /**
     * Configs:
     *
     * Find config file ...
     *
     * command line argument pointing directly to config file. - Fail if config is not in specified location
     * %VINE_HOME%/config.json - if env is supplied, use. If config is missing, gen it there and then use.
     * user.home/.vine/config.json - If missing, gen there and then use.
     *
     *
     *
     *
     * @param args
     */
    public static void main(String[] args) {
        def taskKey = 'task'
        def parser = ArgumentParsers.newArgumentParser('VINE')

        parser.addArgument("--config").nargs('?').help('Alternative config file location')

        def subparsers = parser.addSubparsers().metavar("COMMAND")

        def install = subparsers.addParser('install').setDefault(taskKey, new InstallTask())

        install.addArgument('--name').nargs('?').help('Set an alternative name for the program')
        install.addArgument('--main').nargs('?').help('Provide the Main class for this program')
        install.addArgument('coords').required(true)

        def remove = subparsers.addParser('remove').setDefault(taskKey, new RemoveTask())
        remove.addArgument('args').metavar('args').nargs('+')


        def list = subparsers.addParser('list').setDefault(taskKey, new ListTask())
        list.addArgument('--verbose').action(Arguments.storeTrue());

        def resolve = subparsers.addParser('resolve').setDefault(taskKey, new ResolveTask())
        resolve.addArgument('--pretty').action(Arguments.storeTrue())setDefault(false)
        resolve.addArgument('args').metavar('args').nargs('+')


        def result = parser.parseArgsOrFail(args)

        def config = readConfigs(result.getString('config'))

        def task = (Task) result.get(taskKey)

        task.runTask(config, result)
    }


    static Config readConfigs(String config) {
        if(config) {
            def file = Paths.get(config)

            if(file.toFile().exists())
                return readConfigFile(file)
            else
                throw new RuntimeException("Config file: ${config} not found!")
        }

        String home = System.getProperty('VINE_HOME')

        if(home) {
            def path = Paths.get(home, '/config.json')

            if(path.toFile().exists())
                return readConfigFile(path)
            else
                return generateDefaultConfigs(path)
        }

        def path = Paths.get(System.getProperty('user.home'), '/.vine/config.json')

        if(path.toFile().exists())
            return readConfigFile(path)
        else
            return generateDefaultConfigs(path)
    }

    static Config readConfigFile(Path location) {
        return new ObjectMapper().readValue(location.toFile(), Config.class)
    }

    static Config generateDefaultConfigs(Path location) {
        Config config = new Config();

        config.installDir = location.parent.toFile()

        config.installDir.mkdirs()

        new ObjectMapper().writeValue(location.toFile(), config)

        return config
    }

}