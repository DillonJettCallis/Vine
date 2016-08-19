package com.redgear.vine.task

import com.fasterxml.jackson.databind.ObjectMapper
import com.redgear.vine.config.Config
import com.redgear.vine.config.InstalledData
import net.sourceforge.argparse4j.inf.Namespace
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by LordBlackHole on 8/17/2016.
 */
class ListTask implements Task {

    private static final Logger log = LoggerFactory.getLogger(ListTask.class)

    @Override
    void runTask(Config config, Namespace namespace) {
        def verbose = namespace.getBoolean('verbose')
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

    static InstalledData loadData(File file) {
        return new ObjectMapper().readValue(file, InstalledData.class)
    }


}
