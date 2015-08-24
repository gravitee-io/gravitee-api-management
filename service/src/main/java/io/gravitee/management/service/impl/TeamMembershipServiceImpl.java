/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.management.service.impl;

import io.gravitee.management.model.MembershipEntity;
import io.gravitee.management.model.TeamRole;
import io.gravitee.management.service.TeamMembershipService;
import io.gravitee.management.service.exceptions.TeamNotFoundException;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.management.service.exceptions.UnknownMemberException;
import io.gravitee.repository.api.TeamMembershipRepository;
import io.gravitee.repository.api.TeamRepository;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.model.Member;
import io.gravitee.repository.model.Team;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Component
public class TeamMembershipServiceImpl implements TeamMembershipService {

    /**
     * Logger.
     */
    private final Logger LOGGER = LoggerFactory.getLogger(TeamMembershipServiceImpl.class);

    @Autowired
    private TeamMembershipRepository teamMembershipRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Override
    public void addOrUpdateMember(String teamName, String username, TeamRole teamRole) {
        try {
            LOGGER.debug("Add member {) in team {}", username, teamName);

            // Check if user is not already registered
            Member member = teamMembershipRepository.getMember(teamName, username);
            if (member == null) {
                member = new Member();
                member.setCreatedAt(new Date());
                member.setUpdatedAt(member.getCreatedAt());
                member.setUsername(username);
                member.setRole(convert(teamRole));

                teamMembershipRepository.addMember(teamName, member);
            } else {
                member.setUpdatedAt(new Date());
                member.setRole(convert(teamRole));
                teamMembershipRepository.updateMember(teamName, member);
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to add member {} in team {}", username, teamName, ex);
            throw new TechnicalManagementException("An error occurs while trying to add member " + username +
                    " in team " + teamName, ex);
        }
    }

    @Override
    public void deleteMember(String teamName, String username) {
        try {
            LOGGER.debug("Remove member {) from team {}", username, teamName);

            // Check if user is registered in team
            Member member = teamMembershipRepository.getMember(teamName, username);
            if (member != null) {
                teamMembershipRepository.deleteMember(teamName, username);
            } else {
                throw new UnknownMemberException(username);
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to remove member {} from team {}", username, teamName, ex);
            throw new TechnicalManagementException("An error occurs while trying to remove member " + username +
                    " from team " + teamName, ex);
        }
    }

    @Override
    public Set<MembershipEntity> findMembers(String teamName, TeamRole teamRole) {
        try {
            LOGGER.debug("Find members by team: {}", teamName);

            Optional<Team> team = teamRepository.findByName(teamName);

            if (team.isPresent()) {
                Set<Member> members = teamMembershipRepository.listMembers(teamName);
                Set<MembershipEntity> membershipEntities = new HashSet<>();

                if (teamRole == null) {
                    membershipEntities.addAll(members.stream().map(TeamMembershipServiceImpl::convert).collect(Collectors.toSet()));
                } else {
                    membershipEntities.addAll(members.stream().filter(new Predicate<Member>() {
                        @Override
                        public boolean test(Member member) {
                            return member.getRole().name().equalsIgnoreCase(teamRole.name());
                        }
                    }).map(TeamMembershipServiceImpl::convert).collect(Collectors.toSet()));
                }

                return membershipEntities;
            } else {
                throw new TeamNotFoundException(teamName);
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find members by team {}", teamName, ex);
            throw new TechnicalManagementException("An error occurs while trying to find members by team " + teamName, ex);
        }
    }

    private static MembershipEntity convert(Member member) {
        MembershipEntity membershipEntity = new MembershipEntity();

        membershipEntity.setMember(member.getUsername());
        membershipEntity.setRole(member.getRole().name());
        membershipEntity.setMemberSince(member.getCreatedAt());

        return membershipEntity;
    }

    private static io.gravitee.repository.model.TeamRole convert(TeamRole teamRole) {
        return io.gravitee.repository.model.TeamRole.valueOf(teamRole.name());
    }
}
