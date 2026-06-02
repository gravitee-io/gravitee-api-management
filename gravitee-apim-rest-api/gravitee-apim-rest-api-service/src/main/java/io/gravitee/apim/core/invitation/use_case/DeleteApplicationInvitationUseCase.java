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
import io.gravitee.apim.core.invitation.domain_service.DeleteApplicationInvitationDomainService;
import io.gravitee.apim.core.invitation.model.InvitationId;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class DeleteApplicationInvitationUseCase {

    private final ApplicationCrudService applicationCrudService;
    private final DeleteApplicationInvitationDomainService deleteApplicationInvitationDomainService;

    public void execute(Input input) {
        applicationCrudService.findById(input.applicationId(), input.environmentId());
        deleteApplicationInvitationDomainService.delete(input.applicationId(), input.invitationId());
    }

    public record Input(String environmentId, String applicationId, InvitationId invitationId) {}
}
