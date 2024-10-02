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
package io.gravitee.apim.core.member.domain_service;

import static io.gravitee.apim.core.member.model.SystemRole.PRIMARY_OWNER;
import static io.gravitee.rest.api.service.common.ReferenceContext.Type.ORGANIZATION;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.member.model.MembershipReferenceType;
import io.gravitee.apim.core.member.model.crd.MemberCRD;
import io.gravitee.apim.core.membership.model.Role;
import io.gravitee.apim.core.membership.query_service.RoleQueryService;
import io.gravitee.apim.core.user.domain_service.UserDomainService;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.rest.api.service.common.ReferenceContext;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@DomainService
@RequiredArgsConstructor
public class ValidateCRDMembersDomainService implements Validator<ValidateCRDMembersDomainService.Input> {

    private final UserDomainService userDomainService;
    private final RoleQueryService roleQueryService;

    public record Input(AuditInfo auditInfo, String referenceId, MembershipReferenceType referenceType, Set<MemberCRD> members)
        implements Validator.Input {
        Input sanitized(Set<MemberCRD> sanitizedMembers) {
            return new Input(auditInfo, referenceId, referenceType, sanitizedMembers);
        }
    }

    @Override
    public Result<Input> validateAndSanitize(Input input) {
        var sanitizedMembers = input.members == null ? new HashSet<MemberCRD>() : new HashSet<>(input.members);
        var errors = new ArrayList<Error>();

        validateAndSanitizeMemberId(input, sanitizedMembers, errors);
        validateMemberRole(input, sanitizedMembers, errors);
        validatePrimaryOwner(input, sanitizedMembers, errors);

        return Result.ofBoth(input.sanitized(sanitizedMembers), errors);
    }

    private void validateAndSanitizeMemberId(Input input, Set<MemberCRD> sanitized, ArrayList<Error> errors) {
        var members = sanitized.iterator();
        while (members.hasNext()) {
            var member = members.next();
            userDomainService
                .findBySource(input.auditInfo.organizationId(), member.getSource(), member.getSourceId())
                .ifPresentOrElse(
                    user -> member.setId(user.getId()),
                    () -> {
                        errors.add(
                            Error.warning(
                                "member [%s] of source [%s] could not be found in organization [%s]",
                                member.getSourceId(),
                                member.getSource(),
                                input.auditInfo.organizationId()
                            )
                        );
                        members.remove();
                    }
                );
        }
    }

    private void validateMemberRole(Input input, Set<MemberCRD> sanitized, ArrayList<Error> errors) {
        for (var member : sanitized) {
            findRole(input.auditInfo.organizationId(), input.referenceType, member.getRole())
                .ifPresentOrElse(
                    role -> log.debug("Role {} found for scope {}", member.getRole(), input.referenceType),
                    () -> errors.add(Error.warning("member role [%s] doesn't exist", member.getRole()))
                );
        }
    }

    private Optional<Role> findRole(String organizationId, MembershipReferenceType scope, String role) {
        var context = new ReferenceContext(ORGANIZATION, organizationId);
        return switch (scope) {
            case API -> roleQueryService.findApiRole(role, context);
            case APPLICATION -> roleQueryService.findApplicationRole(role, context);
            default -> throw new TechnicalDomainException(String.format("Role scope [%s] is not supported", scope));
        };
    }

    private void validatePrimaryOwner(Input input, Set<MemberCRD> sanitized, ArrayList<Error> errors) {
        var actor = input.auditInfo.actor();
        var members = sanitized.iterator();

        while (members.hasNext()) {
            var member = members.next();

            log.debug("checking that member {} is not defined as a primary owner", member.getSourceId());

            if (PRIMARY_OWNER.name().equals(member.getRole())) {
                errors.add(Error.severe("setting a member with the primary owner role is not allowed"));
                members.remove();
                return;
            }

            log.debug("checking that member {} is not the authenticated user who will be set as a primary owner", member.getSourceId());

            if (actor.userId().equals(member.getId())) {
                errors.add(Error.severe("can not change the role of primary owner [%s]", member.getSourceId()));
                members.remove();
            }
        }
    }
}
