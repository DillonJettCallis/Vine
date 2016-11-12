package com.redgear.vine.config

import com.redgear.vine.task.Task

/**
 * Created by LordBlackHole on 2016-11-12.
 */
class Options {

    String config
    Task task

    String name
    String coords
    String additional
    String main
    
    boolean verbose
    boolean pretty
    boolean quiet
    boolean debug
    
    boolean locals
    boolean snapshots
    boolean jarMissing
    boolean pomMissing
    boolean empty = true
    
    List<String> args = []


    @Override
    public String toString() {
        return "{\"className\": \"" + Options.class + "\"" +
                ",\"locals\": \"" + locals + "\"" +
                ",\"snapshots\": \"" + snapshots + "\"" +
                ",\"jarMissing\": \"" + jarMissing + "\"" +
                ",\"pomMissing\": \"" + pomMissing + "\"" +
                ",\"empty\": \"" + empty + "\"" +
                '}';
    }
}
