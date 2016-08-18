package com.redgear.vine.repo

/**
 * Created by LordBlackHole on 7/4/2016.
 */
interface Repository {

    Package resolvePackage(String group, String artifact, String version)

    interface Package {

        File getMain()

        List<File> getDependencies()

    }

}
