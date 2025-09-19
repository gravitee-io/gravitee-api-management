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
package io.gravitee.apim.core.notification.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.group.domain_service.ValidateGroupsDomainService;
import io.gravitee.apim.core.utils.CollectionUtils;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.rest.api.model.notification.NotificationConfigType;
import io.gravitee.rest.api.model.notification.PortalNotificationConfigEntity;
import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DomainService
@RequiredArgsConstructor
public class ValidatePortalNotificationDomainService implements Validator<ValidatePortalNotificationDomainService.Input> {

    public record Input(
        PortalNotificationConfigEntity portalNotificationConfig,
        String definitionVersion,
        Set<String> allowedGroups,
        AuditInfo auditInfo
    ) implements Validator.Input {}

    private final ValidateGroupsDomainService groupsValidator;

    @Override
    public Result<Input> validateAndSanitize(Input input) {
        var errors = new ArrayList<Error>();
        PortalNotificationConfigEntity consoleNotification = input.portalNotificationConfig();
        if (consoleNotification != null) {
            if (consoleNotification.getConfigType() != NotificationConfigType.PORTAL) {
                errors.add(Error.severe("The reference type of console notification should be '%s'", NotificationConfigType.PORTAL.name()));
            }
            groupsValidator
                .validateAndSanitize(
                    new ValidateGroupsDomainService.Input(
                        input.auditInfo.environmentId(),
                        CollectionUtils.stream(consoleNotification.getGroups()).collect(Collectors.toSet()),
                        input.definitionVersion(),
                        null,
                        false
                    )
                )
                .peek(sanitized -> consoleNotification.setGroups(CollectionUtils.stream(sanitized.groups()).toList()), errors::addAll);

            Set<String> allowedGroups = CollectionUtils.stream(input.allowedGroups()).collect(Collectors.toSet());
            Set<String> consoleNotificationGroup = CollectionUtils.stream(consoleNotification.getGroups()).collect(Collectors.toSet());
            if (!allowedGroups.containsAll(consoleNotificationGroup)) {
                errors.add(
                    Error.severe(
                        "Console notification configuration contains the following groups %s where only those are allowed %s",
                        input.portalNotificationConfig.getGroups(),
                        input.allowedGroups
                    )
                );
            }
            return Result.ofBoth(input, errors);
        }
        return Result.ofBoth(input, errors);
    }
}
