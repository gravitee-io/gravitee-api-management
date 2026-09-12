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
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_ALLOW_IN_API_PRODUCTS;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_API_TYPE;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_CATEGORIES;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_CATEGORIES_SPLIT;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_DEFINITION_VERSION;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_DESCRIPTION;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_DESCRIPTION_LOWERCASE;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_DESCRIPTION_SPLIT;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_HAS_HEALTH_CHECK;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_HOSTS;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_ID;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_INTEGRATION_ID;
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
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_TAGS;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_TAGS_ASC_SORTED;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_TAGS_DESC_SORTED;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_TAGS_SPLIT;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import fixtures.core.model.ApiFixtures;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.core.search.model.IndexableApi;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.services.healthcheck.EndpointHealthCheckService;
import io.gravitee.rest.api.model.context.OriginContext;
import io.gravitee.rest.api.service.ApiService;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.util.BytesRef;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
            softly.assertThat(result.getFields()).hasSize(2);
            softly.assertThat(result.getField(FIELD_ID).stringValue()).isEqualTo(API_ID);
            softly.assertThat(result.getField(FIELD_TYPE).stringValue()).isEqualTo("api");
        });
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("definitionVersionTerms")
    void should_index_definition_version_as_its_label(String caseName, Api api, String expectedTerm) {
        // Given an api carrying the row's definition version
        var indexable = new IndexableApi(api, PRIMARY_OWNER, Map.of(), Set.of());

        // When it is transformed for the boot-time index rebuild
        var result = cut.transform(indexable);

        // Then the document holds the row's term as its only definition version
        assertThat(result.getFields(FIELD_DEFINITION_VERSION)).extracting(IndexableField::stringValue).containsExactly(expectedTerm);
    }

    private static Stream<Arguments> definitionVersionTerms() {
        return Stream.of(
            Arguments.of("V2 is indexed as its label, not as the enum name", ApiFixtures.aProxyApiV2(), "2.0.0"),
            Arguments.of("V4 is indexed as its label, not as the enum name", ApiFixtures.aProxyApiV4(), "4.0.0"),
            Arguments.of("FEDERATED is indexed as its label", ApiFixtures.aFederatedApi(), "FEDERATED"),
            Arguments.of("FEDERATED_AGENT is indexed as its label", ApiFixtures.aFederatedAgent(), "FEDERATED_AGENT"),
            Arguments.of(
                "a legacy row with no definition version is indexed as the V2 label",
                aLegacyApiWithoutDefinitionVersion(),
                "2.0.0"
            )
        );
    }

    @Test
    void should_not_index_api_type_for_a_legacy_api_without_definition_version() {
        // Given a legacy api whose definition version was never stored
        var indexable = new IndexableApi(aLegacyApiWithoutDefinitionVersion(), PRIMARY_OWNER, Map.of(), Set.of());

        // When it is transformed for the boot-time index rebuild
        var result = cut.transform(indexable);

        // Then the V2 fallback that gave it a definition version term left it with no api type term
        assertThat(result.getFields(FIELD_API_TYPE)).isEmpty();
    }

    @Test
    void should_not_index_the_v2_field_set_for_a_legacy_api_without_definition_version() {
        // Given a legacy api whose definition version was never stored
        var indexable = new IndexableApi(aLegacyApiWithoutDefinitionVersion(), PRIMARY_OWNER, Map.of(), Set.of());

        // When it is transformed for the boot-time index rebuild
        var result = cut.transform(indexable);

        // Then the V2 fallback that gave it a definition version term left it outside the V2 branch and its health
        // check term
        assertThat(result.getFields(FIELD_HAS_HEALTH_CHECK)).isEmpty();
    }

    private static Api aLegacyApiWithoutDefinitionVersion() {
        return Api.builder().id(API_ID).name("Legacy Api").build();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("integrationIdTerms")
    void should_index_an_integration_id_only_when_the_origin_context_carries_one(
        String caseName,
        OriginContext originContext,
        List<String> expectedTerms
    ) {
        // Given an api carrying the row's origin context
        var api = ApiFixtures.aFederatedApi().toBuilder().originContext(originContext).build();
        var indexable = new IndexableApi(api, PRIMARY_OWNER, Map.of(), Set.of());

        // When it is transformed for the boot-time index rebuild
        var result = cut.transform(indexable);

        // Then the document holds the row's terms as its only integration id
        assertThat(result.getFields(FIELD_INTEGRATION_ID)).extracting(IndexableField::stringValue).containsExactlyElementsOf(expectedTerms);
    }

    private static Stream<Arguments> integrationIdTerms() {
        return Stream.of(
            Arguments.of(
                "an integration id is indexed spelled exactly as the origin context carries it, upper case and hyphens included",
                new OriginContext.Integration("Int-A-2024"),
                List.of("Int-A-2024")
            ),
            Arguments.of(
                "an integration origin context holding no id is indexed with no integration id",
                new OriginContext.Integration(null),
                List.of()
            ),
            Arguments.of("an api no integration owns is indexed with no integration id", new OriginContext.Management(), List.of())
        );
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

            // null allowedInApiProducts is normalized to false so the API remains findable by allowedInApiProducts=false
            softly.assertThat(result.getField(FIELD_ALLOW_IN_API_PRODUCTS).stringValue()).isEqualTo("false");
        });
    }

    @Test
    void should_index_explicit_false_allowedInApiProducts() {
        var indexable = new IndexableApi(
            ApiFixtures.aProxyApiV4()
                .toBuilder()
                .apiDefinitionHttpV4(io.gravitee.definition.model.v4.Api.builder().allowedInApiProducts(false).build())
                .build(),
            PRIMARY_OWNER,
            Map.of(),
            Set.of()
        );

        var result = cut.transform(indexable);

        assertThat(result.getField(FIELD_ALLOW_IN_API_PRODUCTS).stringValue()).isEqualTo("false");
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
    void should_transform_a_federated_api() {
        // Given
        var indexable = new IndexableApi(
            ApiFixtures.aFederatedApi()
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
            softly.assertThat(result.getField(FIELD_ALLOW_IN_API_PRODUCTS)).isNull();
        });
    }

    @Test
    void should_index_v4_api_paths_and_hosts_lowercase() {
        var api = ApiFixtures.aProxyApiV4()
            .toBuilder()
            .id(API_ID)
            .apiDefinitionHttpV4(
                io.gravitee.definition.model.v4.Api.builder()
                    .allowedInApiProducts(true)
                    .listeners(
                        List.of(
                            io.gravitee.definition.model.v4.listener.http.HttpListener.builder()
                                .paths(
                                    List.of(
                                        io.gravitee.definition.model.v4.listener.http.Path.builder()
                                            .path("/TestPath")
                                            .host("api.TestHost.com")
                                            .build()
                                    )
                                )
                                .build()
                        )
                    )
                    .build()
            )
            .build();
        var indexable = new IndexableApi(api, PRIMARY_OWNER, Map.of(), Set.of());
        var result = cut.transform(indexable);
        assertThat(result.getFields("paths_lowercase")[0].stringValue()).isEqualTo("/testpath");
        assertThat(result.getFields("hosts_lowercase")[0].stringValue()).isEqualTo("api.testhost.com");
        assertThat(result.getField(FIELD_ALLOW_IN_API_PRODUCTS).stringValue()).isEqualTo("true");
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

    @Nested
    @ExtendWith(MockitoExtension.class)
    class V2ApiDocumentTransformer {

        @Mock
        ApiService apiService;

        private final IndexableApiDocumentTransformer indexableApiDocumentTransformer = new IndexableApiDocumentTransformer();
        private ApiDocumentTransformer apiDocumentTransformer;

        @BeforeEach
        void setUp() {
            this.apiDocumentTransformer = new ApiDocumentTransformer(apiService);
        }

        @Test
        void should_transform_v2_api_with_same_fields_as_api_document_transformer() {
            // Given
            var v2Api = createV2ApiWithFullData();
            var indexableApi = new IndexableApi(v2Api, PRIMARY_OWNER, Map.of("metadata1", "value1"), Set.of("category1", "category2"));
            var genericApiEntity = convertToGenericApiEntity(v2Api);

            // When
            var indexableApiDocument = indexableApiDocumentTransformer.transform(indexableApi);
            var apiDocument = apiDocumentTransformer.transform(genericApiEntity);

            // Then
            assertDocumentsAreEquivalent(indexableApiDocument, apiDocument);
        }

        @Test
        void should_handle_v2_api_paths_correctly() {
            // Given
            var v2Api = createV2ApiWithPaths();
            var indexableApi = new IndexableApi(v2Api, PRIMARY_OWNER, Map.of(), Set.of());
            var genericApiEntity = convertToGenericApiEntity(v2Api);

            // When
            var indexableApiDocument = indexableApiDocumentTransformer.transform(indexableApi);
            var apiDocument = apiDocumentTransformer.transform(genericApiEntity);

            // Then
            SoftAssertions.assertSoftly(softly -> {
                softly
                    .assertThat(indexableApiDocument.getFields(FIELD_PATHS))
                    .extracting(IndexableField::stringValue)
                    .containsExactlyInAnyOrderElementsOf(
                        Arrays.stream(apiDocument.getFields(FIELD_PATHS)).map(IndexableField::stringValue).toList()
                    );
                softly
                    .assertThat(indexableApiDocument.getFields(FIELD_HOSTS))
                    .extracting(IndexableField::stringValue)
                    .containsExactlyInAnyOrderElementsOf(
                        Arrays.stream(apiDocument.getFields(FIELD_HOSTS)).map(IndexableField::stringValue).toList()
                    );
            });
        }

        @Test
        void should_handle_v2_api_tags_correctly() {
            // Given
            var v2Api = createV2ApiWithTags();
            var indexableApi = new IndexableApi(v2Api, PRIMARY_OWNER, Map.of(), Set.of());
            var genericApiEntity = convertToGenericApiEntity(v2Api);

            // When
            var indexableApiDocument = indexableApiDocumentTransformer.transform(indexableApi);
            var apiDocument = apiDocumentTransformer.transform(genericApiEntity);

            // Then
            SoftAssertions.assertSoftly(softly -> {
                softly
                    .assertThat(indexableApiDocument.getFields(FIELD_TAGS))
                    .extracting(IndexableField::stringValue)
                    .containsExactlyInAnyOrderElementsOf(
                        Arrays.stream(apiDocument.getFields(FIELD_TAGS)).map(IndexableField::stringValue).toList()
                    );
                softly
                    .assertThat(indexableApiDocument.getField(FIELD_TAGS_ASC_SORTED).binaryValue())
                    .isEqualTo(apiDocument.getField(FIELD_TAGS_ASC_SORTED).binaryValue());
                softly
                    .assertThat(indexableApiDocument.getField(FIELD_TAGS_DESC_SORTED).binaryValue())
                    .isEqualTo(apiDocument.getField(FIELD_TAGS_DESC_SORTED).binaryValue());
            });
        }

        @Test
        void should_handle_v2_api_health_check_correctly() {
            // Given
            var v2Api = createV2ApiWithHealthCheck();
            var indexableApi = new IndexableApi(v2Api, PRIMARY_OWNER, Map.of(), Set.of());
            var genericApiEntity = convertToGenericApiEntity(v2Api);
            when(apiService.hasHealthCheckEnabled(any(), eq(false))).thenReturn(true);

            // When
            var indexableApiDocument = indexableApiDocumentTransformer.transform(indexableApi);
            var apiDocument = apiDocumentTransformer.transform(genericApiEntity);

            // Then
            assertThat(indexableApiDocument.getField(FIELD_HAS_HEALTH_CHECK).stringValue()).isEqualTo(
                apiDocument.getField(FIELD_HAS_HEALTH_CHECK).stringValue()
            );
        }

        @Test
        void should_handle_v2_api_origin_context_correctly() {
            // Given
            var v2Api = createV2ApiWithOriginContext();
            var indexableApi = new IndexableApi(v2Api, PRIMARY_OWNER, Map.of(), Set.of());
            var genericApiEntity = convertToGenericApiEntity(v2Api);

            // When
            var indexableApiDocument = indexableApiDocumentTransformer.transform(indexableApi);
            var apiDocument = apiDocumentTransformer.transform(genericApiEntity);

            // Then
            assertThat(indexableApiDocument.getField(FIELD_ORIGIN).stringValue()).isEqualTo(
                apiDocument.getField(FIELD_ORIGIN).stringValue()
            );
        }

        private Api createV2ApiWithFullData() {
            return ApiFixtures.aProxyApiV2().toBuilder().labels(List.of("label1", "label2")).build();
        }

        private Api createV2ApiWithPaths() {
            return ApiFixtures.aProxyApiV2()
                .toBuilder()
                .apiDefinition(
                    io.gravitee.definition.model.Api.builder()
                        .id("api-id")
                        .name("api-name")
                        .version("1.0.0")
                        .definitionVersion(DefinitionVersion.V2)
                        .proxy(
                            io.gravitee.definition.model.Proxy.builder()
                                .virtualHosts(
                                    List.of(
                                        new io.gravitee.definition.model.VirtualHost("host1", "/path1"),
                                        new io.gravitee.definition.model.VirtualHost("host2", "/path2")
                                    )
                                )
                                .build()
                        )
                        .build()
                )
                .build();
        }

        private Api createV2ApiWithTags() {
            return ApiFixtures.aProxyApiV2()
                .toBuilder()
                .apiDefinition(
                    io.gravitee.definition.model.Api.builder()
                        .id("api-id")
                        .name("api-name")
                        .version("1.0.0")
                        .definitionVersion(DefinitionVersion.V2)
                        .tags(Set.of("tag1", "tag2", "tag3"))
                        .proxy(
                            io.gravitee.definition.model.Proxy.builder()
                                .virtualHosts(List.of(new io.gravitee.definition.model.VirtualHost("/path")))
                                .build()
                        )
                        .build()
                )
                .build();
        }

        private Api createV2ApiWithHealthCheck() {
            return ApiFixtures.aProxyApiV2()
                .toBuilder()
                .apiDefinition(
                    io.gravitee.definition.model.Api.builder()
                        .id("api-id")
                        .name("api-name")
                        .version("1.0.0")
                        .definitionVersion(DefinitionVersion.V2)
                        .proxy(
                            io.gravitee.definition.model.Proxy.builder()
                                .virtualHosts(List.of(new io.gravitee.definition.model.VirtualHost("/path")))
                                .groups(
                                    Set.of(
                                        io.gravitee.definition.model.EndpointGroup.builder()
                                            .name("default-group")
                                            .endpoints(
                                                Set.of(
                                                    io.gravitee.definition.model.Endpoint.builder()
                                                        .name("default")
                                                        .type("http1")
                                                        .target("https://api.gravitee.io/echo")
                                                        .healthCheck(EndpointHealthCheckService.builder().enabled(true).build())
                                                        .build()
                                                )
                                            )
                                            .build()
                                    )
                                )
                                .build()
                        )
                        .build()
                )
                .build();
        }

        private Api createV2ApiWithOriginContext() {
            return ApiFixtures.aProxyApiV2()
                .toBuilder()
                .originContext(new io.gravitee.rest.api.model.context.OriginContext.Management())
                .build();
        }

        private io.gravitee.rest.api.model.v4.api.GenericApiEntity convertToGenericApiEntity(Api api) {
            return new io.gravitee.rest.api.model.api.ApiEntity() {
                @Override
                public String getId() {
                    return api.getId();
                }

                @Override
                public String getName() {
                    return api.getName();
                }

                @Override
                public String getDescription() {
                    return api.getDescription();
                }

                @Override
                public io.gravitee.definition.model.DefinitionVersion getDefinitionVersion() {
                    return api.getDefinitionVersion();
                }

                @Override
                public io.gravitee.rest.api.model.api.ApiLifecycleState getLifecycleState() {
                    return api.getApiLifecycleState() == Api.ApiLifecycleState.PUBLISHED
                        ? io.gravitee.rest.api.model.api.ApiLifecycleState.PUBLISHED
                        : io.gravitee.rest.api.model.api.ApiLifecycleState.UNPUBLISHED;
                }

                @Override
                public Lifecycle.State getState() {
                    return api.getLifecycleState() == Api.LifecycleState.STARTED ? Lifecycle.State.STARTED : Lifecycle.State.STOPPED;
                }

                @Override
                public io.gravitee.rest.api.model.Visibility getVisibility() {
                    return api.getVisibility() == Api.Visibility.PUBLIC
                        ? io.gravitee.rest.api.model.Visibility.PUBLIC
                        : io.gravitee.rest.api.model.Visibility.PRIVATE;
                }

                @Override
                public io.gravitee.rest.api.model.PrimaryOwnerEntity getPrimaryOwner() {
                    return io.gravitee.rest.api.model.PrimaryOwnerEntity.builder()
                        .id(PRIMARY_OWNER.id())
                        .email(PRIMARY_OWNER.email())
                        .displayName(PRIMARY_OWNER.displayName())
                        .type(PRIMARY_OWNER.type().name())
                        .build();
                }

                @Override
                public java.util.List<String> getLabels() {
                    return api.getLabels();
                }

                @Override
                public java.util.Set<String> getCategories() {
                    return Set.of("category1", "category2");
                }

                @Override
                public java.util.Set<String> getTags() {
                    return api.getApiDefinition() != null ? api.getApiDefinition().getTags() : Set.of();
                }

                @Override
                public java.util.Date getCreatedAt() {
                    return java.util.Date.from(api.getCreatedAt().toInstant());
                }

                @Override
                public java.util.Date getUpdatedAt() {
                    return java.util.Date.from(api.getUpdatedAt().toInstant());
                }

                @Override
                public String getReferenceId() {
                    return api.getEnvironmentId();
                }

                @Override
                public String getReferenceType() {
                    return "ENVIRONMENT";
                }

                @Override
                public java.util.Map<String, Object> getMetadata() {
                    return Map.of("metadata1", "value1");
                }

                @Override
                public io.gravitee.rest.api.model.context.OriginContext getOriginContext() {
                    return api.getOriginContext();
                }

                @Override
                public io.gravitee.definition.model.Proxy getProxy() {
                    return api.getApiDefinition() != null ? api.getApiDefinition().getProxy() : null;
                }
            };
        }

        private void assertDocumentsAreEquivalent(Document newDoc, Document oldDoc) {
            SoftAssertions.assertSoftly(softly -> {
                var newFieldNames = newDoc.getFields().stream().map(IndexableField::name).collect(Collectors.toSet());
                var oldFieldNames = oldDoc.getFields().stream().map(IndexableField::name).collect(Collectors.toSet());

                // allow_in_api_products is only indexed for V4 HTTP APIs in IndexableApiDocumentTransformer,
                // while ApiDocumentTransformer may index it with a default value. Ignore that field for V2 equivalence.
                newFieldNames.remove(FIELD_ALLOW_IN_API_PRODUCTS);
                oldFieldNames.remove(FIELD_ALLOW_IN_API_PRODUCTS);

                softly.assertThat(newFieldNames).isEqualTo(oldFieldNames);

                for (String fieldName : newFieldNames) {
                    var newValues = Arrays.stream(newDoc.getFields(fieldName)).map(IndexableField::stringValue).collect(Collectors.toSet());
                    var oldValues = Arrays.stream(oldDoc.getFields(fieldName)).map(IndexableField::stringValue).collect(Collectors.toSet());

                    softly.assertThat(newValues).as("Field: %s", fieldName).isEqualTo(oldValues);
                }
            });
        }
    }
}
