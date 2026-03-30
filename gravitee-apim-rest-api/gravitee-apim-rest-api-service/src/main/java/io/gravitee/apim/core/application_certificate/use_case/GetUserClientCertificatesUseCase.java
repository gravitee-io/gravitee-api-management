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
package io.gravitee.apim.core.application_certificate.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.application.domain_service.UserApplicationDomainService;
import io.gravitee.apim.core.application.query_service.ApplicationQueryService;
import io.gravitee.apim.core.application_certificate.crud_service.ClientCertificateCrudService;
import io.gravitee.apim.core.application_certificate.model.ClientCertificate;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.PageableImpl;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetUserClientCertificatesUseCase {

    private final ClientCertificateCrudService clientCertificateCrudService;
    private final UserApplicationDomainService userApplicationDomainService;
    private final ApplicationQueryService applicationQueryService;

    public Output execute(Input input) {
        Set<String> appIds = userApplicationDomainService.findApplicationIdsByUserId(input.userId());

        if (appIds.isEmpty()) {
            return new Output(new Page<>(List.of(), 0, 0, 0), List.of());
        }

        Page<ClientCertificate> certificates = clientCertificateCrudService.findByApplicationIds(appIds, input.pageable());

        List<BaseApplicationEntity> applications = List.of();
        if (input.includeApplications()) {
            applications = applicationQueryService
                .searchByIds(appIds, input.environmentId(), new PageableImpl(1, appIds.size()))
                .getContent();
        }

        return new Output(certificates, applications);
    }

    public record Input(String userId, String environmentId, Pageable pageable, boolean includeApplications) {}

    public record Output(Page<ClientCertificate> clientCertificates, List<BaseApplicationEntity> applications) {}
}
