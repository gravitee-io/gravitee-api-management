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
package io.gravitee.apim.core.integration.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.NotAllowedDomainException;
import io.gravitee.apim.core.integration.crud_service.AsyncJobCrudService;
import io.gravitee.apim.core.integration.crud_service.IntegrationCrudService;
import io.gravitee.apim.core.integration.exception.IntegrationNotFoundException;
import io.gravitee.apim.core.integration.model.AsyncJob;
import io.gravitee.apim.core.integration.model.Integration;
import io.gravitee.apim.core.integration.service_provider.IntegrationAgent;
import io.gravitee.apim.core.license.domain_service.LicenseDomainService;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.service.common.UuidString;
import io.reactivex.rxjava3.core.Single;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@Slf4j
@RequiredArgsConstructor
public class StartIngestIntegrationApisUseCase {

    private final IntegrationCrudService integrationCrudService;
    private final AsyncJobCrudService asyncJobCrudService;
    private final IntegrationAgent integrationAgent;
    private final LicenseDomainService licenseDomainService;

    public Single<AsyncJob.Status> execute(Input input) {
        var auditInfo = input.auditInfo;
        var integrationId = input.integrationId;
        var organizationId = auditInfo.organizationId();
        var environmentId = auditInfo.environmentId();

        if (!licenseDomainService.isFederationFeatureAllowed(organizationId)) {
            return Single.error(NotAllowedDomainException.noLicenseForFederation());
        }

        return Single
            .fromCallable(() ->
                integrationCrudService
                    .findById(integrationId)
                    .filter(integration -> integration.getEnvironmentId().equals(environmentId))
                    .orElseThrow(() -> new IntegrationNotFoundException(integrationId))
            )
            .flatMap(integration ->
                integrationAgent
                    .startIngest(integration.getId(), UuidString.generateRandom(), input.apiIds())
                    .map(ingestStarted -> {
                        log.info("Ingestion started for integration {}", integration.getId());

                        if (ingestStarted.total() == 0) {
                            log.info("No APIs to ingest for integration {}", integration.getId());
                            return AsyncJob.Status.SUCCESS;
                        }

                        asyncJobCrudService.create(
                            newIngestJob(ingestStarted.ingestJobId(), integration, auditInfo.actor().userId(), ingestStarted.total())
                        );

                        return AsyncJob.Status.PENDING;
                    })
            )
            .doOnError(throwable -> log.error("Error to start ingest {}", integrationId, throwable));
    }

    public AsyncJob newIngestJob(String id, Integration integration, String initiatorId, Long total) {
        var now = TimeProvider.now();
        return AsyncJob
            .builder()
            .id(id)
            .sourceId(integration.getId())
            .environmentId(integration.getEnvironmentId())
            .initiatorId(initiatorId)
            .status(AsyncJob.Status.PENDING)
            .upperLimit(total)
            .createdAt(now)
            .updatedAt(now)
            .build();
    }

    public record Input(String integrationId, List<String> apiIds, AuditInfo auditInfo) {}
}
