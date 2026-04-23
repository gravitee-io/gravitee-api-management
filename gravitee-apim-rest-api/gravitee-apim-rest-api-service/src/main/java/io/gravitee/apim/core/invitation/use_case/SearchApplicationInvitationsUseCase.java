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
package io.gravitee.apim.core.invitation.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.application.crud_service.ApplicationCrudService;
import io.gravitee.apim.core.invitation.model.ApplicationInvitationItem;
import io.gravitee.apim.core.invitation.model.SearchApplicationInvitationsCriteria;
import io.gravitee.apim.core.invitation.query_service.InvitationQueryService;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.service.common.ExecutionContext;

@UseCase
public class SearchApplicationInvitationsUseCase {

    private final ApplicationCrudService applicationCrudService;
    private final InvitationQueryService invitationQueryService;

    public SearchApplicationInvitationsUseCase(
        ApplicationCrudService applicationCrudService,
        InvitationQueryService invitationQueryService
    ) {
        this.applicationCrudService = applicationCrudService;
        this.invitationQueryService = invitationQueryService;
    }

    public Output execute(Input input) {
        applicationCrudService.findById(input.applicationId(), input.executionContext().getEnvironmentId());

        return new Output(invitationQueryService.findByApplicationId(input.applicationId(), input.criteria(), input.pageable()));
    }

    public record Input(
        ExecutionContext executionContext,
        String applicationId,
        SearchApplicationInvitationsCriteria criteria,
        Pageable pageable
    ) {}

    public record Output(Page<ApplicationInvitationItem> invitations) {}
}
