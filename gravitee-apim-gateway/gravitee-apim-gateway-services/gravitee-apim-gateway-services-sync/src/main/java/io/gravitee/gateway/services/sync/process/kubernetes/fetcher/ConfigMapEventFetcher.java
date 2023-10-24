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

import static io.gravitee.repository.management.model.Event.EventProperties.API_ID;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.kubernetes.client.KubernetesClient;
import io.gravitee.kubernetes.client.api.LabelSelector;
import io.gravitee.kubernetes.client.api.WatchQuery;
import io.gravitee.kubernetes.client.config.KubernetesConfig;
import io.gravitee.kubernetes.client.model.v1.ConfigMap;
import io.gravitee.kubernetes.client.model.v1.Event;
import io.gravitee.kubernetes.client.model.v1.OwnerReference;
import io.gravitee.repository.management.model.EventType;
import io.gravitee.repository.management.model.LifecycleState;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import java.util.*;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@RequiredArgsConstructor
public class ConfigMapEventFetcher {

    private static final String LABEL_MANAGED_BY = "managed-by";
    private static final String LABEL_GIO_TYPE = "gio-type";
    protected static final String GRAVITEE_IO = "gravitee.io";
    protected static final String API_DEFINITION_V1_ALPHA1 = "v1alpha1";
    protected static final String APIDEFINITIONS_TYPE = "apidefinitions.gravitee.io";
    protected static final String DATA_ENVIRONMENT_ID = "environmentId";
    protected static final String DATA_DEFINITION = "definition";
    private static final int RETRY_DELAY_MILLIS = 10000;
    private final KubernetesClient client;
    private final String[] namespaces;
    private final ObjectMapper objectMapper;

    public int bulkEvents() {
        return 1;
    }

    public Flowable<List<io.gravitee.repository.management.model.Event>> fetchLatest() {
        return watchConfigMaps().flatMapMaybe(this::convertTo).buffer(bulkEvents());
    }

    private Flowable<Event<ConfigMap>> watchConfigMaps() {
        List<String> namespacesAsList = getNamespacesAsList();
        if (namespacesAsList.contains("ALL")) {
            return watchConfigMaps(null);
        }
        return Flowable.fromIterable(namespacesAsList).flatMap(this::watchConfigMaps);
    }

    private Flowable<io.gravitee.kubernetes.client.model.v1.Event<ConfigMap>> watchConfigMaps(final String namespace) {
        return client
            .watch(
                WatchQuery
                    .configMaps()
                    .namespace(namespace)
                    .labelSelector(LabelSelector.equals(LABEL_MANAGED_BY, GRAVITEE_IO))
                    .labelSelector(LabelSelector.equals(LABEL_GIO_TYPE, APIDEFINITIONS_TYPE))
                    .build()
            )
            .retryWhen(errors -> errors.delay(RETRY_DELAY_MILLIS, TimeUnit.MILLISECONDS));
    }

    public Maybe<io.gravitee.repository.management.model.Event> convertTo(final Event<ConfigMap> configMapEvent) {
        ConfigMap configMap = configMapEvent.getObject();
        try {
            String definition = configMap.getData().get(DATA_DEFINITION);
            if (definition != null && configMap.getMetadata().getOwnerReferences() != null) {
                Optional<OwnerReference> graviteeOwnerReference = configMap
                    .getMetadata()
                    .getOwnerReferences()
                    .stream()
                    .filter(ownerReference -> ownerReference.getApiVersion().startsWith(GRAVITEE_IO))
                    .findFirst();

                String apiId;
                DefinitionVersion definitionVersion;
                if (graviteeOwnerReference.isPresent()) {
                    OwnerReference ownerReference = graviteeOwnerReference.get();
                    if (ownerReference.getApiVersion().endsWith(API_DEFINITION_V1_ALPHA1)) {
                        io.gravitee.definition.model.Api apiDefinition = objectMapper.readValue(
                            definition,
                            io.gravitee.definition.model.Api.class
                        );
                        apiId = apiDefinition.getId();
                        definitionVersion = apiDefinition.getDefinitionVersion();
                    } else {
                        io.gravitee.definition.model.v4.Api apiDefinition = objectMapper.readValue(
                            definition,
                            io.gravitee.definition.model.v4.Api.class
                        );
                        apiId = apiDefinition.getId();
                        definitionVersion = apiDefinition.getDefinitionVersion();
                    }
                } else {
                    throw new RuntimeException("GraviteeOwnerReference is missing for this configmap. Unable to process this event");
                }
                // Need to deserialize api definition in order to recreate a regular Event which can be handled by the ApiSynchronizer.
                final io.gravitee.repository.management.model.Event event = new io.gravitee.repository.management.model.Event();
                event.setProperties(Collections.singletonMap(API_ID.getValue(), apiId));
                event.setCreatedAt(new Date());

                final io.gravitee.repository.management.model.Api api = new io.gravitee.repository.management.model.Api();
                api.setEnvironmentId(configMap.getData().get(DATA_ENVIRONMENT_ID));
                api.setDefinition(definition);
                api.setDefinitionVersion(definitionVersion);
                api.setId(apiId);

                switch (configMapEvent.getType()) {
                    case "ADDED":
                    case "MODIFIED":
                        event.setType(EventType.PUBLISH_API);
                        api.setLifecycleState(LifecycleState.STARTED);
                        break;
                    case "DELETED":
                        event.setType(EventType.UNPUBLISH_API);
                        api.setLifecycleState(LifecycleState.STOPPED);
                        break;
                    default:
                        log.error("Unsupported configMap event type {}.", configMapEvent.getType());
                }

                event.setPayload(objectMapper.writeValueAsString(api));
                return Maybe.just(event);
            }
        } catch (Exception ex) {
            // Log the error and ignore this event.
            log.error("Unable to extract api definition from config map.", ex);
        }
        return Maybe.empty();
    }

    private List<String> getNamespacesAsList() {
        if (namespaces == null || namespaces.length == 0) {
            // By default, we will only watch configmaps in the current namespace
            return List.of(KubernetesConfig.getInstance().getCurrentNamespace());
        }
        return Arrays.asList(namespaces);
    }
}
