package com.buffsovernexus.basketball.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ScenarioNameAlreadyExistsException extends RuntimeException {
    public ScenarioNameAlreadyExistsException(String name) {
        super("A scenario with the name '" + name + "' already exists for this account");
    }
}

