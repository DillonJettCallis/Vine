package com.redgear.vine.task

import com.fasterxml.jackson.databind.ObjectMapper
import com.redgear.vine.config.Config
import com.redgear.vine.config.InstalledData
import com.redgear.vine.config.Options

/**
 * Created by LordBlackHole on 2016-11-12.
 */
class InfoTask implements Task {

    @Override
    void runTask(Config config, Options options) {

        def file = config.installDir.toPath().resolve('data').resolve(options.name + '.json').toFile()

        def data = new ObjectMapper().readValue(file, InstalledData.class)


        println data
    }

}
