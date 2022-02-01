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
package io.gravitee.gateway.services.kubernetes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.service.AbstractService;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.handlers.api.manager.ApiManager;
import io.gravitee.kubernetes.client.KubernetesClient;
import io.gravitee.kubernetes.client.api.LabelSelector;
import io.gravitee.kubernetes.client.api.WatchQuery;
import io.gravitee.kubernetes.client.model.v1.ConfigMap;
import io.gravitee.kubernetes.client.model.v1.Event;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class KubernetesDiscoveryService extends AbstractService<KubernetesDiscoveryService> {

    private final Logger logger = LoggerFactory.getLogger(KubernetesDiscoveryService.class);

    private static final int RETRY_DELAY_MILLIS = 10000;

    @Autowired
    private KubernetesClient client;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private ApiManager manager;

    private Disposable disposable;

    @Override
    protected void doStart() throws Exception {
        startWatch();
    }

    private Flowable<Event<ConfigMap>> watch() {
        return client
            .watch(WatchQuery.configMaps().labelSelector(LabelSelector.equals("managed-by", "gravitee.io")).build())
            .observeOn(Schedulers.computation())
            .repeat()
            .retryWhen(errors -> errors.delay(RETRY_DELAY_MILLIS, TimeUnit.MILLISECONDS));
    }

    private void startWatch() {
        this.disposable =
            watch()
                .flatMapCompletable(this::handleConfigMapEvent)
                .doOnError(throwable -> logger.error("An error occurred during configmaps refresh. Restarting watch.", throwable))
                .retry()
                .subscribe();
    }

    private Completable handleConfigMapEvent(Event<ConfigMap> t) {
        ConfigMap configMap = t.getObject();

        logger.info(
            "New event {} for service {} namespace {}",
            t.getType(),
            configMap.getMetadata().getName(),
            configMap.getMetadata().getNamespace()
        );
        String definition = configMap.getData().get("definition");

        if (definition != null) {
            Api api = null;
            try {
                System.out.println(definition);
                api = mapper.readValue(definition, Api.class);
                switch (t.getType()) {
                    case "ADDED":
                    case "MODIFIED":
                        manager.register(api);
                        break;
                    case "DELETED":
                        manager.unregister(api.getId());
                }
            } catch (Exception ex) {
                logger.error("Unexpected error while trying to register service {}", (api != null) ? api.getId() : "unknown", ex);
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
