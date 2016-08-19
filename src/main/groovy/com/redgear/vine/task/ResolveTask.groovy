package com.redgear.vine.task

import com.redgear.vine.config.Config
import com.redgear.vine.exception.VineException
import com.redgear.vine.repo.Repository
import com.redgear.vine.repo.impl.AetherRepo
import net.sourceforge.argparse4j.inf.Namespace
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by ft4 on 8/18/2016.
 */
class ResolveTask implements Task {

    private static final Logger log = LoggerFactory.getLogger(ResolveTask.class)

    @Override
    void runTask(Config config, Namespace namespace) {
        Repository repo = new AetherRepo(config)
        List<String> args = namespace.getList('args')

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

        if(namespace.getBoolean('pretty')) {
            deps.each {println it}
        } else {
            println deps.join(';')
        }

    }

}
