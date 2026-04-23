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
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.subscription.domain_service.SubscriptionCRDDomainService;
import io.gravitee.apim.core.subscription.domain_service.ValidateSubscriptionCRDDomainService;
import io.gravitee.apim.core.subscription.model.crd.SubscriptionCRDSpec;
import io.gravitee.apim.core.subscription.model.crd.SubscriptionCRDStatus;
import io.gravitee.apim.core.validation.Validator;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@UseCase
@RequiredArgsConstructor
public class ImportSubscriptionCRDUseCase {

    public record Input(AuditInfo auditInfo, SubscriptionCRDSpec spec) {}

    public record Output(SubscriptionCRDStatus status) {}

    private final SubscriptionCRDDomainService domainService;
    private final ValidateSubscriptionCRDDomainService validateSubscriptionCRDDomainService;

    public Output execute(Input input) {
        var validationResult = validateSubscriptionCRDDomainService
            .validateAndSanitize(new ValidateSubscriptionCRDDomainService.Input(input.auditInfo(), input.spec()))
            .map(sanitized -> new Input(sanitized.auditInfo(), sanitized.spec()));

        validationResult
            .severe()
            .ifPresent(errors -> {
                throw new ValidationDomainException(
                    String.format(
                        "Unable to import because of errors [%s]",
                        String.join(",", errors.stream().map(Validator.Error::getMessage).toList())
                    )
                );
            });

        var warnings = validationResult.warning().orElseGet(List::of);
        var sanitizedInput = validationResult.value().orElseThrow(() -> new ValidationDomainException("Unable to sanitize CRD spec"));

        var subscription = domainService.createOrUpdate(sanitizedInput.auditInfo(), sanitizedInput.spec());
        return new Output(
            new SubscriptionCRDStatus(
                subscription.getId(),
                subscription.getStatus().name(),
                subscription.getStartingAt(),
                subscription.getEndingAt(),
                input.auditInfo().organizationId(),
                input.auditInfo().environmentId(),
                SubscriptionCRDStatus.Errors.fromErrorList(warnings)
            )
        );
    }
}
