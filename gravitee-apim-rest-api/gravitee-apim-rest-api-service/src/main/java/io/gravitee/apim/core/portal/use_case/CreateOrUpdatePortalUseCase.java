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
package io.gravitee.apim.core.portal.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.portal.crud_service.PortalCrudService;
import io.gravitee.apim.core.portal.domain_service.PortalNavigationListingDomainService;
import io.gravitee.apim.core.portal.domain_service.PortalNavigationSyncDomainService;
import io.gravitee.apim.core.portal.domain_service.ValidatePortalDomainService;
import io.gravitee.apim.core.portal.model.NavigationPath;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.validation.Validator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class CreateOrUpdatePortalUseCase {

    private final ValidatePortalDomainService validator;
    private final PortalCrudService portalCrudService;
    private final PortalNavigationSyncDomainService portalNavigationSyncDomainService;
    private final PortalNavigationListingDomainService portalNavigationListingDomainService;

    public record Input(AuditInfo auditInfo, Portal portal, List<NavigationPath> navigation) {
        public Input(AuditInfo auditInfo, Portal portal) {
            this(auditInfo, portal, List.of());
        }
    }

    public record Output(Portal portal, List<NavigationPath> navigation, List<Validator.Error> errors) {}

    public Output execute(Input input) {
        var validation = validator.validateAndSanitize(
            new ValidatePortalDomainService.Input(input.auditInfo(), input.portal(), input.navigation())
        );

        validation
            .severe()
            .ifPresent(errors -> {
                throw new ValidationDomainException(errors.stream().map(Validator.Error::getMessage).collect(Collectors.joining(", ")));
            });

        var warnings = validation.warning().orElseGet(List::of);

        var portal = input.portal();
        var existing = portalCrudService.findByIdAndEnvironmentId(portal.getId(), input.auditInfo().environmentId());
        var saved = existing.isPresent() ? portalCrudService.update(portal) : portalCrudService.create(portal);
        portalNavigationSyncDomainService.sync(input.auditInfo(), portal.getId(), input.navigation());
        var navigation = portalNavigationListingDomainService.listAsNavigationPaths(input.auditInfo().environmentId());
        return new Output(saved, navigation, warnings);
    }
}
