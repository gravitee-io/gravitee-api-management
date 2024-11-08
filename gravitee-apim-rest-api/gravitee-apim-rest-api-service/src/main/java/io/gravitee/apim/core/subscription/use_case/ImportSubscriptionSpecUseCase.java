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
package io.gravitee.apim.core.subscription.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.subscription.domain_service.SubscriptionCRDSpecDomainService;
import io.gravitee.apim.core.subscription.model.crd.SubscriptionCRDSpec;
import java.time.ZonedDateTime;
import lombok.RequiredArgsConstructor;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@UseCase
@RequiredArgsConstructor
public class ImportSubscriptionSpecUseCase {

    public record Input(AuditInfo auditInfo, SubscriptionCRDSpec spec) {}

    public record Output(String id, String status, ZonedDateTime startingAt, ZonedDateTime endingAt) {}

    private final SubscriptionCRDSpecDomainService domainService;

    public Output execute(Input input) {
        var subscription = domainService.createOrUpdate(input.auditInfo, input.spec);
        return new Output(subscription.getId(), subscription.getStatus().name(), subscription.getStartingAt(), subscription.getEndingAt());
    }
}
