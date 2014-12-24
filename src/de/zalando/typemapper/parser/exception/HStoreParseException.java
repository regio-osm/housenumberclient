package de.zalando.typemapper.parser.exception;

/*
 * Source: https://github.com/zalando/java-sproc-wrapper
 * License Link: https://github.com/zalando/java-sproc-wrapper/blob/master/LICENSE
 * License: 'Licensed under the Apache License, Version 2.0 (the "License")'
 */

import java.text.ParseException;

public class HStoreParseException extends ParseException {

    private static final long serialVersionUID = 1734348462350943810L;

    public HStoreParseException(final String s, final int errorOffset) {
        super(s, errorOffset);
    }

}
