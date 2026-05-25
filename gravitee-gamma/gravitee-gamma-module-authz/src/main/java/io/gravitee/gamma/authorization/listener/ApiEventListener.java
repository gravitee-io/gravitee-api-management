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
package io.gravitee.gamma.authorization.listener;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.infra.adapter.ApiAdapter;
import io.gravitee.common.event.Event;
import io.gravitee.common.event.EventListener;
import io.gravitee.common.event.EventManager;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.gamma.authorization.api.AuthzCallerContext;
import io.gravitee.gamma.authorization.api.AuthzEntityAdminApi;
import io.gravitee.gamma.authorization.domain.AuthzEntityKind;
import io.gravitee.gamma.authorization.service.CreateOrReplaceAuthzEntityCommand;
import io.gravitee.gamma.definition.authz.AuthzEntityIdConstants;
import io.gravitee.rest.api.service.event.ApiEvent;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import lombok.CustomLog;

@CustomLog
public class ApiEventListener implements EventListener<ApiEvent, io.gravitee.repository.management.model.Api> {

    private static final String SOURCE = "apim";

    private final EventManager eventManager;
    private final AuthzEntityAdminApi entityService;
    private final EntityIdExtractor extractor;
    private final Function<io.gravitee.repository.management.model.Api, Api> repoToCoreAdapter;

    public ApiEventListener(EventManager eventManager, AuthzEntityAdminApi entityService, EntityIdExtractor extractor) {
        this(eventManager, entityService, extractor, ApiAdapter.INSTANCE::toCoreModel);
    }

    ApiEventListener(
        EventManager eventManager,
        AuthzEntityAdminApi entityService,
        EntityIdExtractor extractor,
        Function<io.gravitee.repository.management.model.Api, Api> repoToCoreAdapter
    ) {
        this.eventManager = Objects.requireNonNull(eventManager, "eventManager must not be null");
        this.entityService = Objects.requireNonNull(entityService, "entityService must not be null");
        this.extractor = Objects.requireNonNull(extractor, "extractor must not be null");
        this.repoToCoreAdapter = Objects.requireNonNull(repoToCoreAdapter, "repoToCoreAdapter must not be null");
    }

    @PostConstruct
    public void subscribe() {
        eventManager.subscribeForEvents(this, ApiEvent.class);
    }

    @PreDestroy
    public void unsubscribe() {
        try {
            eventManager.unsubscribeForEvents(this, ApiEvent.class);
        } catch (RuntimeException e) {
            log.warn("Failed to unsubscribe AuthzApiEventListener from EventManager during shutdown", e);
        }
    }

    @Override
    public void onEvent(Event<ApiEvent, io.gravitee.repository.management.model.Api> event) {
        io.gravitee.repository.management.model.Api repoApi = event.content();
        if (repoApi == null || !isV4(repoApi)) {
            return;
        }
        Api coreApi = repoToCoreAdapter.apply(repoApi);
        if (coreApi != null) {
            handle(event.type(), coreApi);
        }
    }

    void handle(ApiEvent type, Api coreApi) {
        if (type == null || coreApi == null) {
            return;
        }
        String envId = coreApi.getEnvironmentId();
        if (envId == null || envId.isBlank()) {
            log.warn("Skipping ApiEvent {} for API '{}' — missing environmentId", type, coreApi.getId());
            return;
        }
        switch (type) {
            case DEPLOY -> upsertAll(coreApi);
            case UPDATE -> syncForApi(coreApi);
            case UNDEPLOY -> undeploy(coreApi);
            default -> {}
        }
    }

    private void upsertAll(Api coreApi) {
        Set<String> entityIds = extractor.extract(coreApi);
        if (entityIds.isEmpty()) {
            return;
        }
        String envId = coreApi.getEnvironmentId();
        AuthzCallerContext caller = AuthzCallerContext.system(envId);
        Map<String, Object> attributes = attributesFor(coreApi);
        List<CreateOrReplaceAuthzEntityCommand> commands = entityIds
            .stream()
            .map(entityId ->
                new CreateOrReplaceAuthzEntityCommand(envId, entityId, AuthzEntityKind.RESOURCE, attributes, List.of(), SOURCE)
            )
            .toList();
        try {
            entityService.bulkUpsert(caller, commands);
        } catch (RuntimeException e) {
            log.warn("Failed to bulk-upsert authz entities for API '{}' ({} aliases)", coreApi.getId(), entityIds.size(), e);
        }
    }

    private void syncForApi(Api coreApi) {
        String envId = coreApi.getEnvironmentId();
        AuthzCallerContext caller = AuthzCallerContext.system(envId);
        String apiId = coreApi.getId();
        Set<String> currentIds = extractor.extract(coreApi);
        Set<String> previousIds = entityService.findApiAliases(envId, apiId);

        for (String orphan : previousIds) {
            if (!currentIds.contains(orphan)) {
                try {
                    entityService.delete(caller, orphan);
                } catch (RuntimeException e) {
                    log.warn("Failed to cascade-delete stale authz alias '{}' on UPDATE", orphan, e);
                }
            }
        }
        upsertAll(coreApi);
    }

    private void undeploy(Api coreApi) {
        String envId = coreApi.getEnvironmentId();
        AuthzCallerContext caller = AuthzCallerContext.system(envId);
        String rootEntityId = AuthzEntityIdConstants.API_PREFIX + coreApi.getId();
        try {
            entityService.delete(caller, rootEntityId);
        } catch (RuntimeException e) {
            log.warn("Failed to cascade-delete authz entity '{}' on UNDEPLOY", rootEntityId, e);
        }
    }

    private static Map<String, Object> attributesFor(Api coreApi) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        if (coreApi.getId() != null) attributes.put("apiId", coreApi.getId());
        if (coreApi.getName() != null) attributes.put("apiName", coreApi.getName());
        if (coreApi.getVersion() != null) attributes.put("apiVersion", coreApi.getVersion());
        return attributes;
    }

    private static boolean isV4(io.gravitee.repository.management.model.Api repoApi) {
        return repoApi.getDefinitionVersion() != null && repoApi.getDefinitionVersion().equals(DefinitionVersion.V4);
    }
}
