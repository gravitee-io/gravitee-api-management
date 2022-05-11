/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.services.sync.kubernetes;

import static io.gravitee.repository.management.model.Event.EventProperties.API_ID;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.service.AbstractService;
import io.gravitee.common.util.Maps;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.handlers.api.jackson.ApiDefinitionModule;
import io.gravitee.gateway.handlers.api.manager.ApiManager;
import io.gravitee.gateway.services.sync.synchronizer.ApiSynchronizer;
import io.gravitee.kubernetes.client.KubernetesClient;
import io.gravitee.kubernetes.client.api.LabelSelector;
import io.gravitee.kubernetes.client.api.WatchQuery;
import io.gravitee.kubernetes.client.model.v1.ConfigMap;
import io.gravitee.kubernetes.client.model.v1.Event;
import io.gravitee.repository.management.model.ApiLifecycleState;
import io.gravitee.repository.management.model.EventType;
import io.gravitee.repository.management.model.LifecycleState;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class KubernetesSyncService extends AbstractService<KubernetesSyncService> {

    protected static final String LABEL_MANAGED_BY = "managed-by";
    protected static final String LABEL_GIO_TYPE = "gio-type";
    protected static final String GRAVITEE_IO = "gravitee.io";
    protected static final String APIDEFINITIONS_TYPE = "apidefinitions.gravitee.io";
    protected static final String DATA_ENVIRONMENT_ID = "environmentId";
    protected static final String DATA_DEFINITION = "definition";
    private final Logger logger = LoggerFactory.getLogger(KubernetesSyncService.class);

    private static final int RETRY_DELAY_MILLIS = 10000;

    private final KubernetesClient client;
    private final ApiSynchronizer apiSynchronizer;
    private final ObjectMapper mapper;
    private Disposable disposable;

    public KubernetesSyncService(KubernetesClient client, ApiSynchronizer apiSynchronizer) {
        this.client = client;
        this.apiSynchronizer = apiSynchronizer;
        this.mapper = new GraviteeMapper();
        this.mapper.registerModule(new ApiDefinitionModule());
    }

    @Override
    protected void doStart() throws Exception {
        startWatch();
    }

    private void startWatch() {
        this.disposable =
            watch()
                .flatMapCompletable(this::handleConfigMapEvent)
                .doOnError(throwable -> logger.error("An error occurred during configmaps refresh. Restarting watch.", throwable))
                .retry()
                .subscribe();
    }

    private Flowable<Event<ConfigMap>> watch() {
        return client
            .watch(
                WatchQuery
                    .configMaps()
                    .labelSelector(LabelSelector.equals(LABEL_MANAGED_BY, GRAVITEE_IO))
                    .labelSelector(LabelSelector.equals(LABEL_GIO_TYPE, APIDEFINITIONS_TYPE))
                    .build()
            )
            .observeOn(Schedulers.computation())
            .repeat()
            .retryWhen(errors -> errors.delay(RETRY_DELAY_MILLIS, TimeUnit.MILLISECONDS));
    }

    private Completable handleConfigMapEvent(Event<ConfigMap> kubEvent) {
        ConfigMap configMap = kubEvent.getObject();

        logger.info(
            "New event {} for service {} namespace {}",
            kubEvent.getType(),
            configMap.getMetadata().getName(),
            configMap.getMetadata().getNamespace()
        );
        String definition = configMap.getData().get(DATA_DEFINITION);

        Api apiDefinition = null;

        if (definition != null) {
            try {
                // Need to deserialize api definition in order to recreate a regular Event which can be handled by the ApiSynchronizer.
                apiDefinition = mapper.readValue(definition, Api.class);

                final io.gravitee.repository.management.model.Event event = new io.gravitee.repository.management.model.Event();
                event.setProperties(Collections.singletonMap(API_ID.getValue(), apiDefinition.getId()));

                final io.gravitee.repository.management.model.Api api = new io.gravitee.repository.management.model.Api();
                api.setEnvironmentId(configMap.getData().get(DATA_ENVIRONMENT_ID));
                api.setDefinition(definition);
                api.setDeployedAt(apiDefinition.getDeployedAt());
                api.setEnvironmentId(apiDefinition.getEnvironmentId());
                api.setId(apiDefinition.getId());

                switch (kubEvent.getType()) {
                    case "ADDED":
                    case "MODIFIED":
                        event.setType(EventType.PUBLISH_API);
                        api.setLifecycleState(LifecycleState.STARTED);
                        break;
                    case "DELETED":
                        event.setType(EventType.UNPUBLISH_API);
                        api.setLifecycleState(LifecycleState.STOPPED);
                }

                event.setPayload(mapper.writeValueAsString(api));
                apiSynchronizer
                    .processApiEvents(Flowable.just(event))
                    .subscribe(s -> logger.info("Event processed"), t -> logger.error("An error occurred while processing event", t));
            } catch (Exception ex) {
                logger.error(
                    "Unexpected error while trying to register service {}",
                    (apiDefinition != null) ? apiDefinition.getId() : "unknown",
                    ex
                );
            }
        }

        return Completable.complete();
    }

    @Override
    protected void doStop() throws Exception {
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }
}
