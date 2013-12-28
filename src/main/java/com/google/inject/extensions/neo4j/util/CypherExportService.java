package com.google.inject.extensions.neo4j.util;

import ch.lambdaj.function.convert.Converter;
import com.google.gson.Gson;
import com.google.inject.Inject;
import org.junit.Assert;
import org.neo4j.graphdb.*;

import java.util.*;

import static ch.lambdaj.Lambda.convert;
import static ch.lambdaj.Lambda.join;
import static org.neo4j.tooling.GlobalGraphOperations.at;

public class CypherExportService {

    @Inject private static GraphDatabaseService gdb;
    @Inject private static DotExportService dotExportService;

    public static void exportToFile() {
        try (Transaction ignored = gdb.beginTx()) {
            dotExportService.toFile();
        }
    }

    public static void assertGraphEquals(String expected) {
        Assert.assertEquals(expected, export());
    }

    public static String export() {
        try (Transaction ignored = gdb.beginTx()) {
            Map<Node, Long> nodeIds = nodeIds();

            StringBuilder sb = new StringBuilder();
            sb.append("create \n");
            sb.append(appendNodes(nodeIds));

            String rels = appendRelationships(nodeIds);
            if (!rels.isEmpty()) {
                sb.append(",\n").append(rels);
            }

            return sb.toString();
        }
    }

    private static Map<Node, Long> nodeIds() {
        long count = 1;
        Map<Node, Long> nodeIds = new LinkedHashMap<>();
        for (Node node : at(gdb).getAllNodes()) {
            nodeIds.put(node, count);
            count++;
        }
        return nodeIds;
    }

    private static String appendNodes(final Map<Node, Long> nodeIds) {
        return join(convert(nodeIds.keySet(), new Converter<Node, String>() {
            @Override public String convert(Node node) {
                return appendNode(node, nodeIds.get(node));
            }
        }), ",\n");
    }

    private static String appendNode(Node node, long id) {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        formatNode(sb, id);
        sb.append(" ");
        formatProperties(sb, node);
        sb.append(")");
        return sb.toString();
    }

    private static String appendRelationships(Map<Node, Long> nodeIds) {
        StringBuilder sb = new StringBuilder();
        for (Node node : nodeIds.keySet()) {
            List<String> rels = new ArrayList<>();
            for (Relationship rel : node.getRelationships(Direction.OUTGOING)) {
                rels.add(appendRelationship(rel, nodeIds.get(rel.getStartNode()), nodeIds.get(rel.getEndNode())));
            }
            if (rels.isEmpty()) continue;

            Collections.sort(rels);
            if (sb.length() > 0) sb.append(",\n");
            sb.append(join(rels, ",\n"));
        }
        return sb.toString();
    }

    private static String appendRelationship(Relationship rel, long startNodeId, long endNodeId) {
        StringBuilder sb = new StringBuilder();
        formatNode(sb, startNodeId);
        sb.append("-[:").append(rel.getType().name());
        formatProperties(sb, rel);
        sb.append("]->");
        formatNode(sb, endNodeId);
        return sb.toString();
    }

    private static void formatProperties(StringBuilder sb, PropertyContainer pc) {
        Map<String, Object> properties = new TreeMap<>();
        for (String prop : pc.getPropertyKeys()) {
            properties.put(prop, pc.getProperty(prop));
        }
        if (properties.isEmpty()) return;
        sb.append(" ");
        final String jsonString = new Gson().toJson(properties);
        sb.append(removeNameQuotes(jsonString));
    }

    private static String removeNameQuotes(String json) {
        return json.replaceAll("\"([^\"]+)\":", "$1:");
    }

    private static void formatNode(StringBuilder sb, long id) {
        sb.append("_").append(id);
    }
}
