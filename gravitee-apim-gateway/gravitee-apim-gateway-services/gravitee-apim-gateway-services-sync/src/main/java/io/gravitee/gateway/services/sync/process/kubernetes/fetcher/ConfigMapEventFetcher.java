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
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.kubernetes.client.KubernetesClient;
import io.gravitee.kubernetes.client.api.LabelSelector;
import io.gravitee.kubernetes.client.api.ResourceQuery;
import io.gravitee.kubernetes.client.api.WatchQuery;
import io.gravitee.kubernetes.client.config.KubernetesConfig;
import io.gravitee.kubernetes.client.exception.ResourceVersionNotFoundException;
import io.gravitee.kubernetes.client.model.v1.ConfigMap;
import io.gravitee.kubernetes.client.model.v1.ConfigMapList;
import io.gravitee.kubernetes.client.model.v1.Event;
import io.gravitee.repository.management.model.EventType;
import io.gravitee.repository.management.model.LifecycleState;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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

    public static final String API_DEFINITIONS_KIND = "apidefinitions.gravitee.io";

    protected static final String RESOURCE_GROUP = "gravitee.io";
    protected static final String RESOURCE_VERSION = "v1alpha1";

    protected static final String DATA_ENVIRONMENT_ID = "environmentId";
    protected static final String DATA_API_DEFINITION_VERSION = "apiDefinitionVersion";
    protected static final String DATA_DEFINITION = "definition";

    private static final String LABEL_MANAGED_BY = "managed-by";
    private static final String LABEL_GIO_TYPE = "gio-type";

    private static final String EVENT_ADDED = "ADDED";
    private static final String EVENT_MODIFIED = "MODIFIED";
    private static final String EVENT_DELETED = "DELETED";

    private final KubernetesClient client;
    private final String[] namespaces;
    private final ObjectMapper objectMapper;

    private final Map<String, String> resourceVersions = new ConcurrentHashMap<>();

    public int bulkEvents() {
        return 1;
    }

    public Flowable<List<io.gravitee.repository.management.model.Event>> fetchAll(String kind) {
        return listConfigMaps(kind).flatMapMaybe(cm -> convertTo(new Event<>(EVENT_ADDED, cm))).buffer(bulkEvents());
    }

    public Flowable<List<io.gravitee.repository.management.model.Event>> fetchLatest(String kind) {
        return watchConfigMaps(kind)
            .flatMapMaybe(configMapEvent ->
                convertTo(configMapEvent)
                    .onErrorResumeNext(throwable -> {
                        log.warn("Error occurred while handling event. Ignoring.", throwable);
                        return Maybe.empty();
                    })
            )
            .buffer(bulkEvents());
    }

    private Flowable<ConfigMap> listConfigMaps(String kind) {
        List<String> namespacesAsList = getNamespacesAsList();
        if (namespacesAsList.contains("ALL")) {
            return listConfigMaps(null, kind).toFlowable().flatMap(Flowable::fromIterable);
        }
        return Flowable.fromIterable(namespacesAsList).flatMapMaybe(ns -> listConfigMaps(ns, kind)).flatMap(Flowable::fromIterable);
    }

    private Maybe<List<ConfigMap>> listConfigMaps(String namespace, String kind) {
        return client
            .get(
                ResourceQuery
                    .configMaps()
                    .namespace(namespace)
                    .labelSelector(LabelSelector.equals(LABEL_MANAGED_BY, RESOURCE_GROUP))
                    .labelSelector(LabelSelector.equals(LABEL_GIO_TYPE, kind))
                    .resourceVersion(resourceVersions.get(kind))
                    .build()
            )
            .doOnSuccess(configMapList -> {
                log.debug("caching config map resources version {} to watch from there", configMapList.getMetadata().getResourceVersion());
                resourceVersions.put(kind, configMapList.getMetadata().getResourceVersion());
            })
            .doOnError(err -> {
                log.debug("removing resource version from cache for kind {}", kind);
                resourceVersions.remove(kind);
            })
            .map(ConfigMapList::getItems);
    }

    private Flowable<Event<ConfigMap>> watchConfigMaps(String kind) {
        List<String> namespacesAsList = getNamespacesAsList();
        if (namespacesAsList.contains("ALL")) {
            return watchConfigMaps(null, kind);
        }
        return Flowable.fromIterable(namespacesAsList).flatMap(ns -> watchConfigMaps(ns, kind));
    }

    private Flowable<io.gravitee.kubernetes.client.model.v1.Event<ConfigMap>> watchConfigMaps(String namespace, String kind) {
        return client
            .watch(
                WatchQuery
                    .configMaps()
                    .namespace(namespace)
                    .labelSelector(LabelSelector.equals(LABEL_MANAGED_BY, RESOURCE_GROUP))
                    .labelSelector(LabelSelector.equals(LABEL_GIO_TYPE, kind))
                    .resourceVersion(resourceVersions.get(kind))
                    .build()
            )
            .doOnError(err -> {
                if (err instanceof ResourceVersionNotFoundException) {
                    log.debug("removing resource version from cache for kind {}", kind);
                    resourceVersions.remove(kind);
                }
            });
    }

    public Maybe<io.gravitee.repository.management.model.Event> convertTo(final Event<ConfigMap> configMapEvent) {
        ConfigMap configMap = configMapEvent.getObject();
        try {
            final String definition = configMap.getData().get(DATA_DEFINITION);

            // Extract the API definition version from the data. Consider it to be V2 to keep backward compatibility.
            final String apiDefinitionVersion = Optional
                .ofNullable(configMap.getData().get(DATA_API_DEFINITION_VERSION))
                .orElse(DefinitionVersion.V2.getLabel());

            if (definition != null) {
                String apiId;
                ApiType apiType = null;
                DefinitionVersion definitionVersion = DefinitionVersion.valueOfLabel(apiDefinitionVersion);

                if (definitionVersion == DefinitionVersion.V2) {
                    io.gravitee.definition.model.Api apiDefinition = objectMapper.readValue(
                        definition,
                        io.gravitee.definition.model.Api.class
                    );
                    apiId = apiDefinition.getId();
                    definitionVersion = apiDefinition.getDefinitionVersion();
                } else if (definitionVersion == DefinitionVersion.V4) {
                    io.gravitee.definition.model.v4.Api apiDefinition = objectMapper.readValue(
                        definition,
                        io.gravitee.definition.model.v4.Api.class
                    );
                    apiId = apiDefinition.getId();
                    apiType = apiDefinition.getType();
                    definitionVersion = apiDefinition.getDefinitionVersion();
                } else {
                    return Maybe.error(new RuntimeException("ApiDefinitionVersion is missing for this configmap: " + definition));
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

                if (apiType != null) {
                    api.setType(apiType);
                }

                switch (configMapEvent.getType()) {
                    case EVENT_ADDED, EVENT_MODIFIED:
                        event.setType(EventType.PUBLISH_API);
                        api.setLifecycleState(LifecycleState.STARTED);
                        break;
                    case EVENT_DELETED:
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
