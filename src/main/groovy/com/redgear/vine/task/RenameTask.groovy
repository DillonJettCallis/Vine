package com.redgear.vine.task

import com.fasterxml.jackson.databind.ObjectMapper
import com.redgear.vine.config.Config
import com.redgear.vine.config.InstallType
import com.redgear.vine.config.InstalledData
import com.redgear.vine.config.Options
import com.redgear.vine.exception.VineException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.file.Path

/**
 * Created by LordBlackHole on 2016-11-12.
 */
class RenameTask implements Task {

    private static final Logger log = LoggerFactory.getLogger(RenameTask.class)

    @Override
    void runTask(Config config, Options options) {
        def workingDir = config.installDir.toPath()

        def mapper = new ObjectMapper()


        def oldConfig = workingDir.resolve('data').resolve(options.name + '.json').toFile()

        if(!oldConfig.exists()) {
            throw new VineException("No such application: $options.name")
        }

        def data = mapper.readValue(oldConfig, InstalledData.class)

        def newFile = new File(data.installDir.parentFile, options.newName)

        data.name = options.newName

        data.installDir.renameTo(newFile)

        data.installDir = newFile

        if(data.type == InstallType.JAR) {
            data.scripts = data.scripts.collect {

                if(it.name == options.name) {
                    def newName = new File(it.parentFile.absolutePath + '/' + options.newName)
                    it.renameTo(newName)
                    newName
                } else if(it.name == options.name + '.bat') {
                    def newName = new File(it.parentFile.absolutePath + '/' + options.newName + '.bat')
                    it.renameTo(newName)
                    newName
                } else {
                    return it
                }
            }
        }

        mapper.writeValue(workingDir.resolve('data').resolve(options.newName + '.json').toFile(), data)

        oldConfig.delete()
    }

    private static boolean rename(Path path, String newName) {

        if(path.toFile().exists()) {
            log.info "Renaming file: ${path}"



            return path.renameTo(path.parent.toString() + '/' + newName)
        }

        return false
    }

}
