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
package io.gravitee.management.services.dynamicproperties;

import io.gravitee.definition.model.Properties;
import io.gravitee.definition.model.Property;
import io.gravitee.management.model.ApiEntity;
import io.gravitee.management.model.EventType;
import io.gravitee.management.model.UpdateApiEntity;
import io.gravitee.management.service.ApiService;
import io.gravitee.management.services.dynamicproperties.model.DynamicProperty;
import io.gravitee.management.services.dynamicproperties.provider.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DynamicPropertyUpdater implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(DynamicPropertyUpdater.class);

    private ApiEntity api;
    private Provider provider;
    private ApiService apiService;

    public DynamicPropertyUpdater(final ApiEntity api) {
        this.api = api;
    }

    @Override
    public void run() {
        logger.debug("Running dynamic-properties poller for {}", api);

        provider.get()
                .whenComplete((dynamicProperties, throwable) -> {
                    if (throwable != null) {
                        logger.error("Unexpected error while getting dynamic properties from provider: {}",
                                provider.name(), throwable);
                    } else if (dynamicProperties != null) {
                        update(dynamicProperties);
                    }
                });
    }

    private void update(Collection<DynamicProperty> dynamicProperties) {
        // Get latest changes
        ApiEntity latestApi = apiService.findById(api.getId());

        List<Property> properties = latestApi.getProperties().getProperties();
        List<Property> userDefinedProperties = properties.stream().filter(property -> !property.isDynamic()).collect(Collectors.toList());

        Map<String, Property> propertyMap = properties.stream().collect(Collectors.toMap(Property::getKey, property -> property));

        List<Property> updatedProperties = new ArrayList<>();
        dynamicProperties.forEach(dynamicProperty -> {
            Property property = propertyMap.get(dynamicProperty.getKey());
            if (property == null) {
                updatedProperties.add(dynamicProperty);
            } else if (property.isDynamic()) {
                updatedProperties.add(dynamicProperty);
            }
        });

        // Add previous user-defined properties
        updatedProperties.addAll(userDefinedProperties);

        // Create properties container
        Properties apiProperties = new Properties();
        apiProperties.setProperties(updatedProperties);
        latestApi.setProperties(apiProperties);

        // Update API
        apiService.update(latestApi.getId(), convert(latestApi));

        // Do not deploy if there are manual changes to push
        if (apiService.isSynchronized(api.getId())) {
            // Publish API only in case of changes
            if (!updatedProperties.containsAll(properties) || !properties.containsAll(updatedProperties)) {
                apiService.deploy(latestApi.getId(), "dynamic-property-updater", EventType.PUBLISH_API);
            }
        }
    }

    private static UpdateApiEntity convert(ApiEntity apiEntity) {
        UpdateApiEntity updateApiEntity = new UpdateApiEntity();

        updateApiEntity.setProxy(apiEntity.getProxy());
        updateApiEntity.setVersion(apiEntity.getVersion());
        updateApiEntity.setName(apiEntity.getName());
        updateApiEntity.setProperties(apiEntity.getProperties());
        updateApiEntity.setDescription(apiEntity.getDescription());

        if (apiEntity.getGroup() != null) {
            updateApiEntity.setGroup(apiEntity.getGroup().getId());
        }
        updateApiEntity.setPaths(apiEntity.getPaths());
        updateApiEntity.setPicture(apiEntity.getPicture());
        updateApiEntity.setResources(apiEntity.getResources());
        updateApiEntity.setTags(apiEntity.getTags());
        updateApiEntity.setServices(apiEntity.getServices());
        updateApiEntity.setVisibility(apiEntity.getVisibility());

        return updateApiEntity;
    }

    public void setApiService(ApiService apiService) {
        this.apiService = apiService;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }
}
