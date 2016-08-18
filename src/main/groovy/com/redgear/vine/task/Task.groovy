package com.redgear.vine.task

import net.sourceforge.argparse4j.inf.Namespace

import java.nio.file.Path

/**
 * Created by LordBlackHole on 8/16/2016.
 */
interface Task {


    void runTask(Path workingDir, Namespace namespace)



}