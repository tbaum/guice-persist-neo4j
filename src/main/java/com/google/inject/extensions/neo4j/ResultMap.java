package com.google.inject.extensions.neo4j;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.emptyMap;

/**
 * @author tbaum
 * @since 17.04.2014
 */
public class ResultMap {
    private final Map<String, Object> map;

    public ResultMap(final Map<String, Object> next) {
        this.map = new HashMap<>(next);
    }

    public static ResultMap empty() {
        return new ResultMap(emptyMap());
    }

    @SuppressWarnings("unchecked") public <T> T get(String o) {
        return (T) map.get(o);
    }

    public boolean contains(String o) {
        return map.get(o) != null;
    }

    @Override public String toString() {
        return map.toString();
    }
}
