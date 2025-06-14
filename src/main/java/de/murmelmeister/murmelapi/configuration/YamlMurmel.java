package de.murmelmeister.murmelapi.configuration;

import de.murmelmeister.murmelapi.exceptions.YamlMurmelException;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.comments.CommentLine;
import org.yaml.snakeyaml.comments.CommentType;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.representer.Representer;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class YamlMurmel {
    private final Path path;
    private final Yaml yaml;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public YamlMurmel(Builder builder) {
        this.path = Path.of(builder.fileName);
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumperOptions.setIndent(builder.indent);
        dumperOptions.setPrettyFlow(builder.prettyFlow);
        dumperOptions.setWidth(builder.width);
        dumperOptions.setProcessComments(builder.processComments);
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setAllowDuplicateKeys(builder.allowDuplicateKeys);
        loaderOptions.setMaxAliasesForCollections(builder.maxAliasesForCollections);
        loaderOptions.setWrappedToRootException(builder.wrappedToRootException);
        loaderOptions.setProcessComments(builder.processComments);

        SafeConstructor safeConstructor = new SafeConstructor(loaderOptions);
        Representer representer = new Representer(dumperOptions);
        this.yaml = new Yaml(safeConstructor, representer, dumperOptions, loaderOptions);
    }

    public <T> T getValue(String key, Class<T> type) {
        lock.readLock().lock();
        try {
            if (Files.notExists(path))
                return null;

            try (InputStream inputStream = Files.newInputStream(path)) {
                Object data = yaml.load(inputStream);

                if (data instanceof Map<?, ?> map) {
                    // Split the key into parts and navigate to the correct location in the YAML
                    String[] keys = key.split("\\.");
                    Object current = map;

                    for (String k : keys) {
                        if (!(current instanceof Map<?, ?> currentMap))
                            return null; // Key not found
                        current = currentMap.get(k);
                    }

                    if (current == null)
                        return null; // Key does not exist

                    if (!type.isInstance(current))
                        throw new ClassCastException("Object " + key + " is not an instance of " + type.getName());

                    return type.cast(current);
                }

                return null; // Not a map
            } catch (IOException e) {
                throw new YamlMurmelException("Error reading file: " + path, e);
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    public <T> T getValue(String key, Class<T> type, T fallback) {
        T value = getValue(key, type);
        if (value != null)
            return value;
        // If the value is null, set it to the fallback value in the YAML file
        setValue(key, fallback);
        return fallback;
    }

    public <T> void setValue(String key, T value) {
        lock.writeLock().lock();
        try {
            Map<String, Object> root;
            // Read the existing YAML file or create an empty map if it doesn't exist
            if (Files.exists(path)) {
                try (InputStream inputStream = Files.newInputStream(path)) {
                    root = new LinkedHashMap<>(yaml.load(inputStream));
                } catch (IOException e) {
                    throw new YamlMurmelException("Error reading file: " + path, e);
                }
            } else root = new LinkedHashMap<>();

            // Split the key into parts and navigate to the correct location in the YAML
            String[] keys = key.split("\\.");
            Map<String, Object> current = root;

            for (int i = 0; i < keys.length - 1; i++) {
                String part = keys[i];
                Object next = current.get(part);

                if (next instanceof Map<?, ?> rawNext) {
                    Map<String, Object> nextMap = new LinkedHashMap<>();
                    for (Map.Entry<?, ?> entry : rawNext.entrySet())
                        nextMap.put(entry.getKey().toString(), entry.getValue());
                    current.put(part, nextMap);
                    current = nextMap;
                } else {
                    Map<String, Object> newMap = new LinkedHashMap<>();
                    current.put(part, newMap);
                    current = newMap;
                }
            }

            current.put(keys[keys.length - 1], value);

            // Write the updated map back to the YAML file
            try {
                if (path.getParent() != null)
                    Files.createDirectories(path.getParent());

                Files.writeString(path, yaml.dump(root));
            } catch (IOException e) {
                throw new YamlMurmelException("Error writing to file: " + path, e);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void removeKey(String key) {
        lock.writeLock().lock();
        try {
            Map<String, Object> root;
            // Read the existing YAML file or return if it doesn't exist
            if (Files.exists(path)) {
                try (InputStream inputStream = Files.newInputStream(path)) {
                    root = new LinkedHashMap<>(yaml.load(inputStream));
                } catch (IOException e) {
                    throw new YamlMurmelException("Error reading file: " + path, e);
                }
            } else return; // Nothing to remove if the file doesn't exist


            // Split the key into parts and navigate to the correct location in the YAML
            String[] keys = key.split("\\.");
            int depth = keys.length;

            List<Map<String, Object>> nodeStack = new LinkedList<>(Collections.nCopies(depth, null));
            List<String> keyStack = new ArrayList<>(Collections.nCopies(depth, null));

            Map<String, Object> current = root;
            for (int i = 0; i < depth - 1; i++) {
                String part = keys[i];
                Object next = current.get(part);

                if (!(next instanceof Map<?, ?> rawNext)) {
                    return; // Key not found
                }
                Map<String, Object> nextMap = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : rawNext.entrySet())
                    nextMap.put(entry.getKey().toString(), entry.getValue());

                // Update the stacks with the current node and key
                nodeStack.set(i, current);
                keyStack.set(i, part);
                current.put(part, nextMap);
                current = nextMap;
            }

            // Set the current node and key for the last part
            nodeStack.set(depth - 1, current);
            keyStack.set(depth - 1, keys[keys.length - 1]);

            // Remove the last key from the current map
            String lastKey = keys[depth - 1];
            if (current.containsKey(lastKey)) {
                current.remove(lastKey);

                // Clean up empty parent nodes
                for (int i = depth - 1; i > 0; i--) {
                    Map<String, Object> removed = nodeStack.get(i);
                    if (!removed.isEmpty()) break;
                    Map<String, Object> parent = nodeStack.get(i - 1);
                    String keyToRemove = keyStack.get(i - 1);
                    parent.remove(keyToRemove);
                }

                // Write the updated map back to the YAML file
                try {
                    Files.writeString(path, yaml.dump(root));
                } catch (IOException e) {
                    throw new YamlMurmelException("Error writing to file: " + path, e);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void addComment(String key, String comment, boolean inLine) {
        lock.writeLock().lock();
        try {
            Node rootNode;
            // Read the existing YAML file or return if it doesn't exist
            if (Files.exists(path)) {
                try (BufferedReader reader = Files.newBufferedReader(path)) {
                    rootNode = yaml.compose(reader);
                } catch (IOException e) {
                    throw new YamlMurmelException("Error reading file: " + path, e);
                }
            } else return; // Nothing to remove if the file doesn't exist

            if (!(rootNode instanceof MappingNode))
                return; // Not a map, nothing to comment

            String[] keys = key.split("\\.");
            NodeTuple tuple = findNodeTuple(rootNode, keys);

            if (tuple == null)
                return; // Key not found, nothing to comment

            if (inLine) setCommentInLine(tuple.getValueNode(), comment);
            else setCommentBlock(tuple.getKeyNode(), comment);

            // Write the updated YAML back to the file
            try {
                if (path.getParent() != null)
                    Files.createDirectories(path.getParent());

                StringWriter writer = new StringWriter();
                yaml.serialize(rootNode, writer);
                Files.writeString(path, writer.toString());
            } catch (IOException e) {
                throw new YamlMurmelException("Error writing to file: " + path, e);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void setCommentInLine(Node node, String comment) {
        CommentLine line = new CommentLine(null, null, " " + comment, CommentType.IN_LINE);
        List<CommentLine> comments = node.getInLineComments();
        comments.add(line);
        node.setInLineComments(comments);
    }

    private void setCommentBlock(Node node, String comment) {
        CommentLine line = new CommentLine(null, null, " " + comment, CommentType.BLOCK);
        List<CommentLine> comments = node.getBlockComments();
        comments.add(line);
        node.setBlockComments(comments);
    }

    private NodeTuple findNodeTuple(Node root, String[] path) {
        if (!(root instanceof MappingNode mappingNode))
            return null;

        String currentKey = path[0];
        for (NodeTuple tuple : mappingNode.getValue()) {
            Node keyNode = tuple.getKeyNode();
            Node valueNode = tuple.getValueNode();

            if (keyNode instanceof ScalarNode scalarNode
                && scalarNode.getValue().equals(currentKey)) {
                if (path.length == 1)
                    return tuple; // Found the key, return the tuple
                String[] remainingPath = Arrays.copyOfRange(path, 1, path.length);
                return findNodeTuple(valueNode, remainingPath); // Recursive for the next part of the path
            }
        }
        return null; // Key not found
    }

    public static Builder builder(String fileName) {
        return new Builder(fileName);
    }

    public static final class Builder {
        private final String fileName;
        private int indent = 2;
        private boolean prettyFlow = true;
        private int width = 80;
        private boolean processComments = true;
        private boolean allowDuplicateKeys = false;
        private int maxAliasesForCollections = 100;
        private boolean wrappedToRootException = true;

        private Builder(String fileName) {
            this.fileName = fileName;
        }

        public Builder indent(int indent) {
            this.indent = indent;
            return this;
        }

        public Builder prettyFlow(boolean prettyFlow) {
            this.prettyFlow = prettyFlow;
            return this;
        }

        public Builder width(int width) {
            this.width = width;
            return this;
        }

        public Builder processComments(boolean processComments) {
            this.processComments = processComments;
            return this;
        }

        public Builder allowDuplicateKeys(boolean allowDuplicateKeys) {
            this.allowDuplicateKeys = allowDuplicateKeys;
            return this;
        }

        public Builder maxAliasesForCollections(int maxAliasesForCollections) {
            this.maxAliasesForCollections = maxAliasesForCollections;
            return this;
        }

        public Builder wrappedToRootException(boolean wrappedToRootException) {
            this.wrappedToRootException = wrappedToRootException;
            return this;
        }

        public YamlMurmel build() {
            return new YamlMurmel(this);
        }
    }
}
