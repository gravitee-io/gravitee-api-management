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
package io.gravitee.rest.api.services.dynamicproperties;

import io.gravitee.definition.model.Properties;
import io.gravitee.definition.model.Property;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.api.ApiDeploymentEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.services.dynamicproperties.model.DynamicProperty;
import io.gravitee.rest.api.services.dynamicproperties.provider.Provider;
import io.vertx.core.Handler;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DynamicPropertyUpdater implements Handler<Long> {

    private final Logger logger = LoggerFactory.getLogger(DynamicPropertyUpdater.class);

    private ApiEntity api;
    private Provider provider;
    private ApiService apiService;
    private ApiConverter apiConverter;

    public DynamicPropertyUpdater(final ApiEntity api) {
        this.api = api;
    }

    private void authenticateAsAdmin() {
        SecurityContextHolder.setContext(
            new SecurityContext() {
                @Override
                public Authentication getAuthentication() {
                    return new Authentication() {
                        @Override
                        public Collection<? extends GrantedAuthority> getAuthorities() {
                            return AuthorityUtils.createAuthorityList(RoleScope.ENVIRONMENT.name() + ':' + SystemRole.ADMIN.name());
                        }

                        @Override
                        public Object getCredentials() {
                            return null;
                        }

                        @Override
                        public Object getDetails() {
                            return null;
                        }

                        @Override
                        public Object getPrincipal() {
                            return new UserDetails("DynamicPropertyUpdater", "*****", Collections.emptyList());
                        }

                        @Override
                        public boolean isAuthenticated() {
                            return true;
                        }

                        @Override
                        public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {}

                        @Override
                        public String getName() {
                            return null;
                        }
                    };
                }

                @Override
                public void setAuthentication(Authentication authentication) {}
            }
        );
    }

    @Override
    public void handle(Long event) {
        logger.debug("Running dynamic-properties poller for {}", api);
        authenticateAsAdmin();

        provider
            .get()
            .whenComplete(
                (dynamicProperties, throwable) -> {
                    if (throwable != null) {
                        logger.error(
                            "[{}] Unexpected error while getting dynamic properties from provider: {}",
                            api.getId(),
                            provider.name(),
                            throwable
                        );
                    } else if (dynamicProperties != null) {
                        update(dynamicProperties);
                    }
                }
            );
    }

    private void update(Collection<DynamicProperty> dynamicProperties) {
        // Get latest changes
        ApiEntity latestApi = apiService.findById(api.getId());

        List<Property> properties = (latestApi.getProperties() != null)
            ? latestApi.getProperties().getProperties()
            : Collections.emptyList();
        List<Property> userDefinedProperties = properties.stream().filter(property -> !property.isDynamic()).collect(Collectors.toList());

        Map<String, Property> propertyMap = properties.stream().collect(Collectors.toMap(Property::getKey, property -> property));

        List<Property> updatedProperties = new ArrayList<>();
        boolean needToBeSaved = false;
        for (DynamicProperty dynamicProperty : dynamicProperties) {
            Property property = propertyMap.get(dynamicProperty.getKey());
            if (property == null || property.isDynamic()) {
                updatedProperties.add(dynamicProperty);
            }
            // save properties only if there's something new
            if (property == null || (property.isDynamic() && !property.getValue().equals(dynamicProperty.getValue()))) {
                needToBeSaved = true;
            }
        }

        if (needToBeSaved) {
            // Add previous user-defined properties
            updatedProperties.addAll(userDefinedProperties);

            // Sort properties alphabetically to avoid redeploy if just the order has changed.
            List<Property> sortedUpdatedProperties = updatedProperties
                .stream()
                .sorted(Comparator.comparing(Property::getKey))
                .collect(Collectors.toList());
            // Create properties container
            Properties apiProperties = new Properties();
            try {
                apiProperties.setProperties(sortedUpdatedProperties);
            } catch (RuntimeException e) {
                logger.error(e.getMessage(), e);
            }
            latestApi.setProperties(apiProperties);

            boolean isSync = apiService.isSynchronized(api.getId());

            // Update API
            apiService.update(latestApi.getId(), apiConverter.toUpdateApiEntity(latestApi));

            // Do not deploy if there are manual changes to push
            if (isSync) {
                // Publish API only in case of changes
                if (!updatedProperties.containsAll(properties) || !properties.containsAll(updatedProperties)) {
                    ApiDeploymentEntity deployEntity = new ApiDeploymentEntity();
                    deployEntity.setDeploymentLabel("Dynamic properties sync");
                    apiService.deploy(latestApi.getId(), "dynamic-property-updater", EventType.PUBLISH_API, deployEntity);
                }
            }
        }
    }

    public void setApiService(ApiService apiService) {
        this.apiService = apiService;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    public void setApiConverter(ApiConverter apiConverter) {
        this.apiConverter = apiConverter;
    }
}
