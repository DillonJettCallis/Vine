package com.redgear.vine.task

import com.fasterxml.jackson.databind.ObjectMapper
import com.redgear.vine.config.Config
import com.redgear.vine.config.InstalledData
import com.redgear.vine.exception.VineException
import net.sourceforge.argparse4j.inf.Namespace
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by LordBlackHole on 8/16/2016.
 */
class RemoveTask implements Task {

    private static final Logger log = LoggerFactory.getLogger(RemoveTask.class)

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


    static InstalledData loadData(File file, String name) {
        log.debug "Loading config file: ${file}"

        if(file.exists()) {
            return new ObjectMapper().readValue(file, InstalledData.class)
        } else {
            throw new VineException("Can't remove ${name} as it doesn't exist!")
        }

    }
}
