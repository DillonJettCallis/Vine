package com.redgear.vine.task

import com.fasterxml.jackson.databind.ObjectMapper
import com.redgear.vine.config.Config
import com.redgear.vine.config.InstalledData
import com.redgear.vine.config.Options
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Task to print out a list of all installed packages.
 *
 * @author Dillon Jett Callis
 * @version 0.1.0
 * @since 2016-8-17
 */
class ListTask implements Task {

    private static final Logger log = LoggerFactory.getLogger(ListTask.class)

    /**
     * Main entry point to this Task.
     * @param config Config file.
     * @param options ArgParse arguments.
     */
    @Override
    void runTask(Config config, Options options) {
        def verbose = options.verbose
        def files = config.installDir.toPath().resolve('data').toFile().listFiles()

        if(!files) {
            println 'No applications installed.'
            return
        }

        files.each {

            def data = loadData(it)

            if(verbose) {
                println "${data.name}: GroupId: ${data.groupId}, ArtifactId: ${data.artifactId}, Version: ${data.version}, Type: ${data.type}, Installed Dir: ${data.installDir}"
            } else
                println "${data.name}: ${data.version}"

        }

    }

    /**
     * Loads the given Data file.
     * @param file InstalledData config file to load.
     * @return The loaded InstalledData
     */
    static InstalledData loadData(File file) {
        return new ObjectMapper().readValue(file, InstalledData.class)
    }


}
