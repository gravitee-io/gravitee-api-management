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
package io.gravitee.management.service;

import io.gravitee.management.model.MembershipEntity;
import io.gravitee.management.model.TeamRole;
import io.gravitee.management.service.exceptions.TeamNotFoundException;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.management.service.exceptions.UnknownMemberException;
import io.gravitee.management.service.impl.TeamMembershipServiceImpl;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.TeamMembershipRepository;
import io.gravitee.repository.management.api.TeamRepository;
import io.gravitee.repository.management.model.Member;
import io.gravitee.repository.management.model.Team;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Azize Elamrani (azize dot elamrani at gmail dot com)
 */
@RunWith(MockitoJUnitRunner.class)
public class TeamMembershipServiceTest {

    private static final String USER_NAME = "tuser";
    private static final String MEMBER_NAME = "mname";
    private static final String TEAM_NAME = "team";
    private static final String TEAM_ROLE_NAME = "MEMBER";

    @InjectMocks
    private TeamMembershipService teamMembershipService = new TeamMembershipServiceImpl();

    @Mock
    private TeamMembershipRepository teamMembershipRepository;
    @Mock
    private TeamRepository teamRepository;

    private TeamRole teamRole = TeamRole.MEMBER;
    @Mock
    private Member member;
    @Mock
    private Team team;

    @Test
    public void shouldAddMember() throws TechnicalException {
        when(teamMembershipRepository.getMember(TEAM_NAME, USER_NAME)).thenReturn(null);

        teamMembershipService.addOrUpdateMember(TEAM_NAME, USER_NAME, teamRole);

        verify(teamMembershipRepository).addMember(eq(TEAM_NAME), argThat(new ArgumentMatcher<Member>() {
            public boolean matches(Object argument) {
                final Member memberToAdd = (Member) argument;
                return USER_NAME.equals(memberToAdd.getUsername()) &&
                    TEAM_ROLE_NAME.equals(memberToAdd.getRole().name()) &&
                    memberToAdd.getCreatedAt() != null &&
                    memberToAdd.getUpdatedAt() != null &&
                    memberToAdd.getCreatedAt().equals(memberToAdd.getUpdatedAt());
            }
        }));
    }

    @Test
    public void shouldUpdateMember() throws TechnicalException {
        when(teamMembershipRepository.getMember(TEAM_NAME, USER_NAME)).thenReturn(member);

        teamMembershipService.addOrUpdateMember(TEAM_NAME, USER_NAME, teamRole);

        verify(member).setUpdatedAt(any());
        verify(member).setRole(io.gravitee.repository.management.model.TeamRole.MEMBER);
        verify(teamMembershipRepository).updateMember(TEAM_NAME, member);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotAddOrUpdateMember() throws TechnicalException {
        when(teamMembershipRepository.getMember(TEAM_NAME, USER_NAME)).thenThrow(TechnicalException.class);

        teamMembershipService.addOrUpdateMember(TEAM_NAME, USER_NAME, teamRole);
    }

    @Test
    public void shouldDeleteMember() throws TechnicalException {
        when(teamMembershipRepository.getMember(TEAM_NAME, USER_NAME)).thenReturn(member);

        teamMembershipService.deleteMember(TEAM_NAME, USER_NAME);

        verify(teamMembershipRepository).deleteMember(TEAM_NAME, USER_NAME);
    }

    @Test(expected = UnknownMemberException.class)
    public void shouldNotDeleteMemberBecauseNotExists() throws TechnicalException {
        when(teamMembershipRepository.getMember(TEAM_NAME, USER_NAME)).thenReturn(null);

        teamMembershipService.deleteMember(TEAM_NAME, USER_NAME);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotDeleteMemberBecauseTechnicalException() throws TechnicalException {
        when(teamMembershipRepository.getMember(TEAM_NAME, USER_NAME)).thenThrow(TechnicalException.class);

        teamMembershipService.deleteMember(TEAM_NAME, USER_NAME);
    }

    @Test
    public void shouldFindMembers() throws TechnicalException {
        when(teamRepository.findByName(TEAM_NAME)).thenReturn(Optional.of(team));
        when(member.getUsername()).thenReturn(MEMBER_NAME);
        when(member.getRole()).thenReturn(io.gravitee.repository.management.model.TeamRole.MEMBER);
        when(teamMembershipRepository.listMembers(TEAM_NAME)).thenReturn(new HashSet<>(Arrays.asList(member)));

        final Set<MembershipEntity> members = teamMembershipService.findMembers(TEAM_NAME, teamRole);

        assertNotNull(members);
        assertEquals(MEMBER_NAME, members.iterator().next().getMember());
    }

    @Test
    public void shouldNotFindMembersBecauseTeamRoleNotMatch() throws TechnicalException {
        when(teamRepository.findByName(TEAM_NAME)).thenReturn(Optional.of(team));
        when(member.getUsername()).thenReturn(MEMBER_NAME);
        when(member.getRole()).thenReturn(io.gravitee.repository.management.model.TeamRole.ADMIN);
        when(teamMembershipRepository.listMembers(TEAM_NAME)).thenReturn(new HashSet<>(Arrays.asList(member)));

        final Set<MembershipEntity> members = teamMembershipService.findMembers(TEAM_NAME, teamRole);

        assertNotNull(members);
        assertTrue(members.isEmpty());
    }

    @Test(expected = TeamNotFoundException.class)
    public void shouldNotFindMembersBecauseTeamNotFound() throws TechnicalException {
        when(teamRepository.findByName(TEAM_NAME)).thenReturn(Optional.empty());

        teamMembershipService.findMembers(TEAM_NAME, teamRole);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotFindMembersBecauseTechnicalException() throws TechnicalException {
        when(teamRepository.findByName(TEAM_NAME)).thenThrow(TechnicalException.class);

        teamMembershipService.findMembers(TEAM_NAME, teamRole);
    }
}
