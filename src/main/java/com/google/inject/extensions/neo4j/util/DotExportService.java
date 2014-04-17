package com.google.inject.extensions.neo4j.util;

import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.google.inject.extensions.neo4j.GuicedExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.graphdb.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

import static org.neo4j.graphdb.Direction.OUTGOING;
import static org.neo4j.helpers.collection.IteratorUtil.asCollection;
import static org.neo4j.helpers.collection.IteratorUtil.asIterable;

public class DotExportService {

    public static final Set<ClusterStrategy<Node>> clusterStrategies = new HashSet<>();
    public static final Set<FormatStrategy<Node>> highlightStrategies = new HashSet<>();
    public static final Set<FormatStrategy<Relationship>> reverseLink = new HashSet<>();
    @Inject private static GuicedExecutionEngine cypher;
    @Inject private static GraphDatabaseService gds;

    public static String export(Iterator<Node> allNodes) {
        Collection<Node> node = asCollection(asIterable(allNodes));
        StringBuilder sb = new StringBuilder();
        sb.append("digraph {\n");
        appendNodes(sb, node);
        appendRelationships(sb, node);
        sb.append("}");
        return sb.toString();
    }

    private static void appendNodes(StringBuilder sb, Iterable<Node> nodes) {
        Map<String, StringBuilder> cluster = new HashMap<>();

        for (final Node node : nodes) {

            Optional<ClusterStrategy<Node>> strategy = clusterStrategies.stream().filter(x -> x.matches(node)).findFirst();
            if (strategy.isPresent()) {
                String type = strategy.get().clusterName(node);
                if (!cluster.containsKey(type)) {
                    cluster.put(type, new StringBuilder());
                }
                appNode(node, cluster.get(type));
            } else
                appNode(node, sb);
        }
        cluster.keySet().forEach(sg ->
                sb.append("subgraph cluster_").append(sg).append(" { \n").append(cluster.get(sg)).append("}\n"));
    }

    private static void appNode(Node node, StringBuilder stringBuilder) {
        Optional<FormatStrategy<Node>> strategy = highlightStrategies.stream().filter(s -> s.matches(node)).findFirst();

        stringBuilder.append("_").append(node.getId())
                .append(" ").append(formatProperties(node, false, strategy.isPresent())).append(";\n");
    }

    private static void appendRelationships(StringBuilder sb, Collection<Node> nodes) {
        Set<Relationship> rels = new HashSet<>();

        for (Node node : nodes) {
            for (Relationship rel : node.getRelationships(OUTGOING)) {
                if (nodes.contains(rel.getEndNode())) {
                    rels.add(rel);
                }
            }
        }

        for (Relationship rel : rels) {
            Node startNode = rel.getStartNode();
            Node endNode = rel.getEndNode();
            appendRelationship(sb, rel, startNode.getId(), endNode.getId());
        }
    }

    private static void appendRelationship(StringBuilder sb, Relationship rel, long startNodeId, long endNodeId) {
        final boolean isReverse = reverseLink.stream().filter(r -> r.matches(rel)).findFirst().isPresent();

        if (isReverse) {
            sb.append("_").append(endNodeId).append(" -> _").append(startNodeId);
        } else {
            sb.append("_").append(startNodeId).append(" -> _").append(endNodeId);
        }
        sb.append(formatProperties(rel, isReverse, false, rel.getType().name())).append(";\n");
    }

    private static String id(PropertyContainer pc) {
        if (pc instanceof Node) {
            Node node = (Node) pc;
            return String.valueOf(node.getId());
        }

        if (pc instanceof Relationship) {
            Relationship relationship = (Relationship) pc;
            return String.valueOf(relationship.getId());
        }
        return "";
    }

    public static String export(String query) {
        try (Transaction tx = gds.beginTx()) {
            final String result = export(cypher.execute(query).<Node>columnAs("n"));
            tx.success();
            return result;
        }
    }

    public static void toFile() {
        Throwable t = new Throwable();
        t.fillInStackTrace();

        StackTraceElement e = t.getStackTrace()[1];
        String className = e.getClassName();
        String methodName = e.getMethodName();
        File f = new File("target", className + "_" + methodName + "_" + e.getLineNumber() + ".gv");

        toFile(f, "START n=node(*) return n");
    }

    private static String formatProperties(PropertyContainer pc, boolean reverse, boolean mark, String... lines) {
        if (!pc.getPropertyKeys().iterator().hasNext() && lines.length == 0) return "";
        return "[label=\" [" + id(pc) + "] " + (pc instanceof Node ? formatLabels((Node) pc) : "") + "\n" +
                formatProperties(pc) + (lines.length > 0 ? String.join("\n", lines) : "") + "\"" +
                (mark ? ",style=\"filled\"" : "") +
                (reverse ? ",dir=back" : "") +
                "]";
    }

    private static String formatProperties(PropertyContainer pc) {
        final Map<String, Object> properties = toMap(pc);
        return properties.isEmpty() ? "" : " " + removeNameQuotes(new GsonBuilder().setPrettyPrinting().create().toJson(properties));
    }

    private static String formatLabels(Node pc) {
        StringBuilder builder = new StringBuilder();
        pc.getLabels().forEach(label -> builder.append(":").append(label.name()));
        return builder.toString();
    }

    private static Map<String, Object> toMap(PropertyContainer pc) {
        final Map<String, Object> result = new TreeMap<>();
        for (String prop : pc.getPropertyKeys()) {
            result.put(prop, pc.getProperty(prop));
        }
        return result;
    }

    private static String removeNameQuotes(String json) {
        json = json.trim();
        if (json.startsWith("{\n") && json.endsWith("}")) json = json.substring(2, json.length() - 1);

        return json
                .replaceAll("\"([^\"]+)\":", "$1:")
                .replaceAll("\"", "&quot;");
    }

    public static void toFile(File f, String query) {
        try {
            //noinspection ResultOfMethodCallIgnored
            f.getParentFile().mkdirs();
            Writer fo = new FileWriter(f);
            fo.write(export(query));
            fo.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public interface FormatStrategy<T> {
        boolean matches(T node);
    }

    public interface ClusterStrategy<T> extends FormatStrategy<T> {
        String clusterName(Node node);
    }
}
