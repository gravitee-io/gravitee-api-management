/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.impl.search.lucene.transformer;

import static io.gravitee.rest.api.service.impl.search.lucene.DocumentTransformer.FIELD_REFERENCE_ID;
import static io.gravitee.rest.api.service.impl.search.lucene.DocumentTransformer.FIELD_REFERENCE_TYPE;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_API_LIFECYCLE_STATE;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_CATEGORIES;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_CATEGORIES_SPLIT;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_DEFINITION_VERSION;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_DESCRIPTION;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_DESCRIPTION_LOWERCASE;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_DESCRIPTION_SPLIT;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_HOSTS;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_ID;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_LABELS;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_LABELS_LOWERCASE;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_LABELS_SPLIT;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_METADATA;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_METADATA_SPLIT;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_NAME;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_NAME_LOWERCASE;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_NAME_SPLIT;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_ORIGIN;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_OWNER;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_OWNER_LOWERCASE;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_OWNER_MAIL;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_PATHS;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_PATHS_SPLIT;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_STATUS;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_TAGS;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_TAGS_SPLIT;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import fixtures.core.model.ApiFixtures;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.core.search.model.IndexableApi;
import io.gravitee.definition.model.DefinitionVersion;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.util.BytesRef;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

public class IndexableApiDocumentTransformerTest {

    private static final String API_ID = "api-id";

    private static final PrimaryOwnerEntity PRIMARY_OWNER = new PrimaryOwnerEntity(
        "user-id",
        "john.doe@gravitee.io",
        "John Doe",
        PrimaryOwnerEntity.Type.USER
    );

    IndexableApiDocumentTransformer cut = new IndexableApiDocumentTransformer();

    @Test
    void should_transform_id_and_type_only_when_definition_version_and_name_are_null() {
        // Given
        var indexable = new IndexableApi(
            Api.builder().id(API_ID).lifecycleState(Api.LifecycleState.STARTED).build(),
            PRIMARY_OWNER,
            Map.of(),
            Set.of()
        );

        // When
        var result = cut.transform(indexable);

        // Then
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getFields()).hasSize(6);
            softly.assertThat(result.getField(FIELD_ID).stringValue()).isEqualTo(API_ID);
            softly.assertThat(result.getField(FIELD_TYPE).stringValue()).isEqualTo("api");
            softly.assertThat(result.getField(FIELD_STATUS).stringValue()).isEqualTo(Api.LifecycleState.STARTED.name());
            softly.assertThat(result.getField(FIELD_API_LIFECYCLE_STATE).stringValue()).isEqualTo(Api.ApiLifecycleState.CREATED.name());
        });
    }

    @Test
    void should_transform_api_info() {
        // Given
        var indexable = new IndexableApi(
            ApiFixtures.aProxyApiV4().toBuilder().labels(List.of("Label1, Label2")).build(),
            PRIMARY_OWNER,
            Map.of(),
            Set.of("category1", "category2")
        );

        // When
        var result = cut.transform(indexable);

        // Then
        SoftAssertions.assertSoftly(softly -> {
            //            softly.assertThat(result.getFields()).hasSize(2);
            softly.assertThat(result.getField(FIELD_DEFINITION_VERSION).stringValue()).isEqualTo(DefinitionVersion.V4.getLabel());
            softly.assertThat(result.getField(FIELD_REFERENCE_TYPE).stringValue()).isEqualTo("ENVIRONMENT");
            softly.assertThat(result.getField(FIELD_REFERENCE_ID).stringValue()).isEqualTo("environment-id");

            // name
            softly.assertThat(result.getField(FIELD_NAME).stringValue()).isEqualTo("My Api");
            // FIELD_NAME_SORTED
            softly.assertThat(result.getField(FIELD_NAME_LOWERCASE).stringValue()).isEqualTo("my api");
            // FIELD_NAME_SPLIT

            // description
            softly.assertThat(result.getField(FIELD_DESCRIPTION).stringValue()).isEqualTo("api-description");
            // FIELD_DESCRIPTION_SORTED
            softly.assertThat(result.getField(FIELD_DESCRIPTION_LOWERCASE).stringValue()).isEqualTo("api-description");
            // FIELD_DESCRIPTION_SPLIT

            // paths
            softly.assertThat(result.getField(FIELD_PATHS).stringValue()).isEqualTo("/http_proxy");
            softly.assertThat(result.getField(FIELD_PATHS_SPLIT).stringValue()).isEqualTo("/http_proxy");
            // FIELD_HOSTS
            // FIELD_HOSTS_SPLIT
            // FIELD_PATHS_SORTED

            // labels
            softly.assertThat(result.getField(FIELD_LABELS).stringValue()).isEqualTo("Label1, Label2");
            softly.assertThat(result.getField(FIELD_LABELS_LOWERCASE).stringValue()).isEqualTo("label1, label2");
            softly.assertThat(result.getField(FIELD_LABELS_SPLIT).stringValue()).isEqualTo("Label1, Label2");

            // categories
            softly
                .assertThat(result.getFields(FIELD_CATEGORIES))
                .extracting(IndexableField::stringValue)
                .contains("category1", "category2");
            softly
                .assertThat(result.getFields(FIELD_CATEGORIES_SPLIT))
                .extracting(IndexableField::stringValue)
                .contains("category1", "category2");

            // tags
            softly.assertThat(result.getFields(FIELD_TAGS)).extracting(IndexableField::stringValue).contains("tag1");
            softly.assertThat(result.getFields(FIELD_TAGS_SPLIT)).extracting(IndexableField::stringValue).contains("tag1");

            // origin
            softly.assertThat(result.getField(FIELD_ORIGIN).stringValue()).isEqualTo("management");
        });
    }

    @Test
    void should_transform_primary_owner_info() {
        // Given
        var indexable = new IndexableApi(ApiFixtures.aProxyApiV4(), PRIMARY_OWNER, Map.of(), Set.of());

        // When
        var result = cut.transform(indexable);

        // Then
        SoftAssertions.assertSoftly(softly -> {
            // owner
            softly.assertThat(result.getField(FIELD_OWNER).stringValue()).isEqualTo("John Doe");
            softly.assertThat(result.getField(FIELD_OWNER_LOWERCASE).stringValue()).isEqualTo("john doe");
            softly.assertThat(result.getField(FIELD_OWNER_MAIL).stringValue()).isEqualTo("john.doe@gravitee.io");
        });
    }

    @Test
    void should_transform_api_metadata() {
        // Given
        var indexable = new IndexableApi(
            ApiFixtures.aProxyApiV4(),
            PRIMARY_OWNER,
            Map.of("metadata1", "value1", "metadata2", "value2"),
            Set.of()
        );

        // When
        var result = cut.transform(indexable);

        // Then
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getFields(FIELD_METADATA)).extracting(IndexableField::stringValue).contains("value1", "value2");
            softly.assertThat(result.getFields(FIELD_METADATA_SPLIT)).extracting(IndexableField::stringValue).contains("value1", "value2");
        });
    }

    @Test
    void should_throw_when_not_V4_api() {
        // Given
        var indexable = new IndexableApi(ApiFixtures.aProxyApiV2(), PRIMARY_OWNER, Map.of(), Set.of());

        // When
        var throwable = catchThrowable(() -> cut.transform(indexable));

        // Then
        assertThat(throwable).isInstanceOf(TechnicalDomainException.class).hasMessage("Unsupported definition version: V2");
    }

    @Test
    void should_transform_a_federated_api() {
        // Given
        var indexable = new IndexableApi(
            ApiFixtures
                .aFederatedApi()
                .toBuilder()
                .id(API_ID)
                .lifecycleState(Api.LifecycleState.STARTED)
                .description("A Description")
                .labels(List.of("Label1, Label2"))
                .build(),
            PRIMARY_OWNER,
            Map.of(),
            Set.of("Category1", "Category2")
        );

        // When
        var result = cut.transform(indexable);

        // Then
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getField(FIELD_ID).stringValue()).isEqualTo(API_ID);
            softly.assertThat(result.getField(FIELD_TYPE).stringValue()).isEqualTo("api");
            softly.assertThat(result.getField(FIELD_DEFINITION_VERSION).stringValue()).isEqualTo("FEDERATED");
            softly.assertThat(result.getField(FIELD_REFERENCE_TYPE).stringValue()).isEqualTo("ENVIRONMENT");
            softly.assertThat(result.getField(FIELD_REFERENCE_ID).stringValue()).isEqualTo("environment-id");
            // Name
            softly.assertThat(result.getField(FIELD_NAME).stringValue()).isEqualTo("My Api");
            softly.assertThat(result.getField(FIELD_NAME_LOWERCASE).stringValue()).isEqualTo("my api");
            softly.assertThat(result.getField(FIELD_NAME_SPLIT).stringValue()).isEqualTo("My Api");
            // Description
            softly.assertThat(result.getField(FIELD_DESCRIPTION).stringValue()).isEqualTo("A Description");
            softly.assertThat(result.getField(FIELD_DESCRIPTION_LOWERCASE).stringValue()).isEqualTo("a description");
            softly.assertThat(result.getField(FIELD_DESCRIPTION_SPLIT).stringValue()).isEqualTo("A Description");
            // Primary Owner
            softly.assertThat(result.getField(FIELD_OWNER).stringValue()).isEqualTo("John Doe");
            softly.assertThat(result.getField(FIELD_OWNER_LOWERCASE).stringValue()).isEqualTo("john doe");
            softly.assertThat(result.getField(FIELD_OWNER_MAIL).stringValue()).isEqualTo("john.doe@gravitee.io");
            // Labels
            softly.assertThat(result.getField(FIELD_LABELS).stringValue()).isEqualTo("Label1, Label2");
            softly.assertThat(result.getField(FIELD_LABELS_LOWERCASE).stringValue()).isEqualTo("label1, label2");
            softly.assertThat(result.getField(FIELD_LABELS_SPLIT).stringValue()).isEqualTo("Label1, Label2");
            // Categories
            softly
                .assertThat(result.getFields(FIELD_CATEGORIES))
                .extracting(IndexableField::stringValue)
                .contains("Category1", "Category2");
            softly
                .assertThat(result.getFields(FIELD_CATEGORIES_SPLIT))
                .extracting(IndexableField::stringValue)
                .contains("Category1", "Category2");
        });
    }

    @Test
    void should_transform_a_native_api() {
        // Given
        var indexable = new IndexableApi(
            ApiFixtures.aNativeApi().toBuilder().id(API_ID).description("A Description").labels(List.of("Label1, Label2")).build(),
            PRIMARY_OWNER,
            Map.of(),
            Set.of("Category1", "Category2")
        );

        // When
        var result = cut.transform(indexable);

        // Then
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getField(FIELD_ID).stringValue()).isEqualTo(API_ID);
            softly.assertThat(result.getField(FIELD_TYPE).stringValue()).isEqualTo("api");
            softly.assertThat(result.getField(FIELD_DEFINITION_VERSION).stringValue()).isEqualTo("4.0.0");
            softly.assertThat(result.getField(FIELD_REFERENCE_TYPE).stringValue()).isEqualTo("ENVIRONMENT");
            softly.assertThat(result.getField(FIELD_REFERENCE_ID).stringValue()).isEqualTo("environment-id");
            // Name
            softly.assertThat(result.getField(FIELD_NAME).stringValue()).isEqualTo("My Api");
            softly.assertThat(result.getField(FIELD_NAME_LOWERCASE).stringValue()).isEqualTo("my api");
            softly.assertThat(result.getField(FIELD_NAME_SPLIT).stringValue()).isEqualTo("My Api");
            // Description
            softly.assertThat(result.getField(FIELD_DESCRIPTION).stringValue()).isEqualTo("A Description");
            softly.assertThat(result.getField(FIELD_DESCRIPTION_LOWERCASE).stringValue()).isEqualTo("a description");
            softly.assertThat(result.getField(FIELD_DESCRIPTION_SPLIT).stringValue()).isEqualTo("A Description");
            // Primary Owner
            softly.assertThat(result.getField(FIELD_OWNER).stringValue()).isEqualTo("John Doe");
            softly.assertThat(result.getField(FIELD_OWNER_LOWERCASE).stringValue()).isEqualTo("john doe");
            softly.assertThat(result.getField(FIELD_OWNER_MAIL).stringValue()).isEqualTo("john.doe@gravitee.io");
            // Labels
            softly.assertThat(result.getField(FIELD_LABELS).stringValue()).isEqualTo("Label1, Label2");
            softly.assertThat(result.getField(FIELD_LABELS_LOWERCASE).stringValue()).isEqualTo("label1, label2");
            softly.assertThat(result.getField(FIELD_LABELS_SPLIT).stringValue()).isEqualTo("Label1, Label2");
            // Categories
            softly
                .assertThat(result.getFields(FIELD_CATEGORIES))
                .extracting(IndexableField::stringValue)
                .contains("Category1", "Category2");
            softly
                .assertThat(result.getFields(FIELD_CATEGORIES_SPLIT))
                .extracting(IndexableField::stringValue)
                .contains("Category1", "Category2");
            softly.assertThat(result.getFields(FIELD_HOSTS)).extracting(IndexableField::stringValue).contains("native.kafka");
        });
    }

    @Test
    void should_sort_names_by_bytesref() throws Exception {
        List<String> names = List.of("Nano", "zorro", "äther", "Vem", "épée", "épona", "Öko", "bns");
        List<String> expectedSorted = List.of("äther", "bns", "épée", "épona", "Nano", "Öko", "Vem", "zorro");

        Method toSortedValueMethod = IndexableApiDocumentTransformer.class.getDeclaredMethod("toSortedValue", String.class);
        toSortedValueMethod.setAccessible(true);
        Map<String, BytesRef> bytesRefMap = new HashMap<>();
        for (String name : names) {
            BytesRef key = (BytesRef) toSortedValueMethod.invoke(cut, name);
            bytesRefMap.put(name, key);
        }
        List<String> sortedByBytesRef = new ArrayList<>(names);
        sortedByBytesRef.sort(Comparator.comparing(bytesRefMap::get, BytesRef::compareTo));

        // Also sort with collator directly for comparison
        List<String> sortedByCollator = new ArrayList<>(names);
        Field collatorField = IndexableApiDocumentTransformer.class.getDeclaredField("collator");
        collatorField.setAccessible(true);
        Collator collator = (Collator) collatorField.get(cut);
        sortedByCollator.sort(collator);

        // Assertions
        assertThat(sortedByBytesRef).isEqualTo(expectedSorted);
        assertThat(sortedByCollator).isEqualTo(expectedSorted);
    }

    @Test
    void should_sort_names_with_special_characters_correctly() throws Exception {
        List<String> names = List.of("épée-bar", "épée", "zorro/name", "äther", "nano");
        List<String> expectedSorted = List.of("äther", "épée", "épée-bar", "nano", "zorro/name");
        Method toSortedValueMethod = IndexableApiDocumentTransformer.class.getDeclaredMethod("toSortedValue", String.class);
        toSortedValueMethod.setAccessible(true);
        Map<String, BytesRef> bytesRefMap = new HashMap<>();
        for (String name : names) {
            BytesRef key = (BytesRef) toSortedValueMethod.invoke(cut, name);
            bytesRefMap.put(name, key);
        }
        List<String> sortedByBytesRef = new ArrayList<>(names);
        sortedByBytesRef.sort(Comparator.comparing(bytesRefMap::get, BytesRef::compareTo));

        // Also sort with collator directly for comparison
        List<String> sortedByCollator = new ArrayList<>(names);
        Field collatorField = IndexableApiDocumentTransformer.class.getDeclaredField("collator");
        collatorField.setAccessible(true);
        Collator collator = (Collator) collatorField.get(cut);
        sortedByCollator.sort(collator);

        // Assertions
        assertThat(sortedByBytesRef).isEqualTo(expectedSorted);
        assertThat(sortedByCollator).isEqualTo(expectedSorted);
    }
}
