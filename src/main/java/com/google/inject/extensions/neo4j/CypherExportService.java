package com.google.inject.extensions.neo4j;

import ch.lambdaj.function.convert.Converter;
import com.google.gson.Gson;
import org.neo4j.graphdb.*;

import javax.inject.Inject;
import java.util.*;

import static ch.lambdaj.Lambda.convert;
import static ch.lambdaj.Lambda.join;
import static java.util.Collections.sort;
import static org.neo4j.graphdb.Direction.OUTGOING;
import static org.neo4j.tooling.GlobalGraphOperations.at;

public class CypherExportService {
    private final GraphDatabaseService gdb;

    @Inject public CypherExportService(GraphDatabaseService gdb) {
        this.gdb = gdb;
    }

    public String export() {
        Map<Node, Long> nodes = allNodes();

        Collection<String> statements = new ArrayList<>();
        statements.addAll(appendNodes(nodes));
        statements.addAll(appendRelationships(nodes));

        return "create \n" + join(statements, ",\n");
    }

    private Map<Node, Long> allNodes() {
        Map<Node, Long> sort = new LinkedHashMap<>();
        long nodeCounter = 0;
        for (Node node : at(gdb).getAllNodes()) {
            sort.put(node, nodeCounter++);
        }
        return sort;
    }

    private Collection<String> appendNodes(Map<Node, Long> nodes) {
        return convert(nodes.entrySet(), new Converter<Map.Entry<Node, Long>, String>() {
            @Override public String convert(Map.Entry<Node, Long> entry) {
                return "(" + formatNode(entry.getValue()) + " " + formatProperties(entry.getKey()) + ")";
            }
        });
    }

    private Collection<String> appendRelationships(final Map<Node, Long> nodes) {
        Collection<String> result = new ArrayList<>();
        for (Node node : nodes.keySet()) {
            result.addAll(formatRelationships(nodes, node.getRelationships(OUTGOING)));
        }
        return result;
    }

    private List<String> formatRelationships(final Map<Node, Long> nodeIdMap, Iterable<Relationship> relationships) {
        List<String> result = convert(relationships,
                new Converter<Relationship, String>() {
                    @Override public String convert(Relationship rel) {
                        return formatNode(nodeIdMap.get(rel.getStartNode())) +
                                formatRelationship(rel) +
                                formatNode(nodeIdMap.get(rel.getEndNode()));

                    }
                });

        sort(result);
        return result;
    }

    private String formatRelationship(Relationship rel) {
        return "-[:" + rel.getType().name() + formatProperties(rel) + "]->";
    }

    private String formatProperties(PropertyContainer pc) {
        final Map<String, Object> properties = toMap(pc);
        return properties.isEmpty() ? "" : " " + removeNameQuotes(new Gson().toJson(properties));
    }

    private Map<String, Object> toMap(PropertyContainer pc) {
        final Map<String, Object> result = new TreeMap<>();
        for (String prop : pc.getPropertyKeys()) {
            result.put(prop, pc.getProperty(prop));
        }
        return result;
    }

    private String removeNameQuotes(String json) {
        return json.replaceAll("\"([^\"]+)\":", "$1:");
    }

    private String formatNode(long id) {
        return "_" + id;
    }
}