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
package io.gravitee.gateway.services.sync.process.local.synchronizer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.api.service.ApiKeyService;
import io.gravitee.gateway.api.service.SubscriptionService;
import io.gravitee.gateway.handlers.api.manager.ApiManager;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.gateway.services.sync.process.common.mapper.SubscriptionMapper;
import io.gravitee.gateway.services.sync.process.local.mapper.ApiKeyMapper;
import io.gravitee.gateway.services.sync.process.local.mapper.ApiMapper;
import io.gravitee.gateway.services.sync.process.repository.service.EnvironmentService;
import io.gravitee.repository.management.model.Event;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ThreadPoolExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LocalApiSynchronizerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void folds_pure_json_payload_and_nested_definition_into_strings() throws Exception {
        JsonNode root = mapper.readTree(
            """
            { "apiEvent": { "id": "x", "type": "PUBLISH_API",
              "payload": { "id": "x", "type": "agent",
                "definition": { "kind": "standalone", "id": "x" } } } }
            """
        );

        JsonNode folded = LocalApiSynchronizer.foldEmbeddedJson(mapper, root);

        // payload is now an (escaped) JSON string, as the repository Event.payload (String) expects
        JsonNode payloadNode = folded.path("apiEvent").path("payload");
        assertThat(payloadNode.isTextual()).isTrue();

        // ... and the nested definition inside it is also a string (repository Api.definition is String)
        JsonNode payload = mapper.readTree(payloadNode.asText());
        assertThat(payload.path("definition").isTextual()).isTrue();
        assertThat(mapper.readTree(payload.path("definition").asText()).path("kind").asText()).isEqualTo("standalone");
    }

    @Test
    void leaves_a_legacy_escaped_string_payload_untouched() throws Exception {
        String legacy = "{\"apiEvent\":{\"id\":\"x\",\"type\":\"PUBLISH_API\",\"payload\":\"{\\\"id\\\":\\\"x\\\"}\"}}";
        JsonNode root = mapper.readTree(legacy);

        JsonNode folded = LocalApiSynchronizer.foldEmbeddedJson(mapper, root);

        assertThat(folded.path("apiEvent").path("payload").asText()).isEqualTo("{\"id\":\"x\"}");
    }

    @Nested
    @DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
    class Synchronize {

        @TempDir
        Path registry;

        private final ApiManager apiManager = mock(ApiManager.class);
        private final ApiMapper apiMapper = mock(ApiMapper.class);

        private LocalApiSynchronizer synchronizer;

        @BeforeEach
        void setUp() {
            synchronizer = new LocalApiSynchronizer(
                mock(ApiKeyMapper.class),
                mock(ApiKeyService.class),
                apiManager,
                apiMapper,
                mock(EnvironmentService.class),
                mapper,
                mock(SubscriptionMapper.class),
                mock(SubscriptionService.class),
                mock(ThreadPoolExecutor.class)
            );
        }

        @Test
        void deploys_and_registers_each_api_file() throws Exception {
            writeApiFile("a.json", "a");
            ReactableApi<?> reactable = reactable("a");
            doReturn(reactable).when(apiMapper).to(any());
            when(apiManager.register(reactable)).thenReturn(true);

            synchronizer.synchronize(registry.toFile()).test().assertComplete().assertNoErrors();

            verify(apiManager).register(reactable);
        }

        @Test
        void ignores_an_already_registered_api_without_throwing() throws Exception {
            writeApiFile("a.json", "a");
            ReactableApi<?> reactable = reactable("a");
            doReturn(reactable).when(apiMapper).to(any());
            when(apiManager.register(reactable)).thenReturn(false); // already registered

            // The previous behaviour threw IllegalStateException("Error during registration"); now it is a no-op skip.
            synchronizer.synchronize(registry.toFile()).test().assertComplete().assertNoErrors();

            verify(apiManager).register(reactable);
        }

        @Test
        void registers_the_remaining_apis_when_one_is_already_registered() throws Exception {
            writeApiFile("a.json", "a");
            writeApiFile("b.json", "b");
            ReactableApi<?> a = reactable("a");
            ReactableApi<?> b = reactable("b");
            doReturn(a).when(apiMapper).to(argThat(e -> e != null && "a".equals(e.getId())));
            doReturn(b).when(apiMapper).to(argThat(e -> e != null && "b".equals(e.getId())));
            when(apiManager.register(a)).thenReturn(false); // already registered
            when(apiManager.register(b)).thenReturn(true);

            synchronizer.synchronize(registry.toFile()).test().assertComplete().assertNoErrors();

            verify(apiManager).register(a);
            verify(apiManager).register(b);
        }

        @Test
        void completes_without_error_on_a_malformed_file() throws Exception {
            Files.writeString(registry.resolve("broken.json"), "{ this is not json");

            // A single bad file must not fail the whole synchronization (no propagated error → no LocalSyncManager restart).
            synchronizer.synchronize(registry.toFile()).test().assertComplete().assertNoErrors();

            verify(apiManager, never()).register(any());
        }

        @Test
        void skips_a_file_without_an_api_event() throws Exception {
            Files.writeString(registry.resolve("empty.json"), "{}");

            synchronizer.synchronize(registry.toFile()).test().assertComplete().assertNoErrors();

            verify(apiManager, never()).register(any());
        }

        private void writeApiFile(String name, String id) throws Exception {
            // Pure-JSON local-sync envelope (foldEmbeddedJson turns payload into the string the repository Event expects).
            Files.writeString(
                registry.resolve(name),
                "{\"apiEvent\":{\"id\":\"" + id + "\",\"type\":\"PUBLISH_API\",\"payload\":{\"id\":\"" + id + "\"}}}"
            );
        }

        private ReactableApi<?> reactable(String id) {
            ReactableApi<?> reactable = mock(ReactableApi.class);
            lenient().when(reactable.getId()).thenReturn(id);
            return reactable;
        }
    }
}
