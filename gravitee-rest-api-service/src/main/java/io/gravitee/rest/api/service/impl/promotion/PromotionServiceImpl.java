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
package io.gravitee.rest.api.service.impl.promotion;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PromotionRepository;
import io.gravitee.repository.management.model.Promotion;
import io.gravitee.repository.management.model.PromotionStatus;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.promotion.PromotionEntity;
import io.gravitee.rest.api.model.promotion.PromotionEntityStatus;
import io.gravitee.rest.api.model.promotion.PromotionRequestEntity;
import io.gravitee.rest.api.model.promotion.PromotionTargetEntity;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.InstallationService;
import io.gravitee.rest.api.service.cockpit.command.bridge.operation.BridgeOperation;
import io.gravitee.rest.api.service.cockpit.services.CockpitReply;
import io.gravitee.rest.api.service.cockpit.services.CockpitReplyStatus;
import io.gravitee.rest.api.service.cockpit.services.CockpitService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.BridgeOperationException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.AbstractService;
import io.gravitee.rest.api.service.jackson.ser.api.ApiSerializer;
import io.gravitee.rest.api.service.promotion.PromotionService;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class PromotionServiceImpl extends AbstractService implements PromotionService {

    private final ApiService apiService;
    private final CockpitService cockpitService;
    private final InstallationService installationService;
    private final PromotionRepository promotionRepository;
    private final EnvironmentService environmentService;

    public PromotionServiceImpl(
        ApiService apiService,
        CockpitService cockpitService,
        InstallationService installationService,
        PromotionRepository promotionRepository,
        EnvironmentService environmentService
    ) {
        this.apiService = apiService;
        this.cockpitService = cockpitService;
        this.installationService = installationService;
        this.promotionRepository = promotionRepository;
        this.environmentService = environmentService;
    }

    @Override
    public List<PromotionTargetEntity> listPromotionTargets(String organizationId, String environmentId) {
        EnvironmentEntity environmentEntity = environmentService.findById(environmentId);

        final CockpitReply<List<PromotionTargetEntity>> listCockpitReply = this.cockpitService.listPromotionTargets(organizationId);
        if (listCockpitReply.getStatus() == CockpitReplyStatus.SUCCEEDED) {
            return listCockpitReply
                .getReply()
                .stream()
                // Check using HRID instead of ID because the 'DEFAULT' env id has been overridden by Cockpit
                .filter(target -> !target.getHrids().equals(environmentEntity.getHrids()))
                .collect(Collectors.toList());
        }
        throw new BridgeOperationException(BridgeOperation.LIST_ENVIRONMENT);
    }

    @Override
    public PromotionEntity promote(String api, PromotionRequestEntity promotionRequest) {
        // TODO: do we have to use filteredFields like for duplicate (i think no need members and groups)
        // FIXME: can we get the version from target environment
        String apiDefinition = apiService.exportAsJson(api, ApiSerializer.Version.DEFAULT.getVersion());

        Promotion promotionToSave = convert(promotionRequest, apiDefinition);
        Promotion createdPromotion = null;
        try {
            createdPromotion = promotionRepository.create(promotionToSave);
        } catch (TechnicalException exception) {
            throw new TechnicalManagementException(
                String.format("An error occurs while trying to create a promotion request for API %s", api),
                exception
            );
        }

        PromotionEntity promotionEntity = convert(createdPromotion);
        CockpitReply<PromotionEntity> cockpitReply = cockpitService.requestPromotion(promotionEntity);
        if (cockpitReply.getStatus() != CockpitReplyStatus.SUCCEEDED) {
            throw new BridgeOperationException(BridgeOperation.PROMOTE_API);
        }

        return promotionEntity;
    }

    private Promotion convert(PromotionRequestEntity promotionRequest, String apiDefinition) {
        Promotion promotion = new Promotion();
        promotion.setCreatedAt(new Date());
        promotion.setStatus(PromotionStatus.CREATED);
        promotion.setApiDefinition(apiDefinition);
        promotion.setSourceEnvironmentId(GraviteeContext.getCurrentEnvironment());
        promotion.setSourceInstallationId(installationService.get().getId());
        promotion.setTargetEnvironmentId(promotionRequest.getTargetEnvironmentId());
        promotion.setTargetInstallationId(promotionRequest.getTargetInstallationId());

        return promotion;
    }

    private PromotionEntity convert(Promotion promotion) {
        PromotionEntity promotionEntity = new PromotionEntity();
        promotionEntity.setCreatedAt(promotion.getCreatedAt());
        promotionEntity.setUpdatedAt(promotion.getUpdatedAt());
        promotionEntity.setSourceEnvironmentId(promotion.getSourceEnvironmentId());
        promotionEntity.setSourceInstallationId(promotion.getSourceInstallationId());
        promotionEntity.setTargetEnvironmentId(promotion.getTargetEnvironmentId());
        promotionEntity.setTargetInstallationId(promotion.getTargetInstallationId());
        promotionEntity.setApiDefinition(promotion.getApiDefinition());
        promotionEntity.setStatus(convert(promotion.getStatus()));

        return promotionEntity;
    }

    private PromotionEntityStatus convert(PromotionStatus promotionStatus) {
        return PromotionEntityStatus.valueOf(promotionStatus.name());
    }
}
