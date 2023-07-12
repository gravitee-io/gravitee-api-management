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
package io.gravitee.gateway.services.sync.kubernetes;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.Api;
import io.gravitee.gateway.services.sync.synchronizer.ApiSynchronizer;
import io.gravitee.kubernetes.client.KubernetesClient;
import io.gravitee.kubernetes.client.model.v1.ConfigMap;
import io.gravitee.kubernetes.client.model.v1.Event;
import io.gravitee.kubernetes.client.model.v1.ObjectMeta;
import io.gravitee.kubernetes.client.model.v1.Watchable;
import io.reactivex.rxjava3.core.Flowable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class KubernetesSyncServiceTest {

    @InjectMocks
    private KubernetesSyncService cut;

    @Mock
    private KubernetesClient kubernetesClient;

    @Mock
    private ApiSynchronizer apiSynchronizer;

    @Mock
    private ObjectMapper mapper;

    @Before
    public void setUp() throws Exception {
        cut = new KubernetesSyncService(kubernetesClient, apiSynchronizer);
        cut.setMapper(mapper);
    }

    @Test
    public void watchAllNamespaces() throws Exception {
        cut.setNamespaces(new String[] { "ALL" });

        CountDownLatch latch = new CountDownLatch(3);
        when(kubernetesClient.watch(any())).thenReturn(mockFlowableEvents());

        Api api = new Api();
        api.setId(UUID.randomUUID().toString());
        when(mapper.readValue("test", Api.class)).thenReturn(api);
        when(apiSynchronizer.processApiEvents(any()))
            .then(
                (Answer<Flowable<String>>) invocation -> {
                    latch.countDown();
                    return Flowable.just(api.getId());
                }
            );

        cut.doStart();

        latch.await();
        verify(apiSynchronizer, times(3)).processApiEvents(any());
    }

    @Test
    public void watchGivenNamespaces() throws Exception {
        cut.setNamespaces(new String[] { "default", "dev" });

        CountDownLatch latch = new CountDownLatch(3);
        when(kubernetesClient.watch(any())).thenReturn(mockFlowableEvents());

        Api api = new Api();
        api.setId(UUID.randomUUID().toString());
        when(mapper.readValue("test", Api.class)).thenReturn(api);
        when(apiSynchronizer.processApiEvents(any()))
            .then(
                (Answer<Flowable<String>>) invocation -> {
                    latch.countDown();
                    return Flowable.just(api.getId());
                }
            );

        cut.doStart();

        latch.await(1000L, TimeUnit.MILLISECONDS);
        verify(apiSynchronizer, times(2)).processApiEvents(any());
    }

    private Flowable<Event<? extends Watchable>> mockFlowableEvents() {
        ObjectMeta objectMeta1 = new ObjectMeta();
        objectMeta1.setName("service1");
        objectMeta1.setNamespace("default");
        ObjectMeta objectMeta2 = new ObjectMeta();
        objectMeta2.setName("service2");
        objectMeta2.setNamespace("dev");
        ObjectMeta objectMeta3 = new ObjectMeta();
        objectMeta3.setName("service3");
        objectMeta3.setNamespace("test");

        Map<String, String> data = new HashMap<>();
        data.put(KubernetesSyncService.DATA_DEFINITION, "test");

        ConfigMap configMap1 = new ConfigMap("v1", null, data, true, "ConfigMap", objectMeta1);
        ConfigMap configMap2 = new ConfigMap("v1", null, data, true, "ConfigMap", objectMeta2);
        ConfigMap configMap3 = new ConfigMap("v1", null, data, true, "ConfigMap", objectMeta3);

        return Flowable.just(mockEvent(configMap1), mockEvent(configMap2), mockEvent(configMap3));
    }

    private Event<ConfigMap> mockEvent(ConfigMap configMap) {
        Event<ConfigMap> event = new Event<>();
        event.setType("ADD");
        event.setObject(configMap);

        return event;
    }
}
