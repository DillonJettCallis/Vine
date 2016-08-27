package com.redgear.vine.config

/**
 * Created by LordBlackHole on 8/26/2016.
 */
class EndorsedConfig {

    List<EndorsedPackage> packages

}


class EndorsedPackage {

    String name

    String groupId

    String artifactId

    String main

    String additionalArgs

}