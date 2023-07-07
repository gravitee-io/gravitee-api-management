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

import static io.gravitee.gateway.services.sync.process.kubernetes.fetcher.ConfigMapEventFetcher.DATA_DEFINITION;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.Api;
import io.gravitee.kubernetes.client.KubernetesClient;
import io.gravitee.kubernetes.client.config.KubernetesConfig;
import io.gravitee.kubernetes.client.model.v1.ConfigMap;
import io.gravitee.kubernetes.client.model.v1.Event;
import io.gravitee.kubernetes.client.model.v1.ObjectMeta;
import io.gravitee.kubernetes.client.model.v1.Watchable;
import io.reactivex.rxjava3.core.Flowable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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
class ConfigMapEventFetcherTest {

    private ConfigMapEventFetcher cut;

    @Mock
    private KubernetesClient kubernetesClient;

    @Mock
    private ObjectMapper objectMapper;

    @BeforeEach
    public void beforeEach() throws JsonProcessingException {
        Api api = new Api();
        api.setId(UUID.randomUUID().toString());
        when(objectMapper.readValue("api", Api.class)).thenReturn(api);
    }

    @Test
    void should_watch_all_namespaces() {
        cut = new ConfigMapEventFetcher(kubernetesClient, new String[] { "ALL" }, objectMapper);

        when(kubernetesClient.watch(argThat(query -> query.getNamespace() == null))).thenReturn(mockFlowableEvents());

        cut.fetchLatest().test().assertComplete().assertValueCount(3);
    }

    @Test
    void should_watch_all_namespaces_with_all_at_any_position() {
        cut = new ConfigMapEventFetcher(kubernetesClient, new String[] { "default", "ALL" }, objectMapper);

        when(kubernetesClient.watch(argThat(query -> query.getNamespace() == null))).thenReturn(mockFlowableEvents());

        cut.fetchLatest().test().assertComplete().assertValueCount(3);
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

        cut.fetchLatest().test().assertComplete().assertValueCount(2);
    }

    @Test
    void should_watch_current_namespace() {
        cut = new ConfigMapEventFetcher(kubernetesClient, null, objectMapper);
        KubernetesConfig.getInstance().setCurrentNamespace("current");
        ConfigMap configMap = createConfigMap("service1", "default");

        when(kubernetesClient.watch(argThat(argument -> argument.getNamespace().equals("current"))))
            .thenReturn(Flowable.just(createEvent(configMap)));

        cut.fetchLatest().test().assertComplete().assertValueCount(1);
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
        Map<String, String> data = new HashMap<>();
        data.put(DATA_DEFINITION, "api");
        return new ConfigMap("v1", null, data, true, "ConfigMap", objectMeta);
    }

    private Event<ConfigMap> createEvent(ConfigMap configMap) {
        Event<ConfigMap> event = new Event<>();
        event.setType("ADDED");
        event.setObject(configMap);

        return event;
    }
}
