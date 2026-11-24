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
package io.gravitee.gamma.rest.core.guardrail.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.common.data.domain.Page;
import io.gravitee.gamma.rest.core.guardrail.exception.GuardrailNotFoundException;
import io.gravitee.gamma.rest.core.guardrail.model.ApiReference;
import io.gravitee.gamma.rest.core.guardrail.port.repository.GuardrailRepository;
import io.gravitee.gamma.rest.core.guardrail.port.service_provider.GuardedApiPort;
import io.gravitee.gamma.rest.core.guardrail.port.service_provider.GuardedApiReadPermissionPort;
import lombok.RequiredArgsConstructor;

/**
 * Lists the APIs of the environment a Guardrail runs on, by name: those whose definition names it. Paged, like every
 * list of this surface: a Guardrail of an environment may run on as many APIs as the environment holds.
 *
 * <p>Only the APIs the caller may read are named. Holding the Guardrail permission of an environment is not a right to
 * read every API of it, so a page drops the APIs the caller has no {@code API_DEFINITION[READ]} on — the count of
 * APIs running the Guardrail is still the whole count, which is why a page can hold fewer rows than it could.
 *
 * @author GraviteeSource Team
 */
@UseCase
@RequiredArgsConstructor
public class ListGuardrailApisUseCase {

    private static final int DEFAULT_PAGE_NUMBER = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    private final GuardrailRepository guardrailRepository;
    private final GuardedApiPort apis;
    private final GuardedApiReadPermissionPort permissions;

    public Output execute(Input input) {
        var environmentId = input.auditInfo().environmentId();
        var guardrail = guardrailRepository
            .findByEnvironmentId(environmentId, input.guardrailId())
            .orElseThrow(() -> new GuardrailNotFoundException(input.guardrailId()));
        var pageNumber = input.pageNumber() > 0 ? input.pageNumber() : DEFAULT_PAGE_NUMBER;
        var size = input.pageSize() > 0 ? Math.min(input.pageSize(), MAX_PAGE_SIZE) : DEFAULT_PAGE_SIZE;
        var page = apis.findByGuardrail(environmentId, guardrail.getId(), pageNumber, size);
        var readable = page
            .getContent()
            .stream()
            .filter(api -> canRead(input.auditInfo(), api))
            .toList();
        return new Output(new Page<>(readable, page.getPageNumber(), readable.size(), page.getTotalElements()), size);
    }

    /**
     * Named only to a caller who may read it, and to no one when the caller is not identified: an unnamed caller
     * holds no permission to check an API against, so the page it gets is empty rather than whole. {@code userId} is
     * null for a request whose principal carries no user, which is what the REST layer answers for one it cannot
     * name.
     */
    private boolean canRead(AuditInfo auditInfo, ApiReference api) {
        var userId = auditInfo.actor() == null ? null : auditInfo.actor().userId();
        return userId != null && permissions.canRead(auditInfo.organizationId(), userId, api.id());
    }

    /** @param pageNumber 1-based; the first page when not positive */
    public record Input(AuditInfo auditInfo, String guardrailId, int pageNumber, int pageSize) {}

    /**
     * @param apis the APIs of the page the caller may read; {@code getTotalElements} counts every API running the
     *     Guardrail, read or not.
     * @param perPage the page size actually served
     */
    public record Output(Page<ApiReference> apis, int perPage) {}
}
