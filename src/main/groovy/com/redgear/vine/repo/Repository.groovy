package com.redgear.vine.repo

import com.redgear.vine.config.Coords

/**
 * Created by LordBlackHole on 7/4/2016.
 */
interface Repository {

    Package resolvePackage(Coords coords)

    interface Package {

        File getMain()

        List<File> getDependencies()

    }

}
