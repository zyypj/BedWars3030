package com.tomkeuper.bedwars.api.exceptions;


import com.tomkeuper.bedwars.api.server.VersionSupport;

public class InvalidEffectException extends Throwable {

    public InvalidEffectException(String message) {
        super(message + " não é um(a) " + VersionSupport.getName() + " válido! Usando os padrões..");
    }
}
