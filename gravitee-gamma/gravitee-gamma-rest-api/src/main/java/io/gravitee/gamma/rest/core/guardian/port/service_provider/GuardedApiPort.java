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
package io.gravitee.gamma.rest.core.guardian.port.service_provider;

import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.common.data.domain.Page;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.gamma.rest.core.guardian.exception.ApiInternalFlowChangedException;
import io.gravitee.gamma.rest.core.guardian.model.ApiReference;
import io.gravitee.gamma.rest.core.guardian.model.GuardedApi;
import java.util.Optional;

/**
 * The APIs Guardians are enabled on. The APIM API store is an external service from this domain's point of view.
 *
 * <p>Every call is scoped by environment, reads and the write alike, so an API of another environment answers as an
 * unknown one.
 *
 * @author GraviteeSource Team
 */
public interface GuardedApiPort {
    Optional<GuardedApi> findById(String environmentId, String apiId);

    /**
     * One page of the APIs of the environment whose definition runs this Guardian, by name.
     *
     * @param pageNumber 1-based
     */
    Page<ApiReference> findByGuardian(String environmentId, String guardianId, int pageNumber, int pageSize);

    /**
     * Replaces the internal flow of an API that has one, and sends that change to the gateways by the same call —
     * only that change, not whatever else was saved on the API without being deployed.
     *
     * <p>An API the gateways do not run — never deployed, stopped or unpublished — is written and nothing is sent: it
     * stays out of sync, and its publisher's next deployment is what carries the Guardians over.
     *
     * @param auditInfo who is calling and from where; the environment of the call is read from it, so an API of
     *     another environment answers as an unknown one.
     * @param expectedInternalFlow the flow {@code internalFlow} was computed from, the one the caller read. The write
     *     happens only while the API still holds it, so two callers editing the same API cannot overwrite one another.
     * @throws ApiInternalFlowChangedException when the API no longer holds {@code expectedInternalFlow}.
     * @throws io.gravitee.apim.core.api.exception.ApiNotFoundException when the environment holds no such API.
     * @throws io.gravitee.apim.core.exception.TechnicalDomainException when what the gateways run cannot be read, so
     *     the change cannot be sent to them. Nothing is written then — neither the internal flow nor the event.
     */
    GuardedApi updateInternalFlow(AuditInfo auditInfo, String apiId, Flow expectedInternalFlow, Flow internalFlow);
}
