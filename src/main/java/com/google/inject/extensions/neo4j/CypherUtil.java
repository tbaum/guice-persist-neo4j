package com.google.inject.extensions.neo4j;

import ch.lambdaj.Lambda;
import ch.lambdaj.function.convert.Converter;
import org.neo4j.cypher.javacompat.ExecutionEngine;

import java.io.Serializable;
import java.util.Map;

import static com.google.inject.extensions.neo4j.ConvertingCypherIterable.ResultMapIterator;

public abstract class CypherUtil {

    public static <T extends Serializable> ListResult<T> asListPresentation(ExecutionEngine cypher,
                                                                            String whereQuery, int start, int limit,
                                                                            Converter<ConvertingCypherIterable.ResultMap, T> converter,
                                                                            String countColumn,
                                                                            String returnColumns,
                                                                            Map<String, Object> parameter) {
        String countQuery = whereQuery + " RETURN count(" + countColumn + ") as count";
        String listQuery = whereQuery + " " + returnColumns + " skip " + start + " limit " + limit;
        return convertToListResult(cypher, converter, parameter, listQuery, countQuery);

    }

    private static <T extends Serializable> ListResult<T> convertToListResult(ExecutionEngine cypher, Converter<ConvertingCypherIterable.ResultMap, T> converter, Map<String, Object> parameter, String listQuery, String countQueryString) {
        ResultMapIterator listIterator = new ResultMapIterator(cypher.execute(listQuery, parameter));
        ResultMapIterator countIterator = new ResultMapIterator(cypher.execute(countQueryString, parameter));

        Number count = 0;
        if (countIterator.hasNext()) {
            count = countIterator.next().get("count");
        }

        return new ListResult<>(Lambda.convert(listIterator, converter), count.intValue());
    }

}
