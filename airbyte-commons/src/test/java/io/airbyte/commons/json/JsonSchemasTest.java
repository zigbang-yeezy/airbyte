/*
 * Copyright (c) 2021 Airbyte, Inc., all rights reserved.
 */

package io.airbyte.commons.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.JsonNode;
import io.airbyte.commons.resources.MoreResources;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.Mockito;

class JsonSchemasTest {

  @Test
  void testMutateTypeToArrayStandard() {
    final JsonNode expectedWithoutType = Jsons.deserialize("{\"test\":\"abc\"}");
    final JsonNode actualWithoutType = Jsons.clone(expectedWithoutType);
    JsonSchemas.mutateTypeToArrayStandard(expectedWithoutType);
    assertEquals(expectedWithoutType, actualWithoutType);

    final JsonNode expectedWithArrayType = Jsons.deserialize("{\"test\":\"abc\", \"type\":[\"object\"]}");
    final JsonNode actualWithArrayType = Jsons.clone(expectedWithArrayType);
    JsonSchemas.mutateTypeToArrayStandard(actualWithArrayType);
    assertEquals(expectedWithoutType, actualWithoutType);

    final JsonNode expectedWithoutArrayType = Jsons.deserialize("{\"test\":\"abc\", \"type\":[\"object\"]}");
    final JsonNode actualWithStringType = Jsons.deserialize("{\"test\":\"abc\", \"type\":\"object\"}");
    JsonSchemas.mutateTypeToArrayStandard(actualWithStringType);
    assertEquals(expectedWithoutArrayType, actualWithStringType);
  }

  @SuppressWarnings("unchecked")
  @Test
  void testTraverse() throws IOException {
    final JsonNode jsonWithAllTypes = Jsons.deserialize(MoreResources.readResource("json_schemas/json_with_all_types.json"));
    final BiConsumer<JsonNode, List<String>> mock = mock(BiConsumer.class);

    JsonSchemas.traverseJsonSchema(jsonWithAllTypes, mock);
    final InOrder inOrder = Mockito.inOrder(mock);
    inOrder.verify(mock).accept(jsonWithAllTypes, Collections.emptyList());
    inOrder.verify(mock).accept(jsonWithAllTypes.get("properties").get("name"), List.of("name"));
    inOrder.verify(mock).accept(jsonWithAllTypes.get("properties").get("name").get("properties").get("first"), List.of("name", "first"));
    inOrder.verify(mock).accept(jsonWithAllTypes.get("properties").get("name").get("properties").get("last"), List.of("name", "last"));
    inOrder.verify(mock).accept(jsonWithAllTypes.get("properties").get("company"), List.of("company"));
    inOrder.verify(mock).accept(jsonWithAllTypes.get("properties").get("pets"), List.of("pets"));
    inOrder.verify(mock).accept(jsonWithAllTypes.get("properties").get("pets").get("items"), List.of("pets", "[]"));
    inOrder.verify(mock).accept(jsonWithAllTypes.get("properties").get("pets").get("items").get("properties").get("type"),
        List.of("pets", "[]", "type"));
    inOrder.verify(mock).accept(jsonWithAllTypes.get("properties").get("pets").get("items").get("properties").get("number"),
        List.of("pets", "[]", "number"));
    inOrder.verifyNoMoreInteractions();
  }

  @SuppressWarnings("unchecked")
  @ValueSource(strings = {
     "anyOf",
     "oneOf",
    "allOf"
  })
  @ParameterizedTest
  void testTraverseComposite(final String compositeKeyword) throws IOException {
    final String jsonSchemaString = MoreResources.readResource("json_schemas/composite_json_schema.json")
        .replaceAll("<composite-placeholder>", compositeKeyword);
    final JsonNode jsonWithAllTypes = Jsons.deserialize(jsonSchemaString);
    final BiConsumer<JsonNode, List<String>> mock = mock(BiConsumer.class);

    JsonSchemas.traverseJsonSchema(jsonWithAllTypes, mock);

    final InOrder inOrder = Mockito.inOrder(mock);
    inOrder.verify(mock).accept(jsonWithAllTypes, Collections.emptyList());
    inOrder.verify(mock).accept(jsonWithAllTypes.get(compositeKeyword).get(0), Collections.emptyList());
    inOrder.verify(mock).accept(jsonWithAllTypes.get(compositeKeyword).get(1), Collections.emptyList());
    inOrder.verify(mock).accept(jsonWithAllTypes.get(compositeKeyword).get(1).get("properties").get("prop1"), List.of("prop1"));
    inOrder.verify(mock).accept(jsonWithAllTypes.get(compositeKeyword).get(2), Collections.emptyList());
    inOrder.verify(mock).accept(jsonWithAllTypes.get(compositeKeyword).get(2).get("items"), List.of("[]"));
    inOrder.verify(mock).accept(jsonWithAllTypes.get(compositeKeyword).get(3).get(compositeKeyword).get(0), Collections.emptyList());
    inOrder.verify(mock).accept(jsonWithAllTypes.get(compositeKeyword).get(3).get(compositeKeyword).get(1), Collections.emptyList());
    inOrder.verify(mock).accept(jsonWithAllTypes.get(compositeKeyword).get(3).get(compositeKeyword).get(1).get("items"), List.of("[]"));
    inOrder.verifyNoMoreInteractions();
  }

}
