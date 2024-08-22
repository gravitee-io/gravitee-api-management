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

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.member.model.crd.MemberCRD;
import io.gravitee.apim.core.user.domain_service.UserDomainService;
import io.gravitee.apim.core.validation.Validator;
import java.util.ArrayList;
import java.util.HashSet;
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

    public record Input(String organizationId, Set<MemberCRD> members) implements Validator.Input {
        Input sanitized(Set<MemberCRD> sanitizedMembers) {
            return new Input(organizationId, sanitizedMembers);
        }
    }

    @Override
    public Result<Input> validateAndSanitize(Input input) {
        var givenMembers = input.members == null ? Set.<MemberCRD>of() : input.members;
        var sanitized = new HashSet<MemberCRD>();
        var errors = new ArrayList<Error>();

        for (var member : givenMembers) {
            userDomainService
                .findBySource(input.organizationId, member.getSource(), member.getSourceId())
                .ifPresentOrElse(
                    user -> sanitized.add(member.toBuilder().id(user.getId()).build()),
                    () ->
                        errors.add(
                            Error.warning(
                                "Member [%s] of source [%s] could not be found in organization [%s]",
                                member.getSourceId(),
                                member.getSource(),
                                input.organizationId()
                            )
                        )
                );
        }
        return Result.ofBoth(input.sanitized(sanitized), errors);
    }
}
