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
package io.gravitee.gateway.services.sync.process.kubernetes.fetcher;

import static io.gravitee.gateway.services.sync.process.kubernetes.fetcher.ConfigMapEventFetcher.API_DEFINITIONS_KIND;
import static io.gravitee.gateway.services.sync.process.kubernetes.fetcher.ConfigMapEventFetcher.DATA_API_DEFINITION_VERSION;
import static io.gravitee.gateway.services.sync.process.kubernetes.fetcher.ConfigMapEventFetcher.DATA_DEFINITION;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.kubernetes.client.KubernetesClient;
import io.gravitee.kubernetes.client.api.ResourceQuery;
import io.gravitee.kubernetes.client.config.KubernetesConfig;
import io.gravitee.kubernetes.client.exception.ResourceVersionNotFoundException;
import io.gravitee.kubernetes.client.model.v1.ConfigMap;
import io.gravitee.kubernetes.client.model.v1.ConfigMapList;
import io.gravitee.kubernetes.client.model.v1.Event;
import io.gravitee.kubernetes.client.model.v1.ListMeta;
import io.gravitee.kubernetes.client.model.v1.ObjectMeta;
import io.gravitee.kubernetes.client.model.v1.OwnerReference;
import io.gravitee.kubernetes.client.model.v1.Watchable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.SneakyThrows;
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
class ConfigMapEventFetcherTest {

    private ConfigMapEventFetcher cut;

    @Mock
    private KubernetesClient kubernetesClient;

    @Mock
    private ObjectMapper objectMapper;

    @Test
    @SneakyThrows
    void should_clear_cache_on_resource_version_error() {
        cut = new ConfigMapEventFetcher(kubernetesClient, new String[] { "ALL" }, objectMapper);

        when(objectMapper.readValue("api", Api.class)).thenReturn(mockApiV2());

        when(kubernetesClient.get(any()))
            .thenReturn(
                Maybe.just(
                    new ConfigMapList(
                        "v1",
                        List.of(createConfigMap("service1", "default")),
                        "configmaps",
                        new ListMeta(null, null, "12345", null)
                    )
                )
            )
            .thenReturn(
                Maybe.just(
                    new ConfigMapList(
                        "v1",
                        List.of(createConfigMap("service1", "default")),
                        "configmaps",
                        new ListMeta(null, null, "54321", null)
                    )
                )
            )
            .thenReturn(Maybe.error(new ResourceVersionNotFoundException("54321")))
            .thenReturn(
                Maybe.just(
                    new ConfigMapList(
                        "v1",
                        List.of(createConfigMap("service2", "default")),
                        "configmaps",
                        new ListMeta(null, null, "56789", null)
                    )
                )
            );

        cut.fetchAll(API_DEFINITIONS_KIND).test().await().assertValueCount(1);
        cut.fetchAll(API_DEFINITIONS_KIND).test().await().assertValueCount(1);
        cut.fetchAll(API_DEFINITIONS_KIND).test().await().assertFailure(ResourceVersionNotFoundException.class);
        cut.fetchAll(API_DEFINITIONS_KIND).test().await().assertValueCount(1);

        var inOrder = inOrder(kubernetesClient);

        inOrder.verify(kubernetesClient).get(argThat(query -> query.getResourceVersion() == null));
        inOrder.verify(kubernetesClient).get(argThat(query -> "12345".equals(query.getResourceVersion())));
        inOrder.verify(kubernetesClient).get(argThat(query -> query.getResourceVersion() == null));
    }

    @Test
    void should_watch_all_namespaces() throws JsonProcessingException {
        cut = new ConfigMapEventFetcher(kubernetesClient, new String[] { "ALL" }, objectMapper);

        when(kubernetesClient.watch(argThat(query -> query.getNamespace() == null))).thenReturn(mockFlowableEvents());
        when(objectMapper.readValue("api", Api.class)).thenReturn(mockApiV2());

        cut.fetchLatest(API_DEFINITIONS_KIND).test().assertComplete().assertValueCount(3);
    }

    @Test
    void should_watch_all_namespaces_with_all_at_any_position() throws JsonProcessingException {
        cut = new ConfigMapEventFetcher(kubernetesClient, new String[] { "default", "ALL" }, objectMapper);

        when(kubernetesClient.watch(argThat(query -> query.getNamespace() == null))).thenReturn(mockFlowableEvents());
        when(objectMapper.readValue("api", Api.class)).thenReturn(mockApiV2());

        cut.fetchLatest(API_DEFINITIONS_KIND).test().assertComplete().assertValueCount(3);
    }

    @Test
    void should_watch_given_namespaces() throws JsonProcessingException {
        cut = new ConfigMapEventFetcher(kubernetesClient, new String[] { "default", "dev" }, objectMapper);

        doReturn(Flowable.just(createEvent(createConfigMap("service1", "default"))))
            .when(kubernetesClient)
            .watch(argThat(query -> query.getNamespace().equals("default")));

        doReturn(Flowable.just(createEvent(createConfigMap("service2", "dev"))))
            .when(kubernetesClient)
            .watch(argThat(query -> query.getNamespace().equals("dev")));

        when(objectMapper.readValue("api", Api.class)).thenReturn(mockApiV2());

        cut.fetchLatest(API_DEFINITIONS_KIND).test().assertComplete().assertValueCount(2);
    }

    @Test
    void should_watch_current_namespace() throws JsonProcessingException {
        cut = new ConfigMapEventFetcher(kubernetesClient, null, objectMapper);
        KubernetesConfig.getInstance().setCurrentNamespace("current");
        ConfigMap configMap = createConfigMap("service1", "current");

        when(kubernetesClient.watch(argThat(argument -> argument.getNamespace().equals("current"))))
            .thenReturn(Flowable.just(createEvent(configMap)));
        when(objectMapper.readValue("api", Api.class)).thenReturn(mockApiV2());

        cut.fetchLatest(API_DEFINITIONS_KIND).test().assertComplete().assertValueCount(1);
    }

    @Test
    void should_watch_current_namespace_api_v2_when_not_specified() throws JsonProcessingException {
        cut = new ConfigMapEventFetcher(kubernetesClient, null, objectMapper);
        KubernetesConfig.getInstance().setCurrentNamespace("current");
        ConfigMap configMap = createConfigMap("apiV2", "current");

        // If no definition version is present, consider v2 for backward-compatibility.
        configMap.getData().remove(DATA_API_DEFINITION_VERSION);

        when(kubernetesClient.watch(argThat(argument -> argument.getNamespace().equals("current"))))
            .thenReturn(Flowable.just(createEvent(configMap)));
        when(objectMapper.readValue("api", Api.class)).thenReturn(mockApiV2());

        cut.fetchLatest(API_DEFINITIONS_KIND).test().assertComplete().assertValueCount(1);
    }

    @Test
    void should_watch_current_namespace_api_v4() throws JsonProcessingException {
        cut = new ConfigMapEventFetcher(kubernetesClient, null, objectMapper);
        KubernetesConfig.getInstance().setCurrentNamespace("current");
        ConfigMap configMap = createConfigMap("apiV4", "current");

        configMap.getData().put(DATA_API_DEFINITION_VERSION, DefinitionVersion.V4.getLabel());

        when(kubernetesClient.watch(argThat(argument -> argument.getNamespace().equals("current"))))
            .thenReturn(Flowable.just(createEvent(configMap)));

        when(objectMapper.readValue("api", io.gravitee.definition.model.v4.Api.class)).thenReturn(mockApiV4());

        cut.fetchLatest(API_DEFINITIONS_KIND).test().assertComplete().assertValueCount(1);
    }

    @Test
    void should_fail_watch_current_namespace_when_api_definition_version_is_unknown() {
        cut = new ConfigMapEventFetcher(kubernetesClient, null, objectMapper);
        KubernetesConfig.getInstance().setCurrentNamespace("current");
        ConfigMap configMap = createConfigMap("apiV4", "current");

        configMap.getData().put(DATA_API_DEFINITION_VERSION, "unknown");

        when(kubernetesClient.watch(argThat(argument -> argument.getNamespace().equals("current"))))
            .thenReturn(Flowable.just(createEvent(configMap)));

        cut.fetchLatest(API_DEFINITIONS_KIND).test().assertNoValues();
    }

    @Test
    void should_not_convert_to_event() throws JsonProcessingException {
        cut = new ConfigMapEventFetcher(kubernetesClient, null, objectMapper);
        KubernetesConfig.getInstance().setCurrentNamespace("current");
        ConfigMap configMap = createConfigMap("apiV4", "current");
        configMap.getMetadata().setOwnerReferences(null);

        when(kubernetesClient.watch(argThat(argument -> argument.getNamespace().equals("current"))))
            .thenReturn(Flowable.just(createEvent(configMap)));

        cut.fetchLatest(API_DEFINITIONS_KIND).test().assertComplete().assertValueCount(0);
    }

    private Api mockApiV2() {
        Api api = new Api();
        api.setId(UUID.randomUUID().toString());
        return api;
    }

    public io.gravitee.definition.model.v4.Api mockApiV4() {
        io.gravitee.definition.model.v4.Api api = new io.gravitee.definition.model.v4.Api();
        api.setId(UUID.randomUUID().toString());

        return api;
    }

    private Flowable<Event<? extends Watchable>> mockFlowableEvents() {
        ConfigMap configMap1 = createConfigMap("service1", "default");
        ConfigMap configMap2 = createConfigMap("service2", "dev");
        ConfigMap configMap3 = createConfigMap("service3", "test");

        return Flowable.just(createEvent(configMap1), createEvent(configMap2), createEvent(configMap3));
    }

    private ConfigMap createConfigMap(final String name, final String namespace) {
        ObjectMeta objectMeta = new ObjectMeta();
        objectMeta.setName(name);
        objectMeta.setNamespace(namespace);
        OwnerReference ownerReference = new OwnerReference();
        ownerReference.setApiVersion(String.format("%s/%s", ConfigMapEventFetcher.RESOURCE_GROUP, ConfigMapEventFetcher.RESOURCE_VERSION));
        objectMeta.setOwnerReferences(List.of(ownerReference));

        Map<String, String> data = new HashMap<>();

        data.put(DATA_DEFINITION, "api");

        data.put(DATA_API_DEFINITION_VERSION, DefinitionVersion.V2.getLabel());

        return new ConfigMap("v1", null, data, true, "ConfigMap", objectMeta);
    }

    private Event<ConfigMap> createEvent(ConfigMap configMap) {
        Event<ConfigMap> event = new Event<>();
        event.setType("ADDED");
        event.setObject(configMap);

        return event;
    }
}
