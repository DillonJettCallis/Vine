package com.redgear.vine.config

import java.nio.file.Paths

/**
 * Created by LordBlackHole on 8/17/2016.
 */
class Config {


    File installDir  = Paths.get(System.getProperty('user.home'), '/.vine').toFile()

    File localCache = Paths.get(System.getProperty('user.home'), '/.m2/repository').toFile()

    List<Repo> repos = [
            new Repo(type: RepoType.M2, name: 'central', uri: URI.create('https://repo1.maven.org/maven2/')),
            new Repo(type: RepoType.M2, name: 'jcenter', uri: URI.create('https://jcenter.bintray.com/')),
            new Repo(type: RepoType.M2, name: 'clojars', uri: URI.create('http://clojars.org/repo/'))
    ]

    List<URI> endorsedConfigs = [URI.create('https://raw.githubusercontent.com/RedGear/Vine/master/endorsed.json')]

}

class Repo {

    RepoType type

    String name

    URI uri

}

enum RepoType {
    M2,
    IVY
}