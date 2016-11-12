package com.redgear.vine.task

import com.redgear.vine.config.Config
import com.redgear.vine.config.Options
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 *
 * Main class and entry point to the application.
 *
 * @author Dillon Jett Callis
 * @version 0.1.0
 * @since 2016-8-28
 */
class CleanCacheTask implements Task {

    private static final Logger log = LoggerFactory.getLogger(CleanCacheTask.class)


    @Override
    void runTask(Config config, Options ops) {
        def repo = config.localCache

        log.info "Options: {}", ops

        recurse(repo, ops)
    }

    void recurse(File file, Options ops) {
//        log.info "File: {}", file

        if(file.isDirectory()) {

            def children = file.listFiles()

            if(children) {
                log.info "Has children: {}", file

                if(!children.toList().any{it.isDirectory()}) {

                    if(ops.pomMissing) {
                        def pom = children.toList().find { it.name.endsWith('.pom') }

                        if(pom == null) {
                            file.deleteDir()
                        }
                    }

                    if(ops.jarMissing) {
                        def jar = children.toList().find { it.name.endsWith('.jar') }

                        if(jar == null) {
                            file.deleteDir()
                        }
                    }

                    if (ops.snapshots) {
                        def pom = children.toList().find { it.name.endsWith('.pom') }

                        if (pom != null) {
                            def xml = new XmlSlurper().parse(pom)
                            def version = xml.version.text() ?: xml.parent.version.text() ?: ''

                            if (version.endsWith('-SNAPSHOT')) {
                                file.deleteDir()
                            }
                        }
                    }

                    if (ops.locals) {

                        def metadata = children.toList().find { it.name == 'maven-metadata-local.xml' }

                        if (metadata != null) {
                            def xml = new XmlSlurper().parse(metadata)

                            def local = xml.'**'.find{it.name() == 'localCopy'}?.text() ?: 'false'

                            if (local.toBoolean()) {
                                file.deleteDir()
                            }
                        }

                    }

                } else {
                    children.each { recurse(it, ops) }
                }
            }

            if(ops.empty && !file.listFiles()) {
                file.delete()
            }
        }
    }




}

