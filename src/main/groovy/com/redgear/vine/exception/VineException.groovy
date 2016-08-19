package com.redgear.vine.exception

/**
 * Created by LordBlackHole on 8/18/2016.
 */
class VineException extends RuntimeException {

    VineException(String message) {
        super(message)
    }

    VineException(String message, Throwable cause) {
        super(message, cause)
    }

}
