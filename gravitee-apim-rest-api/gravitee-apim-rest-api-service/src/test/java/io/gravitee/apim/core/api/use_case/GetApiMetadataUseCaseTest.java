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
package io.gravitee.apim.core.api.use_case;

import static fixtures.core.model.ApiFixtures.aMessageApiV4;
import static fixtures.core.model.MetadataFixtures.aMetadata;
import static fixtures.core.model.MetadataFixtures.anApiMetadata;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiMetadataQueryServiceInMemory;
import inmemory.MetadataCrudServiceInMemory;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ApiMetadata;
import io.gravitee.apim.core.metadata.model.Metadata;
import io.gravitee.definition.model.flow.Operator;
import io.gravitee.rest.api.model.MetadataFormat;
import java.util.List;
import java.util.stream.Stream;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class GetApiMetadataUseCaseTest {

    private final String API_ID = "api-id";
    private final String ENV_ID = "env-id";
    private final Api API = aMessageApiV4().toBuilder().id(API_ID).environmentId(ENV_ID).build();
    private final MetadataCrudServiceInMemory metadataCrudServiceInMemory = new MetadataCrudServiceInMemory();
    private final ApiCrudServiceInMemory apiCrudServiceInMemory = new ApiCrudServiceInMemory();
    private ApiMetadataQueryServiceInMemory apiMetadataQueryService;
    private GetApiMetadataUseCase getApiMetadataUseCase;

    @BeforeEach
    void setUp() {
        apiMetadataQueryService = new ApiMetadataQueryServiceInMemory(metadataCrudServiceInMemory);
        getApiMetadataUseCase = new GetApiMetadataUseCase(apiCrudServiceInMemory, apiMetadataQueryService);
    }

    @AfterEach
    void tearDown() {
        apiCrudServiceInMemory.reset();
        apiMetadataQueryService.reset();
    }

    @Nested
    class WithValidApi {

        // Initial data to be loaded into in-memory repository
        private final Metadata metadata1 = aMetadata(
            Metadata.ReferenceType.API,
            API_ID,
            "key1",
            "key-1",
            "value1",
            Metadata.MetadataFormat.STRING
        );
        private final Metadata metadata2 = aMetadata(
            Metadata.ReferenceType.API,
            API_ID,
            "key2",
            "key-2",
            "value2",
            Metadata.MetadataFormat.STRING
        );
        private final Metadata globalMetadata2 = aMetadata(
            Metadata.ReferenceType.ENVIRONMENT,
            ENV_ID,
            "key2",
            "global-key-2",
            "global-value-2",
            Metadata.MetadataFormat.STRING
        );
        private final Metadata globalMetadata3 = aMetadata(
            Metadata.ReferenceType.ENVIRONMENT,
            ENV_ID,
            "key3",
            "key-3",
            "global-value-3",
            Metadata.MetadataFormat.STRING
        );
        private final Metadata metadata4 = aMetadata(
            Metadata.ReferenceType.API,
            API_ID,
            "a-key-4",
            "a-key-4-name",
            "value4",
            Metadata.MetadataFormat.STRING
        );

        // Simple api metadata
        private final ApiMetadata apiMetadata1 = anApiMetadata(
            API_ID,
            metadata1.getKey(),
            metadata1.getValue(),
            null,
            metadata1.getName(),
            Metadata.MetadataFormat.STRING
        );
        // Override global metadata
        private final ApiMetadata apiMetadata2 = anApiMetadata(
            API_ID,
            metadata2.getKey(),
            metadata2.getValue(),
            globalMetadata2.getValue(),
            metadata2.getName(),
            Metadata.MetadataFormat.STRING
        );
        // Simple global metadata
        private final ApiMetadata apiMetadata3 = anApiMetadata(
            null,
            globalMetadata3.getKey(),
            null,
            globalMetadata3.getValue(),
            globalMetadata3.getName(),
            Metadata.MetadataFormat.STRING
        );
        // Simple api metadata with key starting with "a"
        private final ApiMetadata apiMetadata4 = anApiMetadata(
            API_ID,
            metadata4.getKey(),
            metadata4.getValue(),
            null,
            metadata4.getName(),
            Metadata.MetadataFormat.STRING
        );

        @BeforeEach
        void setUpWithValidApi() {
            apiCrudServiceInMemory.initWith(List.of(API));
            metadataCrudServiceInMemory.initWith(List.of(metadata1, metadata2, globalMetadata2, globalMetadata3, metadata4));
        }

        @Test
        void should_return_list_of_metadata_and_sort_by_key() {
            var output = getApiMetadataUseCase.execute(new GetApiMetadataUseCase.Input(API_ID, ENV_ID, null, null));
            var metadata = output.metadata();

            assertThat(metadata)
                .hasSize(4)
                .usingRecursiveComparison()
                .isEqualTo(List.of(apiMetadata4, apiMetadata1, apiMetadata2, apiMetadata3));
        }

        @Test
        void should_filter_by_global_metadata() {
            var output = getApiMetadataUseCase.execute(new GetApiMetadataUseCase.Input(API_ID, ENV_ID, "GLOBAL", null));
            var metadata = output.metadata();

            assertThat(metadata).hasSize(2).usingRecursiveComparison().isEqualTo(List.of(apiMetadata2, apiMetadata3));
        }

        @Test
        void should_filter_by_api_metadata() {
            var output = getApiMetadataUseCase.execute(new GetApiMetadataUseCase.Input(API_ID, ENV_ID, "API", null));
            var metadata = output.metadata();

            assertThat(metadata).hasSize(2).usingRecursiveComparison().isEqualTo(List.of(apiMetadata4, apiMetadata1));
        }
    }

    @Nested
    class WithInvalidApi {

        @Test
        void should_throw_error_if_api_not_found() {
            apiCrudServiceInMemory.initWith(List.of(aMessageApiV4().toBuilder().id("not-to-be-found").environmentId(ENV_ID).build()));
            var throwable = catchThrowable(() -> getApiMetadataUseCase.execute(new GetApiMetadataUseCase.Input(API_ID, ENV_ID, null, null))
            );
            AssertionsForClassTypes.assertThat(throwable).isInstanceOf(ApiNotFoundException.class);
        }

        @Test
        void should_throw_error_if_api_found_and_environment_incorrect() {
            apiCrudServiceInMemory.initWith(List.of(aMessageApiV4().toBuilder().id(API_ID).environmentId("not-to-be-found").build()));
            var throwable = catchThrowable(() -> getApiMetadataUseCase.execute(new GetApiMetadataUseCase.Input(API_ID, ENV_ID, null, null))
            );
            AssertionsForClassTypes.assertThat(throwable).isInstanceOf(ApiNotFoundException.class);
        }
    }

    @Nested
    class WithSorting {

        @BeforeEach
        void beforeEach_withSorting() {
            apiCrudServiceInMemory.initWith(List.of(API));

            var firstName = aMetadata(
                Metadata.ReferenceType.API,
                API_ID,
                "first-name",
                "aaa-first-name",
                "first-name",
                Metadata.MetadataFormat.STRING
            );
            var lastName = aMetadata(
                Metadata.ReferenceType.API,
                API_ID,
                "last-name",
                "zzz-first-name",
                "last-name",
                Metadata.MetadataFormat.STRING
            );
            var firstKey = aMetadata(
                Metadata.ReferenceType.API,
                API_ID,
                "aaa-first-key",
                "first-key",
                "first-key",
                Metadata.MetadataFormat.STRING
            );
            var lastKey = aMetadata(
                Metadata.ReferenceType.API,
                API_ID,
                "zzz-last-key",
                "last-key",
                "last-key",
                Metadata.MetadataFormat.STRING
            );
            var firstValue = aMetadata(
                Metadata.ReferenceType.ENVIRONMENT,
                ENV_ID,
                "first-value",
                "first-value",
                "aaa-first-value",
                Metadata.MetadataFormat.STRING
            );
            var lastValue = aMetadata(
                Metadata.ReferenceType.API,
                API_ID,
                "last-value",
                "last-value",
                "zzz-last-value",
                Metadata.MetadataFormat.STRING
            );
            var firstFormat = aMetadata(
                Metadata.ReferenceType.ENVIRONMENT,
                ENV_ID,
                "first-format",
                "first-format",
                "first-format",
                Metadata.MetadataFormat.BOOLEAN
            );
            var lastFormat = aMetadata(
                Metadata.ReferenceType.API,
                API_ID,
                "last-format",
                "last-format",
                "last-format",
                Metadata.MetadataFormat.URL
            );
            apiMetadataQueryService.initWith(
                List.of(firstName, lastName, firstKey, lastKey, firstValue, lastValue, firstFormat, lastFormat)
            );
        }

        @ParameterizedTest
        @MethodSource("provideParameters")
        void should_check_equality(final String sortBy, String expectedApiMetadataValue) {
            var output = getApiMetadataUseCase.execute(new GetApiMetadataUseCase.Input(API_ID, ENV_ID, null, sortBy));
            var metadata = output.metadata();

            assertThat(metadata.get(0)).hasFieldOrPropertyWithValue("key", expectedApiMetadataValue);
        }

        public static Stream<Arguments> provideParameters() {
            return Stream.of(
                Arguments.of("name", "first-name"),
                Arguments.of("-name", "last-name"),
                Arguments.of("key", "aaa-first-key"),
                Arguments.of("-key", "zzz-last-key"),
                Arguments.of("value", "first-value"),
                Arguments.of("-value", "last-value"),
                Arguments.of("format", "first-format"),
                Arguments.of("-format", "last-format"),
                Arguments.of("something-else", "aaa-first-key")
            );
        }
    }
}
