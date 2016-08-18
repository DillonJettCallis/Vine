package com.redgear.vine.task

import net.sourceforge.argparse4j.inf.Namespace

import java.nio.file.Path

/**
 * Created by LordBlackHole on 8/16/2016.
 */
class RemoveTask implements Task {

    @Override
    void runTask(Path workingDir, Namespace namespace) {
        namespace.getList('args').each { name ->
            workingDir.resolve("lib/$name").deleteDir()
            workingDir.resolve("bin/${name}.bat").toFile().delete()
            workingDir.resolve("bin/${name}").toFile().delete()
        }
    }
}
