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
package io.gravitee.apim.infra.search;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import fixtures.core.model.ApiFixtures;
import io.gravitee.apim.core.api.domain_service.ApiPathIndex;
import io.gravitee.apim.core.api.model.Path;
import io.gravitee.apim.core.search.model.IndexableApi;
import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.definition.model.v4.listener.Listener;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.rest.api.model.search.Indexable;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;

class ApiPathIndexationObserverTest {

    private final ApiPathIndex apiPathIndex = mock(ApiPathIndex.class);
    private final ApiPathIndexationObserver observer = new ApiPathIndexationObserver(apiPathIndex);

    @Test
    void onIndex_extracts_paths_and_updatedAt_from_IndexableApi_and_forwards() {
        var api = ApiFixtures.aProxyApiV4();
        var indexable = IndexableApi.builder().api(api).build();

        observer.onIndex(indexable);

        verify(apiPathIndex).index(
            api.getEnvironmentId(),
            api.getId(),
            List.of(Path.builder().path("/http_proxy/").build()),
            api.getUpdatedAt().toInstant()
        );
    }

    @Test
    void onDelete_routes_through_env_agnostic_removeForApi_so_bare_entities_work() {
        // The broadcast cron rebuilds the Indexable via reflection with only id set (see
        // SearchEngineServiceImpl.process() ACTION_DELETE branch). The observer must not depend on env/proxy/listeners
        // being populated on delete.
        var bare = new io.gravitee.rest.api.model.api.ApiEntity();
        bare.setId("api-only-id");

        observer.onDelete(bare);

        verify(apiPathIndex).removeForApi("api-only-id");
    }

    @Test
    void onDelete_for_IndexableApi_also_uses_removeForApi() {
        var api = ApiFixtures.aProxyApiV4();
        var indexable = IndexableApi.builder().api(api).build();

        observer.onDelete(indexable);

        verify(apiPathIndex).removeForApi(api.getId());
    }

    @Test
    void onDelete_for_v4_ApiEntity_also_uses_removeForApi() {
        var bare = new ApiEntity();
        bare.setId("v4-api-only-id");

        observer.onDelete(bare);

        verify(apiPathIndex).removeForApi("v4-api-only-id");
    }

    @Test
    void onIndex_extracts_paths_and_updatedAt_from_legacy_V3_ApiEntity() {
        var v3 = new io.gravitee.rest.api.model.api.ApiEntity();
        v3.setId("v3-api");
        v3.setReferenceId("env-1");
        var proxy = new Proxy();
        proxy.setVirtualHosts(List.of(new VirtualHost("/v3-path")));
        v3.setProxy(proxy);
        var t = Date.from(Instant.parse("2026-01-01T00:00:00Z"));
        v3.setUpdatedAt(t);

        observer.onIndex(v3);

        verify(apiPathIndex).index("env-1", "v3-api", List.of(Path.builder().path("/v3-path/").build()), t.toInstant());
    }

    @Test
    void onIndex_extracts_paths_and_updatedAt_from_legacy_V4_ApiEntity() {
        var v4 = new ApiEntity();
        v4.setId("v4-api");
        v4.setReferenceId("env-2");
        v4.setListeners(
            List.<Listener>of(
                HttpListener.builder()
                    .paths(List.of(io.gravitee.definition.model.v4.listener.http.Path.builder().path("/v4-path").build()))
                    .build()
            )
        );
        var t = Date.from(Instant.parse("2026-01-02T00:00:00Z"));
        v4.setUpdatedAt(t);

        observer.onIndex(v4);

        verify(apiPathIndex).index("env-2", "v4-api", List.of(Path.builder().path("/v4-path/").build()), t.toInstant());
    }

    @Test
    void onIndex_routes_empty_paths_to_remove_to_match_seeder_behavior() {
        var federated = ApiFixtures.aFederatedApi();
        var indexable = IndexableApi.builder().api(federated).build();

        observer.onIndex(indexable);

        verify(apiPathIndex).remove(federated.getEnvironmentId(), federated.getId(), federated.getUpdatedAt().toInstant());
    }

    @Test
    void onIndex_passes_null_updatedAt_when_entity_has_none() {
        var v4 = new ApiEntity();
        v4.setId("v4-api-no-updated-at");
        v4.setReferenceId("env-3");
        v4.setListeners(
            List.<Listener>of(
                HttpListener.builder()
                    .paths(List.of(io.gravitee.definition.model.v4.listener.http.Path.builder().path("/no-time").build()))
                    .build()
            )
        );
        // updatedAt left null

        observer.onIndex(v4);

        verify(apiPathIndex).index("env-3", "v4-api-no-updated-at", List.of(Path.builder().path("/no-time/").build()), null);
    }

    @Test
    void onIndex_invalidates_env_snapshot_when_index_call_throws() {
        var api = ApiFixtures.aProxyApiV4();
        var indexable = IndexableApi.builder().api(api).build();
        org.mockito.Mockito.doThrow(new IllegalStateException("boom"))
            .when(apiPathIndex)
            .index(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
            );

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> observer.onIndex(indexable));

        verify(apiPathIndex).invalidate(api.getEnvironmentId());
    }

    @Test
    void onIndex_ignores_non_api_indexables() {
        var indexable = mock(Indexable.class);

        observer.onIndex(indexable);
        observer.onDelete(indexable);

        verifyNoInteractions(apiPathIndex);
    }
}
