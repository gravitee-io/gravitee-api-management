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
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.kubernetes.client.KubernetesClient;
import io.gravitee.kubernetes.client.config.KubernetesConfig;
import io.gravitee.kubernetes.client.exception.ResourceVersionNotFoundException;
import io.gravitee.kubernetes.client.model.v1.ConfigMap;
import io.gravitee.kubernetes.client.model.v1.ConfigMapList;
import io.gravitee.kubernetes.client.model.v1.Event;
import io.gravitee.kubernetes.client.model.v1.ListMeta;
import io.gravitee.kubernetes.client.model.v1.ObjectMeta;
import io.gravitee.kubernetes.client.model.v1.OwnerReference;
import io.gravitee.kubernetes.client.model.v1.Watchable;
import io.gravitee.repository.management.model.Api;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private static final ObjectMapper objectMapper = new GraviteeMapper();

    @Test
    @SneakyThrows
    void should_clear_cache_on_resource_version_error() {
        cut = new ConfigMapEventFetcher(kubernetesClient, new String[] { "ALL" }, objectMapper);

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
    void should_watch_all_namespaces() {
        cut = new ConfigMapEventFetcher(kubernetesClient, new String[] { "ALL" }, objectMapper);

        when(kubernetesClient.watch(argThat(query -> query.getNamespace() == null))).thenReturn(mockFlowableEvents());

        cut.fetchLatest(API_DEFINITIONS_KIND).test().assertComplete().assertValueCount(3);
    }

    @Test
    void should_watch_all_namespaces_with_all_at_any_position() {
        cut = new ConfigMapEventFetcher(kubernetesClient, new String[] { "default", "ALL" }, objectMapper);

        when(kubernetesClient.watch(argThat(query -> query.getNamespace() == null))).thenReturn(mockFlowableEvents());

        cut.fetchLatest(API_DEFINITIONS_KIND).test().assertComplete().assertValueCount(3);
    }

    @Test
    void should_watch_given_namespaces() {
        cut = new ConfigMapEventFetcher(kubernetesClient, new String[] { "default", "dev" }, objectMapper);

        doReturn(Flowable.just(createEvent(createConfigMap("service1", "default"))))
            .when(kubernetesClient)
            .watch(argThat(query -> query.getNamespace().equals("default")));

        doReturn(Flowable.just(createEvent(createConfigMap("service2", "dev"))))
            .when(kubernetesClient)
            .watch(argThat(query -> query.getNamespace().equals("dev")));

        cut.fetchLatest(API_DEFINITIONS_KIND).test().assertComplete().assertValueCount(2);
    }

    @Test
    void should_watch_current_namespace() {
        cut = new ConfigMapEventFetcher(kubernetesClient, null, objectMapper);
        KubernetesConfig.getInstance().setCurrentNamespace("current");
        ConfigMap configMap = createConfigMap("service1", "current");

        when(kubernetesClient.watch(argThat(argument -> argument.getNamespace().equals("current"))))
            .thenReturn(Flowable.just(createEvent(configMap)));

        cut.fetchLatest(API_DEFINITIONS_KIND).test().assertComplete().assertValueCount(1);
    }

    @Test
    void should_watch_current_namespace_api_v2_when_not_specified() {
        cut = new ConfigMapEventFetcher(kubernetesClient, null, objectMapper);
        KubernetesConfig.getInstance().setCurrentNamespace("current");
        ConfigMap configMap = createConfigMap("apiV2", "current");

        // If no definition version is present, consider v2 for backward-compatibility.
        configMap.getData().remove(DATA_API_DEFINITION_VERSION);

        when(kubernetesClient.watch(argThat(argument -> argument.getNamespace().equals("current"))))
            .thenReturn(Flowable.just(createEvent(configMap)));

        cut.fetchLatest(API_DEFINITIONS_KIND).test().assertComplete().assertValueCount(1);
    }

    @Test
    void should_watch_current_namespace_api_v4() {
        cut = new ConfigMapEventFetcher(kubernetesClient, null, objectMapper);
        KubernetesConfig.getInstance().setCurrentNamespace("current");
        ConfigMap configMap = createConfigMap(DefinitionVersion.V4, "apiV4", "current");

        when(kubernetesClient.watch(argThat(argument -> argument.getNamespace().equals("current"))))
            .thenReturn(Flowable.just(createEvent(configMap)));

        cut
            .fetchLatest(API_DEFINITIONS_KIND)
            .test()
            .assertComplete()
            .assertValue(events -> events.stream().anyMatch(event -> readApi(event.getPayload()).getType() == ApiType.PROXY));
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

    private Flowable<Event<? extends Watchable>> mockFlowableEvents() {
        ConfigMap configMap1 = createConfigMap("service1", "default");
        ConfigMap configMap2 = createConfigMap("service2", "dev");
        ConfigMap configMap3 = createConfigMap("service3", "test");

        return Flowable.just(createEvent(configMap1), createEvent(configMap2), createEvent(configMap3));
    }

    private static ConfigMap createConfigMap(String name, String namespace) {
        return createConfigMap(DefinitionVersion.V2, name, namespace);
    }

    private static ConfigMap createConfigMap(DefinitionVersion definitionVersion, String name, String namespace) {
        ObjectMeta objectMeta = new ObjectMeta();
        objectMeta.setName(name);
        objectMeta.setNamespace(namespace);
        OwnerReference ownerReference = new OwnerReference();
        ownerReference.setApiVersion(String.format("%s/%s", ConfigMapEventFetcher.RESOURCE_GROUP, ConfigMapEventFetcher.RESOURCE_VERSION));
        objectMeta.setOwnerReferences(List.of(ownerReference));

        Map<String, String> data = new HashMap<>();

        if (definitionVersion == DefinitionVersion.V2) {
            data.put(DATA_DEFINITION, definitionV2());
            data.put(DATA_API_DEFINITION_VERSION, DefinitionVersion.V2.getLabel());
        } else {
            data.put(DATA_DEFINITION, definitionV4());
            data.put(DATA_API_DEFINITION_VERSION, DefinitionVersion.V4.getLabel());
        }

        return new ConfigMap("v1", null, data, true, "ConfigMap", objectMeta);
    }

    private static String definitionV4() {
        return """
                {
                          "id": "d56923f4-d2e0-c56e-83cb-6670bf8759f9",
                          "definitionVersion": "4.0.0",
                          "type": "proxy",
                          "listeners": [],
                          "endpointGroups": [],
                          "plans": []
                      }
            """;
    }

    private static String definitionV2() {
        return """
                {
                          "id": "019a5f4e-5802-48d2-890a-dc24570d2e32",
                          "name": "K8s Basic Example",
                          "gravitee": "2.0.0",
                          "flow_mode": "DEFAULT",
                          "proxy": {
                              "virtual_hosts": [
                                  {
                                      "path": "/k8s-basic"
                                  }
                              ],
                              "groups": [
                                  {
                                      "endpoints": [
                                          {
                                              "name": "Default",
                                              "target": "https://api.gravitee.io/echo"
                                          }
                                      ],
                                      "load_balancing": {}
                                  }
                              ]
                          }
                      }
            """;
    }

    private static Event<ConfigMap> createEvent(ConfigMap configMap) {
        Event<ConfigMap> event = new Event<>();
        event.setType("ADDED");
        event.setObject(configMap);

        return event;
    }

    private static Api readApi(String json) {
        try {
            return objectMapper.readValue(json, Api.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
