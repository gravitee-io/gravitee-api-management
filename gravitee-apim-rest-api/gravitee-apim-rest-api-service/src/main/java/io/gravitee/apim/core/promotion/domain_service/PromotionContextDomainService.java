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
package io.gravitee.apim.core.promotion.domain_service;

import com.fasterxml.jackson.databind.json.JsonMapper;
import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.core.environment.crud_service.EnvironmentCrudService;
import io.gravitee.apim.core.promotion.crud_service.PromotionCrudService;
import io.gravitee.apim.core.promotion.model.Promotion;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;

@DomainService
public class PromotionContextDomainService {

    private final PromotionCrudService promotionCrudService;
    private final ApiQueryService apiQueryService;
    private final EnvironmentCrudService environmentCrudService;
    private final JsonMapper jsonMapper;

    public PromotionContextDomainService(
        PromotionCrudService promotionCrudService,
        ApiQueryService apiQueryService,
        EnvironmentCrudService environmentCrudService,
        JsonMapper jsonMapper
    ) {
        this.promotionCrudService = promotionCrudService;
        this.apiQueryService = apiQueryService;
        this.environmentCrudService = environmentCrudService;
        this.jsonMapper = jsonMapper;
    }

    public record PromotionContext(
        Promotion promotion,
        DefinitionVersion expectedDefinitionVersion,
        Api existingPromotedApi,
        String targetEnvId
    ) {}

    /*
     * Validates the promotion before applying it.
     *
     * This validation is required because V2 APIs can be migrated to V4 APIs using the dedicated feature.
     * We assume that the target API and its expected definition versions must match.
     *
     * Once all V2 APIs are migrated and V2 no longer supported, this validation step can be removed,
     * and the promotion can be applied directly through the corresponding UseCase.
     */
    public PromotionContext getPromotionContext(String promotionId, boolean isAccepted) {
        var promotion = promotionCrudService.getById(promotionId);
        var crossId = extractCrossIdFromDefinition(promotion);
        var environment = environmentCrudService.getByCockpitId(promotion.getTargetEnvCockpitId());
        var targetApiOpt = apiQueryService.findByEnvironmentIdAndCrossId(environment.getId(), crossId);
        var expectedDefinitionVersion = getPromotionDefinitionVersion(promotion);

        if (isAccepted && targetApiOpt.isPresent()) {
            if (targetApiOpt.get().getDefinitionVersion() != expectedDefinitionVersion) {
                throw new IllegalStateException(
                    "An API with the same crossId already exists with a different definition version in the target environment"
                );
            }
        }

        return new PromotionContext(promotion, expectedDefinitionVersion, targetApiOpt.orElse(null), environment.getId());
    }

    /**
     * Extracts the crossId from the API definition JSON stored in the promotion.
     */
    private String extractCrossIdFromDefinition(Promotion promotion) {
        try {
            var root = jsonMapper.readTree(promotion.getApiDefinition());

            // Try to get crossId directly (v2 format)
            var crossIdNode = root.get("crossId");
            if (crossIdNode != null && !crossIdNode.isNull()) {
                return crossIdNode.asText();
            }

            // Try to get crossId from api object (v4 format)
            var apiNode = root.get("api");
            if (apiNode != null && !apiNode.isNull()) {
                crossIdNode = apiNode.get("crossId");
                if (crossIdNode != null && !crossIdNode.isNull()) {
                    return crossIdNode.asText();
                }
            }

            throw new IllegalStateException("Could not find crossId in promotion " + promotion.getId() + " API definition");
        } catch (Exception e) {
            throw new TechnicalManagementException("An error occurred while trying to extract crossId from promotion " + promotion.getId());
        }
    }

    private DefinitionVersion getPromotionDefinitionVersion(Promotion promotion) {
        try {
            var root = jsonMapper.readTree(promotion.getApiDefinition());

            var graviteeNode = root.findValue("gravitee");
            if (graviteeNode != null && "2.0.0".equals(graviteeNode.asText())) {
                return DefinitionVersion.V2;
            }

            var definitionVersionNode = root.findValue("definitionVersion");
            if (definitionVersionNode != null && !definitionVersionNode.isNull()) {
                return DefinitionVersion.valueOf(definitionVersionNode.asText());
            }
        } catch (Exception e) {
            throw new TechnicalManagementException(
                "An error occurred while try to parse promotion definition version " + promotion.getId()
            );
        }

        throw new IllegalStateException("Could not determine definition version for promotion " + promotion.getId());
    }
}
