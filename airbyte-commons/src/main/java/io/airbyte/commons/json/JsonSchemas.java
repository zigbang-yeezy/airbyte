/*
 * Copyright (c) 2021 Airbyte, Inc., all rights reserved.
 */

package io.airbyte.commons.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.airbyte.commons.io.IOs;
import io.airbyte.commons.resources.MoreResources;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// todo handle capitalization
// todo we don't hande not. fail explicitly if we enoucnter it
public class JsonSchemas {

  private static final String JSON_SCHEMA_TYPE_KEY = "type";
  private static final String JSON_SCHEMA_PROPERTIES_KEY = "properties";
  private static final String JSON_SCHEMA_ITEMS_KEY = "items";

  // all JSONSchema types.
  private static final String ARRAY_TYPE = "array";
  private static final String OBJECT_TYPE = "object";
  private static final String STRING_TYPE = "string";
  private static final String NUMBER_TYPE = "number";
  private static final String BOOLEAN_TYPE = "boolean";
  private static final String NULL_TYPE = "null";
  private static final String ONE_OF_TYPE = "oneOf";
  private static final String ALL_OF_TYPE = "allOf";
  private static final String ANY_OF_TYPE = "anyOf";

  private static final String ARRAY_JSON_PATH = "[]";

  private static final Set<String> COMPOSITE_KEYWORDS = Set.of(ONE_OF_TYPE, ALL_OF_TYPE, ANY_OF_TYPE);

  /**
   * JsonSchema supports to ways of declaring type. `type: "string"` and `type: ["null", "string"]`.
   * This method will mutate a JsonNode with a type field so that the output type is the array
   * version.
   *
   * @param jsonNode - a json object with children that contain types.
   */
  public static void mutateTypeToArrayStandard(final JsonNode jsonNode) {
    if (jsonNode.get(JSON_SCHEMA_TYPE_KEY) != null && !jsonNode.get(JSON_SCHEMA_TYPE_KEY).isArray()) {
      final JsonNode type = jsonNode.get(JSON_SCHEMA_TYPE_KEY);
      ((ObjectNode) jsonNode).putArray(JSON_SCHEMA_TYPE_KEY).add(type);
    }
  }

  /*
   * JsonReferenceProcessor relies on all of the json in consumes being in a file system (not in a
   * jar). This method copies all of the json configs out of the jar into a temporary directory so
   * that JsonReferenceProcessor can find them.
   */
  public static <T> Path prepareSchemas(final String resourceDir, final Class<T> klass) {
    try {
      final List<String> filenames;
      try (final Stream<Path> resources = MoreResources.listResources(klass, resourceDir)) {
        filenames = resources.map(p -> p.getFileName().toString())
            .filter(p -> p.endsWith(".yaml"))
            .collect(Collectors.toList());
      }

      final Path configRoot = Files.createTempDirectory("schemas");
      for (final String filename : filenames) {
        IOs.writeFile(
            configRoot,
            filename,
            MoreResources.readResource(String.format("%s/%s", resourceDir, filename)));
      }

      return configRoot;
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  // todo handle type as array e.g. [number, string]
  /**
   * Traverse JSON object as JSONSchema. The provided consumer will be called at each node and contain
   * the path from the root node to the current node.
   *
   * @param jsonNode - json object to traverse.
   * @param consumer - consumer to be called at each node. it accepts the node and the path to the
   *        node from the root of the object passed to this method (the jsonNode argument to this
   *        method)
   */
  public static void traverseJsonSchema(final JsonNode jsonNode, final BiConsumer<JsonNode, List<String>> consumer) {
    traverseJsonSchema(jsonNode, new ArrayList<>(), consumer);
  }

  /**
   * Recursive implementation of { @link JsonSchemas#traverseJsonSchema(final JsonNode jsonNode, final
   * BiConsumer<JsonNode, List<String>> consumer) }. Takes path as argument so that the path can be
   * passsed to the consumer.
   *
   * @param jsonNode - json object to traverse.
   * @param path - path from the first call of traverseJsonSchema to the current node.
   * @param consumer - consumer to be called at each node. it accepts the node and the path to the
   *        node from the root of the object passed at the root level invocation of
   *        traverseJsonSchema.
   */
  private static void traverseJsonSchema(final JsonNode jsonNode,
                                         final List<String> path,
                                         final BiConsumer<JsonNode, List<String>> consumer) {
    if (!jsonNode.isObject()) {
      throw new IllegalArgumentException(String.format("json schema nodes should always be object nodes. path: %s actual: %s", path, jsonNode));
    }

    consumer.accept(jsonNode, path);

    // if type is missing assume object. not official JsonSchema, but it seems to be a common
    // compromise.
    final String nodeType = jsonNode.has(JSON_SCHEMA_TYPE_KEY) ? jsonNode.get(JSON_SCHEMA_TYPE_KEY).asText() : OBJECT_TYPE;

    switch (nodeType) {
      case ARRAY_TYPE -> {
        final List<String> newPath = new ArrayList<>(path);
        newPath.add(ARRAY_JSON_PATH);
        traverseJsonSchema(jsonNode.get(JSON_SCHEMA_ITEMS_KEY), newPath, consumer);
      }
      case OBJECT_TYPE -> {
        final Optional<String> comboKeyWordOptional = getKeywordIfComposite(jsonNode);
        if (jsonNode.has(JSON_SCHEMA_PROPERTIES_KEY)) {
          for (final Iterator<Entry<String, JsonNode>> it = jsonNode.get(JSON_SCHEMA_PROPERTIES_KEY).fields(); it.hasNext();) {
            final Entry<String, JsonNode> child = it.next();
            final List<String> newPath = new ArrayList<>(path);
            newPath.add(child.getKey());
            traverseJsonSchema(child.getValue(), newPath, consumer);
          }
        } else if (comboKeyWordOptional.isPresent()) {
          for (final JsonNode arrayItem : jsonNode.get(comboKeyWordOptional.get())) {
            traverseJsonSchema(arrayItem, path, consumer);
          }
        } else {
          throw new IllegalArgumentException(
              "malformed JsonSchema object type, must have one of the following fields: properties, oneOf, allOf, anyOf");
        }
      }
      // if a value type do nothing
      // case BOOLEAN_TYPE, NUMBER_TYPE, STRING_TYPE, NULL_TYPE -> consumer.accept(jsonNode, path);
    }
  }

  /**
   * If the object uses JSONSchema composite functionality (e.g. oneOf, anyOf, allOf), detect it and
   * return which one it is using.
   *
   * @param node - object to detect use of composite functionality.
   * @return the composite functionality being used, if not using composite functionality, empty.
   */
  private static Optional<String> getKeywordIfComposite(final JsonNode node) {
    for (final String keyWord : COMPOSITE_KEYWORDS) {
      if (node.has(keyWord)) {
        return Optional.ofNullable(keyWord);
      }
    }
    return Optional.empty();
  }

}
