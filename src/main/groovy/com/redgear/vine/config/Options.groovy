package com.redgear.vine.config

import com.redgear.vine.task.Task

/**
 * Created by LordBlackHole on 2016-11-12.
 */
class Options {

    //All
    String config
    Task task

    //Mostly for install (name is used for remove and rename too.
    String name
    String coords
    String additional
    String main


    //Misc
    boolean verbose
    boolean pretty
    boolean quiet
    boolean debug


    //For clean
    boolean locals
    boolean snapshots
    boolean jarMissing
    boolean pomMissing
    boolean empty = true

    //For rename
    String newName
    
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
