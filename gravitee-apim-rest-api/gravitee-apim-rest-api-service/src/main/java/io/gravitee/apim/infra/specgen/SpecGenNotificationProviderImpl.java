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
package io.gravitee.apim.infra.specgen;

import static io.gravitee.rest.api.service.common.GraviteeContext.getExecutionContext;
import static io.gravitee.rest.api.service.notification.ApiHook.NEW_SPEC_GENERATED;

import io.gravitee.apim.core.specgen.model.ApiSpecGen;
import io.gravitee.apim.core.specgen.service_provider.SpecGenNotificationProvider;
import io.gravitee.rest.api.model.ApiModel;
import io.gravitee.rest.api.service.PortalNotificationService;
import io.gravitee.rest.api.service.notification.ApiHook;
import io.gravitee.rest.api.service.notification.NotificationParamsBuilder;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author Rémi SULTAN (remi.sultan at graviteesource.com)
 * @author GraviteeSource Team
 */
@Service
@Slf4j
public class SpecGenNotificationProviderImpl implements SpecGenNotificationProvider {

    private final PortalNotificationService portalNotificationService;

    public SpecGenNotificationProviderImpl(PortalNotificationService portalNotificationService) {
        this.portalNotificationService = portalNotificationService;
    }

    @Override
    public void notify(ApiSpecGen apiSpecGen, String userId) {
        var apiModel = new ApiModel();
        apiModel.setName(apiSpecGen.name());
        portalNotificationService.create(
            getExecutionContext(),
            NEW_SPEC_GENERATED,
            List.of(userId),
            new NotificationParamsBuilder().api(apiModel).build()
        );
    }
}
