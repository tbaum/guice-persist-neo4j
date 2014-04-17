package com.google.inject.extensions.neo4j.util;

import com.google.gson.Gson;
import com.google.inject.Inject;
import org.junit.Assert;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.schema.ConstraintDefinition;
import org.neo4j.graphdb.schema.IndexDefinition;

import java.util.*;

import static java.lang.String.join;
import static org.neo4j.helpers.collection.IteratorUtil.asCollection;
import static org.neo4j.tooling.GlobalGraphOperations.at;

public class CypherExportService {

    @Inject private static GraphDatabaseService gdb;

    public static void exportToFile() {
        DotExportService.toFile();
    }

    public static void assertGraphEquals(String expected) {
        Assert.assertEquals(expected, new Exporter(gdb).export());
    }

    public static void assertGraphConstraintsEquals(String expected) {
        Assert.assertEquals(expected, new Exporter(gdb).exportConstraints());
    }

    public static String export() {
        return new Exporter(gdb).export();
    }

    public static class Exporter {
        private final GraphDatabaseService gdb;
        private final IdResolver idResolver = new IdResolver();

        public Exporter(GraphDatabaseService gdb) {
            this.gdb = gdb;
        }

        public String export() {
            try (Transaction tx = gdb.beginTx()) {
                Collection<Node> nodeIds = asCollection(at(gdb).getAllNodes());

                StringBuilder sb = new StringBuilder("create \n");
                sb.append(appendNodes(nodeIds));

                String rels = appendRelationships(nodeIds);
                if (!rels.isEmpty()) {
                    sb.append(",\n").append(rels);
                }
                tx.success();
                return sb.toString();
            }
        }

        public String exportConstraints() {
            try (Transaction tx = gdb.beginTx()) {

                StringBuilder sb = new StringBuilder();
                appendConstrains(sb);
                appendIndexes(sb);
                tx.success();
                return sb.toString();
            }
        }

        private void appendConstrains(StringBuilder sb) {
            final List<String> result = new ArrayList<>();
            for (ConstraintDefinition constraint : gdb.schema().getConstraints()) {
                switch (constraint.getConstraintType()) {

                    case UNIQUENESS:
                        final Iterator<String> propertyKeys = constraint.getPropertyKeys().iterator();
                        if (!propertyKeys.hasNext()) {
                            throw new RuntimeException("unable to handle constraint " + constraint.getConstraintType());
                        }
                        result.add("CREATE CONSTRAINT ON (c:" + constraint.getLabel() + ") ASSERT c." + propertyKeys.next() + " IS UNIQUE;");
                        if (propertyKeys.hasNext()) {
                            throw new RuntimeException("unable to handle constraint " + constraint.getConstraintType());
                        }
                        break;
                    default:
                        throw new RuntimeException("unable to handle constraint " + constraint.getConstraintType());
                }

            }
            addSortedResult(sb, result);
        }

        private void appendIndexes(StringBuilder sb) {
            final List<String> result = new ArrayList<>();
            for (IndexDefinition index : gdb.schema().getIndexes()) {
                if (!index.isConstraintIndex()) {
                    result.add("CREATE INDEX ON :" + index.getLabel() + "(" + join(",", index.getPropertyKeys()) + ");");
                }
            }
            addSortedResult(sb, result);
        }

        private void addSortedResult(StringBuilder sb, List<String> result) {
            Collections.sort(result);
            for (String s : result) {
                sb.append(s).append("\n");
            }
        }

        private String appendNodes(final Collection<Node> allNodes) {
            return join(",\n", asCollection(allNodes.stream().map(this::appendNode).iterator()));
        }

        private String appendNode(Node node) {
            StringBuilder sb = new StringBuilder();
            sb.append("(");
            formatNode(sb, node);
            formatLabels(sb, node);
            sb.append(" ");
            formatProperties(sb, node);
            sb.append(")");
            return sb.toString();
        }

        private String appendRelationships(Collection<Node> allNodes) {
            StringBuilder sb = new StringBuilder();
            for (Node node : allNodes) {
                List<String> rels = new ArrayList<>();
                for (Relationship rel : node.getRelationships(Direction.OUTGOING)) {
                    rels.add(appendRelationship(rel));
                }
                if (rels.isEmpty()) continue;

                Collections.sort(rels);
                if (sb.length() > 0) sb.append(",\n");
                sb.append(join(",\n", rels));
            }
            return sb.toString();
        }

        private String appendRelationship(Relationship rel) {
            StringBuilder sb = new StringBuilder();
            sb.append('(');
            formatNode(sb, rel.getStartNode());
            sb.append(")-[:").append(rel.getType().name());
            formatProperties(sb, rel);
            sb.append("]->(");
            formatNode(sb, rel.getEndNode());
            sb.append(')');
            return sb.toString();
        }

        private void formatProperties(StringBuilder sb, PropertyContainer pc) {
            Map<String, Object> properties = new TreeMap<>();
            for (String prop : pc.getPropertyKeys()) {
                properties.put(prop, pc.getProperty(prop));
            }
            if (properties.isEmpty()) return;
            sb.append(" ");
            final String jsonString = new Gson().toJson(properties);
            sb.append(removeNameQuotes(jsonString));
        }

        private String removeNameQuotes(String json) {
            return json.replaceAll("\"([^\"]+)\":", "$1:");
        }

        private void formatNode(StringBuilder sb, Node node) {
            sb.append(idResolver.resolveId(node));
        }

        private void formatLabels(StringBuilder sb, Node n) {
            n.getLabels().forEach(label -> sb.append(":").append(label));
        }
    }

    private static class IdResolver {
        private final Map<String, Map<Node, Long>> nodeId = new HashMap<>();

        private String resolveId(Node node) {
            String label = firstLabel(node);

            Map<Node, Long> nodeLongMap = labelMap(label);
            Long id = labelIdFor(node, nodeLongMap);

            return formatName(label, id);
        }

        private Long labelIdFor(Node node, Map<Node, Long> nodeLongMap) {
            if (!nodeLongMap.containsKey(node)) {
                nodeLongMap.put(node, nodeLongMap.size() + 1L);
            }

            return nodeLongMap.get(node);
        }

        private Map<Node, Long> labelMap(String label) {
            if (nodeId.containsKey(label)) {
                return nodeId.get(label);
            }

            HashMap<Node, Long> value = new HashMap<>();
            nodeId.put(label, value);

            return value;
        }

        private String formatName(String label, Long aLong) {
            return label + "_" + aLong;
        }

        private String firstLabel(Node node) {
            final Iterator<Label> iterator = node.getLabels().iterator();
            return iterator.hasNext() ? iterator.next().name() : "_no_label_";
        }
    }
}
