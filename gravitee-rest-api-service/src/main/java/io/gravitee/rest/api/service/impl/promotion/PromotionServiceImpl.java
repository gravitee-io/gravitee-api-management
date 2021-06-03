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

import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.promotion.PromotionTargetEntity;
import io.gravitee.rest.api.service.cockpit.command.bridge.operation.BridgeOperation;
import io.gravitee.rest.api.service.cockpit.services.CockpitReply;
import io.gravitee.rest.api.service.cockpit.services.CockpitReplyStatus;
import io.gravitee.rest.api.service.cockpit.services.CockpitService;
import io.gravitee.rest.api.service.exceptions.BridgeOperationException;
import io.gravitee.rest.api.service.impl.AbstractService;
import io.gravitee.rest.api.service.promotion.PromotionService;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class PromotionServiceImpl extends AbstractService implements PromotionService {

    private CockpitService cockpitService;

    public PromotionServiceImpl(CockpitService cockpitService) {
        this.cockpitService = cockpitService;
    }

    @Override
    public List<PromotionTargetEntity> listPromotionTargets(String organizationId, String environmentId) {
        final CockpitReply<List<PromotionTargetEntity>> listCockpitReply = this.cockpitService.listPromotionTargets(organizationId);
        if (listCockpitReply.getStatus() == CockpitReplyStatus.SUCCEEDED) {
            return listCockpitReply
                .getReply()
                .stream()
                .filter(target -> !target.getId().equals(environmentId))
                .collect(Collectors.toList());
        }
        throw new BridgeOperationException(BridgeOperation.LIST_ENVIRONMENT);
    }
}
