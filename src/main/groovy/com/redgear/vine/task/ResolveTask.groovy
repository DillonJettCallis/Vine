package com.redgear.vine.task

import com.redgear.vine.config.Config
import com.redgear.vine.config.Options
import com.redgear.vine.exception.VineException
import com.redgear.vine.repo.Repository
import com.redgear.vine.repo.impl.AetherRepo
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Downloads the given arguments but does not install them.
 *
 * @author Dillon Jett Callis
 * @version 0.1.0
 * @since 2016-8-18
 *
 */
class ResolveTask implements Task {

    private static final Logger log = LoggerFactory.getLogger(ResolveTask.class)

    //TODO: Merge some of this functionality with Install.
    @Override
    void runTask(Config config, Options options) {
        Repository repo = new AetherRepo(config)
        List<String> args = options.args

        log.debug "Resolving ${args.size()} artifacts"

        def deps = args.collect {

            def split = it.split(":")

            if(split.length < 3 || split.length > 4) {
                throw new VineException("Invalid Maven Coords: $it")
            }

            def group = split[0]
            def artifact = split[1]
            def version = split[2]

            //So we can take the ones that have group:artifact:jar:version.
            if(split.length == 4) {
                artifact = split[2]
                version = split[3]
            }

            def pack = repo.resolvePackage(group, artifact, version)

            [pack.main] + pack.dependencies
        }.flatten().toSet()

        if(options.pretty) {
            deps.each {println it}
        } else {
            println deps.join(';')
        }

    }

}
