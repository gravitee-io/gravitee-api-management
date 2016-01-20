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
package io.gravitee.gateway.services.sync;

import io.gravitee.gateway.core.definition.Api;
import io.gravitee.gateway.core.manager.ApiManager;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.gravitee.repository.management.model.LifecycleState;

import java.io.IOException;
import java.net.InetAddress;
import java.text.Collator;
import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class SyncManager {

    public static final String TAGS_PROP = "tags";

    private final Logger logger = LoggerFactory.getLogger(SyncManager.class);

    @Autowired
    private ApiRepository apiRepository;
    
    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ApiManager apiManager;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${tags:}")
    private String propertyTags;

    public void refresh() {
        logger.debug("Refreshing gateway state...");

        try {
            Set<io.gravitee.repository.management.model.Api> apis = apiRepository.findAll();
            
            // Determine deployed APIs store into events payload
            Set<Api> deployedApis = getDeployedApis(apis);

            Map<String, Api> apisMap = deployedApis.stream()
                    .filter(api -> api != null && isConfiguredTags(api))
                    .collect(Collectors.toMap(Api::getId, api -> api));

            // Determine APIs to undeploy
            Set<String> apiToRemove = apiManager.apis().stream()
                    .filter(api -> !apisMap.containsKey(api.getId()) || !isConfiguredTags(api))
                    .map(Api::getId)
                    .collect(Collectors.toSet());

            apiToRemove.stream().forEach(apiManager::undeploy);

            // Determine APIs to update
            apisMap.keySet().stream()
                    .filter(apiId -> apiManager.get(apiId) != null)
                    .forEach(apiId -> {
                        // Get local cached API
                        Api deployedApi = apiManager.get(apiId);

                        // Get API from store
                        Api remoteApi = apisMap.get(apiId);

                        if (deployedApi.getDeployedAt().before(remoteApi.getDeployedAt())) {
                            apiManager.update(remoteApi);
                        }
                    });

            // Determine APIs to deploy
            apisMap.keySet().stream()
                    .filter(api -> apiManager.get(api) == null)
                    .forEach(api -> {
                        Api newApi = apisMap.get(api);
                        apiManager.deploy(newApi);
                    });

        } catch (TechnicalException te) {
            logger.error("Unable to sync instance", te);
        }
    }

    private boolean isConfiguredTags(Api api) {
        final String systemPropertyTags = System.getProperty(TAGS_PROP);
        final String tags = systemPropertyTags == null ? propertyTags : systemPropertyTags;
        if (tags != null && !tags.isEmpty()) {
            if (api.getTags() != null) {
                final List<String> tagList = Arrays.asList(tags.split(","));
                return tagList.stream()
                        .anyMatch(tag -> api.getTags().stream()
                                .anyMatch(apiTag -> {
                                    final Collator collator = Collator.getInstance();
                                    collator.setStrength(Collator.NO_DECOMPOSITION);
                                    return collator.compare(tag, apiTag) == 0;
                                })
                        );
            }
            // tags are configured on gateway instance but not found on api
            return false;
        }
        // no tags configured on this gateway instance
        return true;
    }
    
    private Set<Api> getDeployedApis(Set<io.gravitee.repository.management.model.Api> apis) {
    	Set<Api> deployedApis = new HashSet<Api>();
    	
    	Collection<Event> events = eventRepository.findByType(Arrays.asList(EventType.PUBLISH_API, EventType.UNPUBLISH_API));
    	List<Event> eventsSorted = events.stream().sorted((e1, e2) -> e1.getCreatedAt().compareTo(e2.getCreatedAt())).collect(Collectors.toList());
    	Collections.reverse(eventsSorted);
    	try {
    		for (io.gravitee.repository.management.model.Api api : apis) {
	    		for (Event _event : eventsSorted) {
	    			JsonNode node = objectMapper.readTree(_event.getPayload());
	    			io.gravitee.repository.management.model.Api payloadApi = objectMapper.convertValue(node, io.gravitee.repository.management.model.Api.class);
	    			if (api.getId().equals(payloadApi.getId())) {
	    				deployedApis.add(convert(payloadApi));
	    				// create success publish API event
	    				String hostAddress = InetAddress.getLocalHost().getHostAddress();
	    				Event event = new Event();
	    				event.setType(EventType.PUBLISH_API_RESULT);
	    				event.setPayload("API : " + api.getId() + " deployed");
	    				event.setParentId(event.getId());

                        Map<String, String> eventProps = new HashMap<>();
                        eventProps.put(Event.EventProperties.ORIGIN.getValue(), hostAddress);

	    				eventRepository.create(event);
	    				break;
	    			}
	    		}
    		}
    	} catch (Exception e) {
    		logger.error("Error while determining deployed APIs store into events payload", e);
    	}
    	
    	return deployedApis;
    }

    private Api convert(io.gravitee.repository.management.model.Api remoteApi) {
        try {
            String definition = remoteApi.getDefinition();
            if (definition != null && !definition.isEmpty()) {
                Api api = objectMapper.readValue(definition, Api.class);

                api.setId(remoteApi.getId());
                api.setName(remoteApi.getName());
                api.setVersion(remoteApi.getVersion());
                api.setEnabled(remoteApi.getLifecycleState() == LifecycleState.STARTED);
                api.setDeployedAt(remoteApi.getUpdatedAt());

                return api;
            }
        } catch (IOException ioe) {
            logger.error("Unable to prepare API definition from repository", ioe);
        }

        return null;
    }

    public void setApiRepository(ApiRepository apiRepository) {
        this.apiRepository = apiRepository;
    }

    public void setEventRepository(EventRepository eventRepository) {
    	this.eventRepository = eventRepository;
    }
    
    public void setApiManager(ApiManager apiManager) {
        this.apiManager = apiManager;
    }
}
