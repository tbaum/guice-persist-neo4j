package com.google.inject.extensions.neo4j;

import ch.lambdaj.function.convert.Converter;

public class ResultMapConverter<T> implements Converter<ConvertingCypherIterable.ResultMap, T> {

    private final String key;

    public static <T> ResultMapConverter<T> key(String key) {
        return new ResultMapConverter<>(key);
    }

    private ResultMapConverter(String key) {
        this.key = key;
    }

    @Override public T convert(ConvertingCypherIterable.ResultMap resultMap) {
        return resultMap.get(key);
    }
}
