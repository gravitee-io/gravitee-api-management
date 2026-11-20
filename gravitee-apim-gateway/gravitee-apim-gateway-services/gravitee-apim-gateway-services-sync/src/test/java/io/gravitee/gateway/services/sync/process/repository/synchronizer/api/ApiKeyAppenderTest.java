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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.gateway.services.sync.process.repository.mapper.ApiKeyMapper;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.model.ApiKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiKeyAppenderTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;

    private ApiKeyAppender cut;

    private static final int BULK_ITEMS = 100;
    private static final int CHUNK_SIZE = 3;

    @BeforeEach
    public void beforeEach() {
        cut = new ApiKeyAppender(apiKeyRepository, new ApiKeyMapper(), BULK_ITEMS, CHUNK_SIZE);
    }

    @Test
    void should_do_nothing_when_no_apikeys_for_given_deployables() {
        ApiReactorDeployable apiReactorDeployable1 = ApiReactorDeployable.builder()
            .apiId("api1")
            .reactableApi(mock(ReactableApi.class))
            .subscriptions(List.of(Subscription.builder().id("subscription1").plan("plan1").build()))
            .apiKeyPlans(Set.of("plan1"))
            .build();
        ApiReactorDeployable apiReactorDeployable2 = ApiReactorDeployable.builder()
            .apiId("api2")
            .reactableApi(mock(ReactableApi.class))
            .subscriptions(List.of(Subscription.builder().id("subscription2").plan("plan2").build()))
            .build();
        List<ApiReactorDeployable> appends = cut.appends(true, List.of(apiReactorDeployable1, apiReactorDeployable2), Set.of("env"));
        assertThat(appends).hasSize(2);
        assertThat(appends.get(0).apiKeys()).isEmpty();
        assertThat(appends.get(1).apiKeys()).isEmpty();
    }

    @Test
    void should_appends_apikeys_for_given_deployable() throws TechnicalException {
        ApiReactorDeployable apiReactorDeployable1 = ApiReactorDeployable.builder()
            .apiId("api1")
            .reactableApi(mock(ReactableApi.class))
            .subscriptions(
                List.of(
                    Subscription.builder().id("subscription1").api("api1").plan("plan1").build(),
                    Subscription.builder().id("subscription2").api("api1").plan("plan1").build()
                )
            )
            .apiKeyPlans(Set.of("plan1"))
            .build();
        ApiKey e1 = new ApiKey();
        e1.setId("k1");
        e1.setSubscriptions(List.of("subscription1"));
        ApiKey e2 = new ApiKey();
        e2.setId("k2");
        e2.setSubscriptions(List.of("subscription1"));
        when(
            apiKeyRepository.searchAfter(
                argThat(argument -> argument.getEnvironments().equals(Set.of("env"))),
                any(),
                isNull(),
                eq(BULK_ITEMS)
            )
        ).thenReturn(List.of(e1, e2));
        ApiReactorDeployable apiReactorDeployable2 = ApiReactorDeployable.builder()
            .apiId("api2")
            .reactableApi(mock(ReactableApi.class))
            .subscriptions(List.of(Subscription.builder().id("subscription2").api("api2").plan("noapikeyplan").build()))
            .build();
        List<ApiReactorDeployable> deployables = cut.appends(true, List.of(apiReactorDeployable1, apiReactorDeployable2), Set.of("env"));
        assertThat(deployables).hasSize(2);
        assertThat(deployables.get(0).apiKeys()).hasSize(2);
        assertThat(deployables.get(1).apiKeys()).isEmpty();
    }

    @Test
    void should_chunk_subscriptions_in_list_and_dedup_federated_keys_across_chunks() throws TechnicalException {
        // 5 subscription ids → 2 chunks of size 3 (last chunk has 2). A federated api key tied to
        // subscriptions across both chunks must be emitted exactly once.
        List<Subscription> subs = IntStream.range(0, 5)
            .mapToObj(i -> Subscription.builder().id("sub" + i).api("api1").plan("plan1").build())
            .toList();
        ApiReactorDeployable deployable = ApiReactorDeployable.builder()
            .apiId("api1")
            .reactableApi(mock(ReactableApi.class))
            .subscriptions(new ArrayList<>(subs))
            .apiKeyPlans(Set.of("plan1"))
            .build();

        // Federated key matches subscriptions in BOTH chunks (sub0 in chunk 0, sub3 in chunk 1).
        ApiKey federated = new ApiKey();
        federated.setId("federated-key");
        federated.setSubscriptions(List.of("sub0", "sub3"));
        // Chunk-local key matches only chunk 1 (sub4).
        ApiKey chunk1Only = new ApiKey();
        chunk1Only.setId("k-chunk1");
        chunk1Only.setSubscriptions(List.of("sub4"));

        when(
            apiKeyRepository.searchAfter(
                argThat(c -> c != null && c.getSubscriptions() != null && c.getSubscriptions().contains("sub0")),
                any(),
                isNull(),
                eq(BULK_ITEMS)
            )
        ).thenReturn(List.of(federated));
        when(
            apiKeyRepository.searchAfter(
                argThat(c -> c != null && c.getSubscriptions() != null && c.getSubscriptions().contains("sub3")),
                any(),
                isNull(),
                eq(BULK_ITEMS)
            )
        ).thenReturn(List.of(federated, chunk1Only));

        List<ApiReactorDeployable> result = cut.appends(true, List.of(deployable), Set.of("env"));

        // The federated key record is returned by both chunks. Without dedup the record would be
        // processed twice → 4 federated entries (2 subs × 2 chunks) + 1 chunk1Only = 5 entries.
        // With dedup the record is processed once → 2 federated entries (its 2 subs) + 1 chunk1Only.
        assertThat(result.get(0).apiKeys())
            .extracting(io.gravitee.gateway.api.service.ApiKey::getId)
            .containsExactlyInAnyOrder("federated-key", "federated-key", "k-chunk1");
        // Per-chunk page returns < BULK_ITEMS so no cursor advance occurs within a chunk.
        verify(apiKeyRepository, never()).searchAfter(any(), any(), argThat(cursor -> cursor != null), any(int.class));
    }

    @Test
    void should_advance_cursor_when_chunk_page_is_full() throws TechnicalException {
        // Single chunk (3 subs, chunk size 3). Two pages of BULK_ITEMS each, then a short page.
        // Tests that the cursor advances within a chunk's pagination loop.
        ApiKeyAppender smallBulkCut = new ApiKeyAppender(apiKeyRepository, new ApiKeyMapper(), 2, CHUNK_SIZE);

        ApiReactorDeployable deployable = ApiReactorDeployable.builder()
            .apiId("api1")
            .reactableApi(mock(ReactableApi.class))
            .subscriptions(
                List.of(
                    Subscription.builder().id("subA").api("api1").plan("plan1").build(),
                    Subscription.builder().id("subB").api("api1").plan("plan1").build(),
                    Subscription.builder().id("subC").api("api1").plan("plan1").build()
                )
            )
            .apiKeyPlans(Set.of("plan1"))
            .build();

        ApiKey k1 = new ApiKey();
        k1.setId("k1");
        k1.setSubscriptions(List.of("subA"));
        ApiKey k2 = new ApiKey();
        k2.setId("k2");
        k2.setSubscriptions(List.of("subB"));
        ApiKey k3 = new ApiKey();
        k3.setId("k3");
        k3.setSubscriptions(List.of("subC"));

        when(apiKeyRepository.searchAfter(any(), any(), isNull(), eq(2))).thenReturn(List.of(k1, k2));
        when(apiKeyRepository.searchAfter(any(), any(), argThat(cursor -> cursor != null && cursor.id().equals("k2")), eq(2))).thenReturn(
            List.of(k3)
        );

        List<ApiReactorDeployable> result = smallBulkCut.appends(true, List.of(deployable), Set.of("env"));
        assertThat(result.get(0).apiKeys())
            .extracting(io.gravitee.gateway.api.service.ApiKey::getId)
            .containsExactlyInAnyOrder("k1", "k2", "k3");
    }

    @Test
    void should_drop_the_inactive_mapping_of_a_key_on_an_api_where_the_key_has_an_active_one() throws TechnicalException {
        ApiReactorDeployable deployable = deployable(
            "api1",
            subscription("accepted-sub", "api1", "ACCEPTED"),
            subscription("closed-sub", "api1", "CLOSED")
        );
        givenApiKeys(apiKeyRecord("k1", "shared-key", false, "accepted-sub", "closed-sub"));

        List<ApiReactorDeployable> result = cut.appends(false, List.of(deployable), Set.of("env"));

        assertThat(result.getFirst().apiKeys())
            .singleElement()
            .satisfies(apiKey -> {
                assertThat(apiKey.getId()).isEqualTo("k1");
                assertThat(apiKey.getSubscription()).isEqualTo("accepted-sub");
                assertThat(apiKey.isActive()).isTrue();
            });
    }

    @Test
    void should_keep_the_inactive_mapping_of_a_key_on_an_api_where_the_key_has_no_active_one() throws TechnicalException {
        ApiReactorDeployable api1 = deployable("api1", subscription("accepted-sub", "api1", "ACCEPTED"));
        ApiReactorDeployable api2 = deployable("api2", subscription("closed-sub", "api2", "CLOSED"));
        givenApiKeys(apiKeyRecord("k1", "shared-key", false, "accepted-sub", "closed-sub"));

        List<ApiReactorDeployable> result = cut.appends(false, List.of(api1, api2), Set.of("env"));

        assertThat(result.get(0).apiKeys())
            .singleElement()
            .satisfies(apiKey -> assertThat(apiKey.isActive()).isTrue());
        assertThat(result.get(1).apiKeys())
            .singleElement()
            .satisfies(apiKey -> {
                assertThat(apiKey.getSubscription()).isEqualTo("closed-sub");
                assertThat(apiKey.isActive()).isFalse();
            });
    }

    @Test
    void should_keep_every_mapping_of_a_revoked_key() throws TechnicalException {
        ApiReactorDeployable deployable = deployable(
            "api1",
            subscription("sub-a", "api1", "ACCEPTED"),
            subscription("sub-b", "api1", "ACCEPTED")
        );
        givenApiKeys(apiKeyRecord("k1", "shared-key", true, "sub-a", "sub-b"));

        List<ApiReactorDeployable> result = cut.appends(false, List.of(deployable), Set.of("env"));

        assertThat(result.getFirst().apiKeys())
            .extracting(io.gravitee.gateway.api.service.ApiKey::getSubscription, io.gravitee.gateway.api.service.ApiKey::isActive)
            .containsExactlyInAnyOrder(tuple("sub-a", false), tuple("sub-b", false));
    }

    @Test
    void should_keep_the_mappings_of_another_key_with_the_same_value() throws TechnicalException {
        ApiReactorDeployable deployable = deployable("api1", subscription("accepted-sub", "api1", "ACCEPTED"));
        givenApiKeys(
            apiKeyRecord("active-key", "my-key", false, "accepted-sub"),
            apiKeyRecord("revoked-key", "my-key", true, "accepted-sub")
        );

        List<ApiReactorDeployable> result = cut.appends(false, List.of(deployable), Set.of("env"));

        assertThat(result.getFirst().apiKeys())
            .extracting(io.gravitee.gateway.api.service.ApiKey::getId, io.gravitee.gateway.api.service.ApiKey::isActive)
            .containsExactlyInAnyOrder(tuple("active-key", true), tuple("revoked-key", false));
    }

    private void givenApiKeys(ApiKey... apiKeys) throws TechnicalException {
        when(apiKeyRepository.searchAfter(any(), any(), any(), any(int.class))).thenReturn(List.of(apiKeys));
    }

    private static ApiReactorDeployable deployable(String apiId, Subscription... subscriptions) {
        return ApiReactorDeployable.builder()
            .apiId(apiId)
            .reactableApi(mock(ReactableApi.class))
            .subscriptions(List.of(subscriptions))
            .apiKeyPlans(Set.of("plan1"))
            .build();
    }

    private static Subscription subscription(String id, String api, String status) {
        return Subscription.builder().id(id).api(api).plan("plan1").status(status).build();
    }

    private static ApiKey apiKeyRecord(String id, String value, boolean revoked, String... subscriptions) {
        ApiKey apiKey = new ApiKey();
        apiKey.setId(id);
        apiKey.setKey(value);
        apiKey.setRevoked(revoked);
        apiKey.setSubscriptions(List.of(subscriptions));
        return apiKey;
    }
}
