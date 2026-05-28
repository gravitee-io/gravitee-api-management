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
package io.gravitee.apim.infra.query_service.invitation;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.invitation.model.ApplicationInvitation;
import io.gravitee.apim.core.invitation.model.InvitationReference;
import io.gravitee.apim.core.invitation.model.SearchApplicationInvitationsCriteria;
import io.gravitee.apim.core.invitation.query_service.InvitationQueryService;
import io.gravitee.apim.infra.adapter.InvitationAdapter;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.InvitationRepository;
import io.gravitee.repository.management.api.InvitationRepository.InvitationCriteria;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.api.search.builder.SortableBuilder;
import io.gravitee.repository.management.model.InvitationReferenceType;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.PageableImpl;
import jakarta.annotation.Nonnull;
import java.util.List;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class InvitationQueryServiceImpl implements InvitationQueryService {

    private final InvitationRepository invitationRepository;

    public InvitationQueryServiceImpl(@Lazy InvitationRepository invitationRepository) {
        this.invitationRepository = invitationRepository;
    }

    @Override
    public List<ApplicationInvitation> findByReference(@Nonnull InvitationReference reference) {
        try {
            return invitationRepository
                .findByReferenceIdAndReferenceType(reference.id(), InvitationReferenceType.valueOf(reference.type().name()))
                .stream()
                .map(InvitationAdapter.INSTANCE::toApplicationInvitation)
                .toList();
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("An error occurs while trying to find invitations by reference", e);
        }
    }

    @Override
    public Page<ApplicationInvitation> findByApplicationId(
        String applicationId,
        @Nonnull SearchApplicationInvitationsCriteria criteria,
        @Nonnull Pageable pageable
    ) {
        try {
            var resolvedPageable = pageable == null ? new PageableImpl(1, Integer.MAX_VALUE) : pageable;
            var repositoryPage = invitationRepository.search(
                new InvitationCriteria(
                    applicationId,
                    InvitationReferenceType.APPLICATION,
                    criteria != null ? criteria.email().orElse(null) : null
                ),
                new SortableBuilder().field("email").order(Order.ASC).build(),
                new PageableBuilder().pageNumber(resolvedPageable.getPageNumber() - 1).pageSize(resolvedPageable.getPageSize()).build()
            );

            var content = repositoryPage.getContent().stream().map(InvitationAdapter.INSTANCE::toApplicationInvitation).toList();

            return new Page<>(content, resolvedPageable.getPageNumber(), content.size(), repositoryPage.getTotalElements());
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("An error occurs while trying to find application invitations", e);
        }
    }
}
