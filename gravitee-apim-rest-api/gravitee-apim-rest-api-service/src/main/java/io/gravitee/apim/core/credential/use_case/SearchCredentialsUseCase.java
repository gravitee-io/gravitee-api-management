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
package io.gravitee.apim.core.credential.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.credential.domain_service.CredentialOwnerDomainService;
import io.gravitee.apim.core.credential.model.Credential;
import io.gravitee.apim.core.credential.model.CredentialActor;
import io.gravitee.apim.core.credential.model.CredentialQuery;
import io.gravitee.apim.core.credential.query_service.CredentialQueryService;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.common.Pageable;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class SearchCredentialsUseCase {

    private final CredentialQueryService credentialQueryService;
    private final CredentialOwnerDomainService credentialOwnerDomainService;

    public record Input(CredentialActor actor, CredentialQuery query, Pageable pageable) {}

    /** @param ownerNames the display name of each owner on the page that is still a user, by user id */
    public record Output(Page<Credential> page, Map<String, String> ownerNames) {}

    public Output execute(Input input) {
        var pageable = input.pageable();
        if ((long) (pageable.getPageNumber() - 1) * pageable.getPageSize() > Integer.MAX_VALUE) {
            throw new ValidationDomainException(
                "The page is out of range: (page - 1) * perPage must not exceed " + Integer.MAX_VALUE + "."
            );
        }
        var actor = input.actor();
        var page = credentialQueryService.search(actor.organizationId(), actor.environmentId(), input.query(), input.pageable());
        var ownerIds = page.getContent().stream().map(Credential::ownerId).toList();
        return new Output(page, credentialOwnerDomainService.namesOf(actor, ownerIds));
    }
}
