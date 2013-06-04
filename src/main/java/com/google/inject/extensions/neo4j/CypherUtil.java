package com.google.inject.extensions.neo4j;

import ch.lambdaj.Lambda;
import ch.lambdaj.function.convert.Converter;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypherdsl.Identifier;
import org.neo4j.cypherdsl.expression.Expression;
import org.neo4j.cypherdsl.grammar.OrderBy;
import org.neo4j.cypherdsl.grammar.ReturnNext;
import org.neo4j.cypherdsl.grammar.Where;
import org.neo4j.cypherdsl.query.OrderByExpression;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import static com.google.inject.extensions.neo4j.ConvertingCypherIterable.ResultMapIterator;
import static org.neo4j.cypherdsl.CypherQuery.*;

public abstract class CypherUtil {

    public static final Identifier _COUNT = identifier("count");

    public static <T extends Serializable> ListResult<T> asListPresentation(ExecutionEngine cypher,
                                                                            String whereQuery, int start, int limit,
                                                                            Converter<ConvertingCypherIterable.ResultMap, T> converter,
                                                                            String countColumn,
                                                                            String returnColumns,
                                                                            Map<String, Object> parameter) {
        String countQuery = whereQuery + " RETURN count(" + countColumn + ") as " + _COUNT.toString();
        String listQuery = whereQuery + " " + returnColumns + " skip " + start + " limit " + limit;
        return convertToListResult(cypher, converter, parameter, listQuery, countQuery);

    }

    public static <T extends Serializable> ListResult<T> asListPresentation(ExecutionEngine cypher,
                                                                            Where where1, int start, int limit,
                                                                            Converter<ConvertingCypherIterable.ResultMap, T> converter,
                                                                            Expression countColumn,
                                                                            List<? extends Expression> returnColumns,
                                                                            List<OrderByExpression> sortColumns,
                                                                            Map<String, Object> parameter) {
        Expression[] returnExpression = returnColumns.toArray(new Expression[returnColumns.size()]);
        final ReturnNext returns = continueQuery(where1.toQuery(), Where.class).returns(returnExpression);

        OrderBy orderBy;
        if (sortColumns == null || sortColumns.isEmpty()) {
            orderBy = returns;
        } else {
            OrderByExpression[] sortExpression = sortColumns.toArray(new OrderByExpression[sortColumns.size()]);
            orderBy = returns.orderBy(sortExpression);
        }

        return convertToListResult(cypher, converter, parameter,
                orderBy.skip(start).limit(limit).toString(),
                continueQuery(where1.toQuery(), Where.class).returns(as(count(countColumn), _COUNT)).toString());
    }

    private static <T extends Serializable> ListResult<T> convertToListResult(ExecutionEngine cypher, Converter<ConvertingCypherIterable.ResultMap, T> converter, Map<String, Object> parameter, String listQuery, String countQueryString) {
        ResultMapIterator listIterator = new ResultMapIterator(cypher.execute(listQuery, parameter));
        ResultMapIterator countIterator = new ResultMapIterator(cypher.execute(countQueryString, parameter));

        Number count = 0;
        if (countIterator.hasNext()) {
            count = countIterator.next().get(_COUNT);
        }

        return new ListResult<T>(Lambda.convert(listIterator, converter), count.intValue());
    }

}
