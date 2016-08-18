package com.redgear.vine.config

/**
 * Created by ft4 on 8/18/2016.
 */
class InstalledData {

    String name

    String groupId

    String artifactId

    String version

    InstallType type

    File installDir

    List<File> scripts




}


enum InstallType {
    BIN,
    JAR
}
