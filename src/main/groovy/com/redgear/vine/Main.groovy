package com.redgear.vine

import com.redgear.vine.task.InstallTask
import com.redgear.vine.task.ListTask
import com.redgear.vine.task.RemoveTask
import com.redgear.vine.task.Task
import net.sourceforge.argparse4j.ArgumentParsers
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.file.Paths

class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class)

    public static void main(String[] args) {
        def workingDir = Paths.get(System.getProperty('user.home'), '/.vine')

        def taskKey = 'task'
        def parser = ArgumentParsers.newArgumentParser('VINE')


        def subparsers = parser.addSubparsers().metavar("COMMAND")

        def install = subparsers.addParser('install').setDefault(taskKey, new InstallTask())

        install.addArgument('--name').nargs('?').help('Set an alternative name for the program')
        install.addArgument('--main').nargs('?').help('Provide the Main class for this program')
        install.addArgument('args').metavar('args').nargs('*')

        def remove = subparsers.addParser('remove').setDefault(taskKey, new RemoveTask())
        remove.addArgument('args').metavar('args').nargs('*')


        def list = subparsers.addParser('list').setDefault(taskKey, new ListTask())


        def result = parser.parseArgsOrFail(args)

        log.info "Result: $result"


        def task = (Task) result.get(taskKey)

        task.runTask(workingDir, result)
    }

}