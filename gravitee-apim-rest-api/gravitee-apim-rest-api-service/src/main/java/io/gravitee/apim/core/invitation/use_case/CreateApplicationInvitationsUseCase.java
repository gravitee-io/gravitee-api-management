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
import io.gravitee.apim.core.invitation.domain_service.CreateApplicationInvitationsDomainService;
import io.gravitee.apim.core.invitation.model.ApplicationInvitation;
import io.gravitee.apim.core.invitation.model.CreateApplicationInvitations;
import java.util.List;

@UseCase
public class CreateApplicationInvitationsUseCase {

    private final ApplicationCrudService applicationCrudService;
    private final CreateApplicationInvitationsDomainService createApplicationInvitationsDomainService;

    public CreateApplicationInvitationsUseCase(
        ApplicationCrudService applicationCrudService,
        CreateApplicationInvitationsDomainService createApplicationInvitationsDomainService
    ) {
        this.applicationCrudService = applicationCrudService;
        this.createApplicationInvitationsDomainService = createApplicationInvitationsDomainService;
    }

    public Output execute(Input input) {
        applicationCrudService.findById(input.applicationId(), input.environmentId());
        var createApplicationInvitations = input.createApplicationInvitations();

        return new Output(
            createApplicationInvitationsDomainService.create(
                input.organizationId(),
                input.environmentId(),
                input.applicationId(),
                createApplicationInvitations.recipientEmails(),
                createApplicationInvitations.roleName(),
                createApplicationInvitations.notifyUsers(),
                createApplicationInvitations.confirmationPageUrl()
            )
        );
    }

    public record Input(
        String organizationId,
        String environmentId,
        String applicationId,
        CreateApplicationInvitations createApplicationInvitations
    ) {}

    public record Output(List<ApplicationInvitation> invitations) {}
}
