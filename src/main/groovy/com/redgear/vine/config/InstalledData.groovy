package com.redgear.vine.config
/**
 * Created by ft4 on 8/18/2016.
 */
class InstalledData {

    String name

    String groupId

    String artifactId

    String version

    String main

    InstallType type

    File installDir

    List<File> scripts


    @Override
    public String toString() {
        return """
Name:               $name
GroupId:            $groupId
ArtifactId:         $artifactId
Version:            $version
Main:               $main
Type:               $type
Install Directory:  $installDir
Scripts:
${scripts.join('\n')}
"""
    }


}


enum InstallType {
    BIN,
    JAR
}
