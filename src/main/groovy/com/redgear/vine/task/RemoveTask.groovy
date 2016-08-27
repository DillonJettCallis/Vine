package com.redgear.vine.task

import com.fasterxml.jackson.databind.ObjectMapper
import com.redgear.vine.config.Config
import com.redgear.vine.config.InstalledData
import com.redgear.vine.exception.VineException
import net.sourceforge.argparse4j.inf.Namespace
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Task to remove or uninstall an installed package.
 *
 * Example: 'vine remove ant'
 * Uninstalls the ant package.
 *
 * @author Dillon Jett Callis
 * @version 0.1.0
 * @since 2016-8-16
 */
class RemoveTask implements Task {

    private static final Logger log = LoggerFactory.getLogger(RemoveTask.class)

    /**
     * Main entry point to task.
     * @param config Config file.
     * @param namespace ArgParse arguments.
     */
    @Override
    void runTask(Config config, Namespace namespace) {
        def workingDir = config.installDir.toPath()

        def name = namespace.getString('name')

        log.info "Removing application: ${name}"

        def file = workingDir.resolve('data').resolve(name + '.json').toFile()

        def data = loadData(file, name)

        log.debug "Deleting lib dir: ${data.installDir}"
        data.installDir.deleteDir()
        data.scripts.each {
            log.debug "Deleting script: ${it}"
            it.delete()
        }

        log.debug "Deleting config: ${file}"
        file.delete()

    }

    /**
     * Reads the given config file.
     * @param file The file to load.
     * @param name The name of the package, used purely for the exception.
     * @return The loaded config.
     * @throws VineException if the file does not exist.
     */
    static InstalledData loadData(File file, String name) {
        log.debug "Loading config file: ${file}"

        if(file.exists()) {
            return new ObjectMapper().readValue(file, InstalledData.class)
        } else {
            throw new VineException("Can't remove ${name} as it doesn't exist!")
        }

    }
}
