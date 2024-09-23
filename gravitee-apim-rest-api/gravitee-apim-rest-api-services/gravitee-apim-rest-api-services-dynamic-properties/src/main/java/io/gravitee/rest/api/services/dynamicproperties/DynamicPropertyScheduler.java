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
package io.gravitee.rest.api.services.dynamicproperties;

import static io.gravitee.rest.api.service.common.SecurityContextHelper.authenticateAsSystem;

import io.gravitee.common.cron.CronTrigger;
import io.gravitee.definition.model.Properties;
import io.gravitee.definition.model.Property;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.UserRoleEntity;
import io.gravitee.rest.api.model.api.ApiDeploymentEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.services.dynamicproperties.model.DynamicProperty;
import io.gravitee.rest.api.services.dynamicproperties.provider.Provider;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class DynamicPropertyScheduler {

    private final ApiService apiService;
    private final ApiConverter apiConverter;
    private final String schedule;
    private final ApiEntity api;
    private final ExecutionContext executionContext;
    private Disposable disposable;

    @Builder
    public DynamicPropertyScheduler(
        final ApiService apiService,
        final ApiConverter apiConverter,
        final String schedule,
        final ApiEntity api,
        final ExecutionContext executionContext
    ) {
        this.apiService = apiService;
        this.apiConverter = apiConverter;
        this.schedule = schedule;
        this.api = api;
        this.executionContext = executionContext;
    }

    public void schedule(final Provider provider) {
        CronTrigger cronTrigger = new CronTrigger(schedule);
        log.debug("[{}] Running dynamic properties scheduler", api.getId());

        disposable =
            Observable
                .defer(() -> Observable.timer(cronTrigger.nextExecutionIn(), TimeUnit.MILLISECONDS))
                .observeOn(Schedulers.computation())
                .switchMapCompletable(aLong ->
                    provider
                        .get()
                        .flatMapCompletable(dynamicProperties ->
                            Completable.fromRunnable(() -> {
                                log.debug("[{}] Got {} dynamic properties to update", api.getId(), dynamicProperties.size());
                                authenticateAsAdmin();
                                update(dynamicProperties);
                            })
                        )
                        .doOnComplete(() -> log.debug("[{}] Dynamic properties updated", api.getId()))
                )
                .onErrorResumeNext(throwable -> {
                    log.error(
                        "[{}] Unexpected error while getting dynamic properties from provider: {}",
                        api.getId(),
                        provider.name(),
                        throwable
                    );
                    return Completable.complete();
                })
                .repeat()
                .subscribe(() -> {}, throwable -> log.error("Unable to run Dynamic Properties for Api: {}", api.getId()));
    }

    public void cancel() {
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }

    private void authenticateAsAdmin() {
        UserRoleEntity adminRole = new UserRoleEntity();
        adminRole.setScope(RoleScope.ENVIRONMENT);
        adminRole.setName(SystemRole.ADMIN.name());
        authenticateAsSystem("DynamicPropertyUpdater", Set.of(adminRole));
    }

    private void update(List<DynamicProperty> dynamicProperties) {
        // Get latest changes
        ApiEntity latestApi = apiService.findById(executionContext, api.getId());

        List<Property> properties = (latestApi.getProperties() != null)
            ? latestApi.getProperties().getProperties()
            : Collections.emptyList();
        List<Property> userDefinedProperties = properties.stream().filter(property -> !property.isDynamic()).toList();

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
                log.error(e.getMessage(), e);
            }
            latestApi.setProperties(apiProperties);

            boolean isSync = apiService.isSynchronized(executionContext, api.getId());

            // Update API
            try {
                log.debug("[{}] Updating API", latestApi.getId());
                apiService.update(executionContext, latestApi.getId(), apiConverter.toUpdateApiEntity(latestApi), false, false);
                log.debug("[{}] API has been updated", latestApi.getId());
            } catch (TechnicalManagementException e) {
                log.error("An error occurred while updating the API with new values of dynamic properties, deployment will be skipped.", e);
                throw e;
            }

            // Do not deploy if there are manual changes to push
            if (isSync) {
                // Publish API only in case of changes
                if (!updatedProperties.containsAll(properties) || !properties.containsAll(updatedProperties)) {
                    log.debug("[{}] Property change detected, API is about to be deployed", api.getId());
                    ApiDeploymentEntity deployEntity = new ApiDeploymentEntity();
                    deployEntity.setDeploymentLabel("Dynamic properties sync");
                    apiService.deploy(executionContext, latestApi.getId(), "dynamic-property-updater", EventType.PUBLISH_API, deployEntity);
                    log.debug("[{}] API as been deployed", api.getId());
                }
            }
        }
    }
}
