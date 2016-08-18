package com.redgear.vine.task

import com.fasterxml.jackson.databind.ObjectMapper
import com.redgear.vine.config.Config
import com.redgear.vine.config.InstalledData
import net.sourceforge.argparse4j.inf.Namespace

/**
 * Created by LordBlackHole on 8/16/2016.
 */
class RemoveTask implements Task {

    @Override
    void runTask(Config config, Namespace namespace) {
        def workingDir = config.installDir.toPath()

        namespace.getList('args').each { String name ->

            def file = workingDir.resolve('data').resolve(name + '.json').toFile()

            def data = loadData(file, name)

            data.installDir.deleteDir()
            data.scripts.each {
                it.delete()
            }

            file.delete()
        }
    }


    static InstalledData loadData(File file, String name) {
        if(file.exists()) {
            return new ObjectMapper().readValue(file, InstalledData.class)
        } else {
            throw new RuntimeException("Can't remove ${name} as it doesn't exist!")
        }

    }
}
