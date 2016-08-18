package com.redgear.vine.task

import com.redgear.vine.config.Config
import net.sourceforge.argparse4j.inf.Namespace

/**
 * Created by LordBlackHole on 8/16/2016.
 */
interface Task {


    void runTask(Config config, Namespace namespace)



}