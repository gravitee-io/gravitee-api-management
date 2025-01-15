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
package io.gravitee.apim.infra.domain_service.group;

import io.gravitee.apim.core.group.domain_service.ValidateGroupCRDDomainService;
import io.gravitee.apim.core.group.model.crd.GroupCRDSpec;
import io.gravitee.apim.core.member.domain_service.ValidateCRDMembersDomainService;
import io.gravitee.apim.core.member.model.MembershipReferenceType;
import io.gravitee.apim.core.member.model.RoleScope;
import io.gravitee.apim.core.member.model.crd.MemberCRD;
import io.gravitee.apim.core.utils.CollectionUtils;
import io.gravitee.apim.infra.adapter.GroupCRDAdapter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Service
@RequiredArgsConstructor
public class ValidateGroupCRDDomainServiceImpl implements ValidateGroupCRDDomainService {

    private final ValidateCRDMembersDomainService membersValidator;

    @Override
    public Result<ValidateGroupCRDDomainService.Input> validateAndSanitize(ValidateGroupCRDDomainService.Input input) {
        var errors = new ArrayList<Error>();

        validateAndSanitizeMembers(input).peek(members -> input.spec().setMembers(members), errors::addAll);

        return Result.ofBoth(input, errors);
    }

    private Result<Set<GroupCRDSpec.Member>> validateAndSanitizeMembers(ValidateGroupCRDDomainService.Input input) {
        if (CollectionUtils.isEmpty(input.spec().getMembers())) {
            return Result.ofBoth(Set.of(), List.of());
        }

        var apiMembers = GroupCRDAdapter.INSTANCE.toApiMemberCRDSet(input.spec().getMembers());
        var applicationMembers = GroupCRDAdapter.INSTANCE.toApplicationMemberCRDSet(input.spec().getMembers());
        var integrationMembers = GroupCRDAdapter.INSTANCE.toIntegrationMemberCRDSet(input.spec().getMembers());

        var sanitizedGroupMembers = new HashMap<String, GroupCRDSpec.Member>();
        var errors = new ArrayList<Error>();

        membersValidator
            .validateAndSanitize(new ValidateCRDMembersDomainService.Input(input.auditInfo(), MembershipReferenceType.API, apiMembers))
            .peek(output -> groupMembersById(RoleScope.API, output.members(), sanitizedGroupMembers), errors::addAll);

        membersValidator
            .validateAndSanitize(
                new ValidateCRDMembersDomainService.Input(input.auditInfo(), MembershipReferenceType.APPLICATION, applicationMembers)
            )
            .peek(output -> groupMembersById(RoleScope.APPLICATION, output.members(), sanitizedGroupMembers), errors::addAll);

        membersValidator
            .validateAndSanitize(
                new ValidateCRDMembersDomainService.Input(input.auditInfo(), MembershipReferenceType.INTEGRATION, integrationMembers)
            )
            .peek(output -> groupMembersById(RoleScope.INTEGRATION, output.members(), sanitizedGroupMembers), errors::addAll);

        return Result.ofBoth(new HashSet<>(sanitizedGroupMembers.values()), errors);
    }

    private static void groupMembersById(RoleScope roleScope, Set<MemberCRD> members, HashMap<String, GroupCRDSpec.Member> groups) {
        for (var member : members) {
            groups.computeIfPresent(member.getId(), (id, groupMember) -> addMemberRole(roleScope, groupMember, member));
            groups.computeIfAbsent(member.getId(), id -> initGroupMember(roleScope, member));
        }
    }

    private static GroupCRDSpec.Member addMemberRole(RoleScope roleScope, GroupCRDSpec.Member groupMember, MemberCRD member) {
        groupMember.getRoles().put(roleScope, member.getRole());
        return groupMember;
    }

    private static GroupCRDSpec.Member initGroupMember(RoleScope roleScope, MemberCRD member) {
        return GroupCRDSpec.Member
            .builder()
            .id(member.getId())
            .source(member.getSource())
            .sourceId(member.getSourceId())
            .roles(initMemberRoles(roleScope, member.getRole()))
            .build();
    }

    private static Map<RoleScope, String> initMemberRoles(RoleScope roleScope, String roleName) {
        var roles = new EnumMap<RoleScope, String>(RoleScope.class);
        roles.put(roleScope, roleName);
        return roles;
    }
}
