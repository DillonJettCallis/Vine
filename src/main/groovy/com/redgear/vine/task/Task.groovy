package com.redgear.vine.task

import com.redgear.vine.config.Config
import com.redgear.vine.config.Options
import net.sourceforge.argparse4j.inf.Namespace

/**
 * A Task represents a single Command from the cli.
 * ie: 'vine install' should map to a task called InstallTask, and 'vine foo' should be backed by FooTask.
 *
 * @author Dillon Jett Callis
 * @version 0.1.0
 * @since 2016-8-16
 */
interface Task {

    /**
     * Runs this task, as it was chosen by the user from the command line.
     *
     * @param config The config file.
     * @param namespace An ArgParse class holding the parsed command line options.
     */
    void runTask(Config config, Options options)


}