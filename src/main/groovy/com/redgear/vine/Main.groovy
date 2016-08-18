package com.redgear.vine

import com.fasterxml.jackson.databind.ObjectMapper
import com.redgear.vine.config.Config
import com.redgear.vine.task.InstallTask
import com.redgear.vine.task.ListTask
import com.redgear.vine.task.RemoveTask
import com.redgear.vine.task.Task
import groovy.json.JsonSlurper
import net.sourceforge.argparse4j.ArgumentParsers
import org.slf4j.Logger
import org.slf4j.LoggerFactory

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
        install.addArgument('args').metavar('args').nargs('*')

        def remove = subparsers.addParser('remove').setDefault(taskKey, new RemoveTask())
        remove.addArgument('args').metavar('args').nargs('*')


        def list = subparsers.addParser('list').setDefault(taskKey, new ListTask())


        def result = parser.parseArgsOrFail(args)


        readConfigs(result.getString('config'))



        def workingDir = Paths.get(System.getProperty('user.home'), '/.vine')


        def task = (Task) result.get(taskKey)

        task.runTask(workingDir, result)
    }


    static String readConfigs(String config) {
        ObjectMapper mapper = new ObjectMapper();
        //TODO: Actually read configs.

        if(config)
            return config

        String home = System.getProperty('VINE_HOME')

        if(home) {
            return home + '/config.json'
        }

        return System.getProperty('user.home') + '/.vine/config.json'
    }

}