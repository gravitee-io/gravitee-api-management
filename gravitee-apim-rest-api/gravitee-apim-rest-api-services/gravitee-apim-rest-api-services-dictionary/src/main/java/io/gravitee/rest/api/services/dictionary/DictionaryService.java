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
package io.gravitee.rest.api.services.dictionary;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.event.Event;
import io.gravitee.common.event.EventListener;
import io.gravitee.common.event.EventManager;
import io.gravitee.common.service.AbstractService;
import io.gravitee.node.api.Node;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryEntity;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryProviderEntity;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryTriggerEntity;
import io.gravitee.rest.api.service.HttpClientService;
import io.gravitee.rest.api.service.event.DictionaryEvent;
import io.gravitee.rest.api.services.dictionary.provider.http.HttpProvider;
import io.gravitee.rest.api.services.dictionary.provider.http.configuration.HttpProviderConfiguration;
import io.vertx.core.Vertx;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class DictionaryService extends AbstractService implements EventListener<DictionaryEvent, DictionaryEntity> {

    private static final String DICTIONARY_HTTP_PROVIDER = "HTTP";

    @Autowired
    private EventManager eventManager;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private io.gravitee.rest.api.service.configuration.dictionary.DictionaryService dictionaryService;

    @Autowired
    private HttpClientService httpClientService;

    @Autowired
    private Vertx vertx;

    @Autowired
    private Node node;

    private final Map<String, Long> timers = new HashMap<>();

    @Override
    protected String name() {
        return "Dictionary Service";
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        eventManager.subscribeForEvents(this, DictionaryEvent.class);
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
    }

    @Override
    public void onEvent(Event<DictionaryEvent, DictionaryEntity> event) {
        final DictionaryEntity dictionary = event.content();

        switch (event.type()) {
            case START:
                startDynamicDictionary(dictionary);
                break;
            case STOP:
                stopDynamicDictionary(dictionary);
                break;
            case RESTART:
                stopDynamicDictionary(dictionary);
                startDynamicDictionary(dictionary);
                break;
        }
    }

    private void startDynamicDictionary(DictionaryEntity dictionary) {
        if (!timers.containsKey(dictionary.getId())) {
            DictionaryProviderEntity providerConf = dictionary.getProvider();

            if (DICTIONARY_HTTP_PROVIDER.equals(providerConf.getType())) {
                try {
                    HttpProviderConfiguration configuration = objectMapper.treeToValue(
                        providerConf.getConfiguration(),
                        HttpProviderConfiguration.class
                    );
                    DictionaryRefresher refresher = new DictionaryRefresher(dictionary);

                    HttpProvider provider = new HttpProvider(configuration);
                    provider.setHttpClientService(httpClientService);
                    provider.setNode(node);

                    refresher.setProvider(provider);
                    refresher.setDictionaryService(dictionaryService);
                    log.info(
                        "Add a scheduled task to poll dictionary provider for dictionary [{}] each {} {} ",
                        dictionary.getId(),
                        dictionary.getTrigger().getRate(),
                        dictionary.getTrigger().getUnit()
                    );

                    // Force the first refresh, and then run it periodically
                    refresher.handle(null);

                    long periodicTimer = vertx.setPeriodic(getDelayMillis(dictionary.getTrigger()), refresher);
                    timers.put(dictionary.getId(), periodicTimer);
                } catch (JsonProcessingException jpe) {
                    log.error("Dictionary provider configuration for dictionary [{}] is invalid", dictionary.getId(), jpe);
                }
            }
        }
    }

    private long getDelayMillis(DictionaryTriggerEntity trigger) {
        switch (trigger.getUnit()) {
            case MILLISECONDS:
                return trigger.getRate();
            case SECONDS:
                return trigger.getRate() * 1000;
            case MINUTES:
                return trigger.getRate() * 1000 * 60;
            case HOURS:
                return trigger.getRate() * 1000 * 60 * 60;
        }

        return -1;
    }

    private void stopDynamicDictionary(DictionaryEntity dictionary) {
        Long timer = timers.remove(dictionary.getId());
        if (timer != null) {
            log.info("Stop dictionary refresher task for dictionary [{}]", dictionary.getId());
            vertx.cancelTimer(timer);
        }
    }
}
