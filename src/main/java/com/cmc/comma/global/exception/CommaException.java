package com.cmc.comma.global.exception;

import lombok.Getter;

@Getter
public class CommaException extends RuntimeException {

    private final ErrorCode errorCode;

    public CommaException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}