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
import io.gravitee.apim.core.member.exception.UnsupportedMembershipReferencer;
import io.gravitee.apim.core.member.model.MembershipReferenceType;
import io.gravitee.apim.core.member.model.crd.MemberCRD;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.model.Role;
import io.gravitee.apim.core.membership.query_service.MembershipQueryService;
import io.gravitee.apim.core.membership.query_service.RoleQueryService;
import io.gravitee.apim.core.user.domain_service.UserDomainService;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.rest.api.service.common.ReferenceContext;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DomainService
@RequiredArgsConstructor
public class ValidateCRDMembersDomainService implements Validator<ValidateCRDMembersDomainService.Input> {

    private final UserDomainService userDomainService;
    private final RoleQueryService roleQueryService;
    private final MembershipQueryService membershipQueryService;

    public record Input(String organizationId, String referenceId, MembershipReferenceType referenceType, Set<MemberCRD> members)
        implements Validator.Input {
        Input sanitized(Set<MemberCRD> sanitizedMembers) {
            return new Input(organizationId, referenceId, referenceType, sanitizedMembers);
        }
    }

    @Override
    public Result<Input> validateAndSanitize(Input input) {
        var sanitizedMembers = input.members == null ? Set.<MemberCRD>of() : new HashSet<>(input.members);
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
                .findBySource(input.organizationId, member.getSource(), member.getSourceId())
                .ifPresentOrElse(
                    user -> member.setId(user.getId()),
                    () -> {
                        errors.add(
                            Error.warning(
                                "member [%s] of source [%s] could not be found in organization [%s]",
                                member.getSourceId(),
                                member.getSource(),
                                input.organizationId()
                            )
                        );
                        members.remove();
                    }
                );
        }
    }

    private void validateMemberRole(Input input, Set<MemberCRD> sanitized, ArrayList<Error> errors) {
        var members = sanitized.iterator();
        while (members.hasNext()) {
            var member = members.next();
            Role role = null;
            switch (input.referenceType) {
                case APPLICATION:
                    Optional<Role> applicationRole = roleQueryService.findApplicationRole(
                        member.getRole(),
                        new ReferenceContext(ORGANIZATION, input.organizationId())
                    );
                    if (applicationRole.isPresent()) {
                        role = applicationRole.get();
                    }
                    break;
                case API:
                    Optional<Role> apiRole = roleQueryService.findApiRole(
                        member.getRole(),
                        new ReferenceContext(ORGANIZATION, input.organizationId())
                    );
                    if (apiRole.isPresent()) {
                        role = apiRole.get();
                    }
                    break;
                default:
                    throw new UnsupportedMembershipReferencer(
                        String.format("membership reference is not supported [%s]", input.referenceType)
                    );
            }
            if (role == null) {
                errors.add(Error.warning("member role [%s] doesn't exist", member.getRole()));
            }
        }
    }

    private void validatePrimaryOwner(Input input, Set<MemberCRD> sanitized, ArrayList<Error> errors) {
        Optional<Membership> membership;
        switch (input.referenceType) {
            case APPLICATION -> {
                String poRole = roleQueryService
                    .findApplicationRole(PRIMARY_OWNER.name(), new ReferenceContext(ORGANIZATION, input.organizationId))
                    .orElseThrow(() -> new RuntimeException("application primary owner role not found"))
                    .getId();
                membership =
                    membershipQueryService
                        .findByReference(Membership.ReferenceType.APPLICATION, input.referenceId)
                        .stream()
                        .filter(m -> m.getRoleId().equals(poRole))
                        .findFirst();
            }
            case API -> {
                String poRole = roleQueryService
                    .findApiRole(PRIMARY_OWNER.name(), new ReferenceContext(ORGANIZATION, input.organizationId))
                    .orElseThrow(() -> new RuntimeException("api primary owner role not found"))
                    .getId();
                membership =
                    membershipQueryService
                        .findByReference(Membership.ReferenceType.API, input.referenceId)
                        .stream()
                        .filter(m -> m.getRoleId().equals(poRole))
                        .findFirst();
            }
            default -> throw new UnsupportedMembershipReferencer(
                String.format("membership reference type [%s] doesn't exist", input.referenceType.name())
            );
        }

        var members = sanitized.iterator();
        while (members.hasNext()) {
            var member = members.next();
            if (PRIMARY_OWNER.name().equals(member.getRole())) {
                errors.add(Error.severe("setting a member with the primary owner role is not allowed"));
                members.remove();
            }

            if (membership.isPresent() && membership.get().getMemberId().equals(member.getId())) {
                errors.add(Error.severe("can not change the role of exiting primary owner [%s]", member.getSourceId()));
                members.remove();
            }
        }
    }
}
