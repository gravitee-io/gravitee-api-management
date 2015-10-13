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

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import io.gravitee.management.model.NewTeamEntity;
import io.gravitee.management.model.TeamEntity;
import io.gravitee.management.model.UpdateTeamEntity;
import io.gravitee.management.service.exceptions.TeamAlreadyExistsException;
import io.gravitee.management.service.exceptions.TeamNotFoundException;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.management.service.impl.TeamServiceImpl;
import io.gravitee.repository.api.management.TeamMembershipRepository;
import io.gravitee.repository.api.management.TeamRepository;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.model.management.Member;
import io.gravitee.repository.model.management.Team;
import io.gravitee.repository.model.management.TeamRole;

/**
 * @author Azize Elamrani (azize dot elamrani at gmail dot com)
 */
@RunWith(MockitoJUnitRunner.class)
public class TeamServiceTest {

    private static final String USER_NAME = "tuser";
    private static final String TEAM_NAME = "team";
    private static final String EMAIL = "team@gravitee.io";
    private static final String DESCRIPTION = "Team description";
    private static final String OWNER = "owner";
    private static final TeamRole OWNER_ROLE = TeamRole.ADMIN;

    @InjectMocks
    private TeamService teamService = new TeamServiceImpl();

    @Mock
    private TeamRepository teamRepository;
    @Mock
    private TeamMembershipRepository teamMembershipRepository;

    @Mock
    private NewTeamEntity newTeam;
    @Mock
    private UpdateTeamEntity existingTeam;
    @Mock
    private Team team;
    @Mock
    private Date date;

    @Test
    public void shouldFindByName() throws TechnicalException {
        when(team.getName()).thenReturn(TEAM_NAME);
        when(teamRepository.findByName(TEAM_NAME)).thenReturn(Optional.of(team));

        teamService.findByName(TEAM_NAME);

        verify(teamRepository).findByName(TEAM_NAME);
    }

    @Test
    public void shouldNotFindByNameBecauseNotExists() throws TechnicalException {
        when(teamRepository.findByName(TEAM_NAME)).thenReturn(Optional.empty());

        final Optional<TeamEntity> optionalTeam = teamService.findByName(TEAM_NAME);
        assertFalse(optionalTeam.isPresent());
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotFindByNameBecauseTechnicalException() throws TechnicalException {
        when(teamRepository.findByName(TEAM_NAME)).thenThrow(TechnicalException.class);

        teamService.findByName(TEAM_NAME);
    }

    @Test
    public void shouldCreate() throws TechnicalException {
        when(newTeam.getName()).thenReturn(TEAM_NAME);
        when(newTeam.getEmail()).thenReturn(EMAIL);
        when(newTeam.getDescription()).thenReturn(DESCRIPTION);

        when(teamRepository.findByName(TEAM_NAME)).thenReturn(Optional.empty());

        when(team.getName()).thenReturn(TEAM_NAME);
        when(team.getEmail()).thenReturn(EMAIL);
        when(team.getDescription()).thenReturn(DESCRIPTION);
        when(team.getCreatedAt()).thenReturn(date);
        when(team.getUpdatedAt()).thenReturn(date);
        when(teamRepository.create(any(Team.class))).thenReturn(team);

        final TeamEntity createdTeamEntity = teamService.create(newTeam, OWNER);

        verify(teamRepository).create(argThat(new ArgumentMatcher<Team>() {
            public boolean matches(final Object argument) {
                final Team teamToCreate = (Team) argument;
                return TEAM_NAME.equals(teamToCreate.getName()) &&
                    EMAIL.equals(teamToCreate.getEmail()) &&
                    DESCRIPTION.equals(teamToCreate.getDescription()) &&
                    teamToCreate.getCreatedAt() != null &&
                    teamToCreate.getUpdatedAt() != null &&
                    teamToCreate.getCreatedAt().equals(teamToCreate.getUpdatedAt());
            }
        }));

        verify(teamMembershipRepository).addMember(eq(TEAM_NAME), argThat(new ArgumentMatcher<Member>() {
            public boolean matches(final Object argument) {
                final Member member = (Member) argument;
                return OWNER.equals(member.getUsername()) &&
                    OWNER_ROLE.equals(member.getRole()) &&
                    member.getCreatedAt() != null &&
                    member.getUpdatedAt() != null &&
                    member.getCreatedAt().equals(member.getUpdatedAt());
            }
        }));

        assertEquals(TEAM_NAME, createdTeamEntity.getName());
        assertEquals(EMAIL, createdTeamEntity.getEmail());
        assertEquals(DESCRIPTION, createdTeamEntity.getDescription());
        assertEquals(date, createdTeamEntity.getCreatedAt());
        assertEquals(date, createdTeamEntity.getUpdatedAt());
    }

    @Test(expected = TeamAlreadyExistsException.class)
    public void shouldNotCreateBecauseExists() throws TechnicalException {
        when(newTeam.getName()).thenReturn(TEAM_NAME);
        when(teamRepository.findByName(TEAM_NAME)).thenReturn(Optional.of(new Team()));

        teamService.create(newTeam, OWNER);

        verify(teamRepository, never()).create(any());
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotCreateBecauseTechnicalException() throws TechnicalException {
        when(newTeam.getName()).thenReturn(TEAM_NAME);
        when(teamRepository.findByName(TEAM_NAME)).thenReturn(Optional.empty());
        when(teamRepository.create(any(Team.class))).thenThrow(TechnicalException.class);

        teamService.create(newTeam, OWNER);

        verify(teamRepository, never()).create(any());
    }

    @Test
    public void shouldUpdate() throws TechnicalException {
        when(teamRepository.findByName(TEAM_NAME)).thenReturn(Optional.of(team));
        when(teamRepository.update(any(Team.class))).thenReturn(team);

        teamService.update(TEAM_NAME, existingTeam);

        verify(teamRepository).update(argThat(new ArgumentMatcher<Team>() {
            public boolean matches(Object argument) {
                final Team teamToUpdate = (Team) argument;
                return TEAM_NAME.equals(teamToUpdate.getName()) &&
                    teamToUpdate.getUpdatedAt() != null;
            }
        }));
    }

    @Test(expected = TeamNotFoundException.class)
    public void shouldNotUpdateBecauseNotExists() throws TechnicalException {
        when(teamRepository.findByName(TEAM_NAME)).thenReturn(Optional.empty());

        teamService.update(TEAM_NAME, existingTeam);

        verify(teamRepository, never()).update(any());
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotUpdateBecauseTechnicalException() throws TechnicalException {
        when(teamRepository.findByName(TEAM_NAME)).thenReturn(Optional.of(team));
        when(teamRepository.update(any(Team.class))).thenThrow(TechnicalException.class);

        teamService.update(TEAM_NAME, existingTeam);

        verify(teamRepository, never()).update(any());
    }

    @Test
    public void shouldFindByUser() throws TechnicalException {
        final Team privateTeam = new Team();
        privateTeam.setName("privateTeam");
        privateTeam.setPrivateTeam(true);

        final Set<Team> teams = new HashSet<>();
        teams.add(team);
        teams.add(privateTeam);

        when(team.isPrivateTeam()).thenReturn(false);
        when(teamMembershipRepository.findByUser(USER_NAME)).thenReturn(teams);

        final Set<TeamEntity> teamEntities = teamService.findByUser(USER_NAME, false);

        assertNotNull(teamEntities);
        assertEquals(2, teamEntities.size());
    }

    @Test
    public void shouldFindByUserPublicTeamsOnly() throws TechnicalException {
        final String name = "privateTeam";
        final Team privateTeam = new Team();
        privateTeam.setName(name);
        privateTeam.setPrivateTeam(true);

        final Set<Team> teams = new HashSet<>();
        teams.add(team);
        teams.add(privateTeam);

        when(team.isPrivateTeam()).thenReturn(false);
        when(team.getName()).thenReturn(TEAM_NAME);
        when(teamMembershipRepository.findByUser(USER_NAME)).thenReturn(teams);

        final Set<TeamEntity> teamEntities = teamService.findByUser(USER_NAME, true);

        assertNotNull(teamEntities);
        assertEquals(1, teamEntities.size());
        assertEquals(TEAM_NAME, teamEntities.iterator().next().getName());
    }

    @Test
    public void shouldNotFindByUser() throws TechnicalException {
        when(teamMembershipRepository.findByUser(USER_NAME)).thenReturn(null);

        final Set<TeamEntity> teamEntities = teamService.findByUser(USER_NAME, false);

        assertNotNull(teamEntities);
        assertTrue(teamEntities.isEmpty());
    }

    @Test
    public void shouldNotFindByUserPublicTeamsOnly() throws TechnicalException {
        when(teamMembershipRepository.findByUser(USER_NAME)).thenReturn(null);

        final Set<TeamEntity> teamEntities = teamService.findByUser(USER_NAME, true);

        assertNotNull(teamEntities);
        assertTrue(teamEntities.isEmpty());
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotFindByUserBecauseTechnicalException() throws TechnicalException {
        when(teamMembershipRepository.findByUser(USER_NAME)).thenThrow(TechnicalException.class);

        teamService.findByUser(USER_NAME, true);
    }

    @Test
    public void shouldFindAll() throws TechnicalException {
        final Set<Team> teams = new HashSet<>();
        teams.add(team);

        when(team.isPrivateTeam()).thenReturn(false);
        when(teamRepository.findAll(false)).thenReturn(teams);

        final Set<TeamEntity> teamEntities = teamService.findAll(false);

        assertNotNull(teamEntities);
        assertEquals(1, teamEntities.size());
    }

    @Test
    public void shouldFindAllPublicTeamsOnly() throws TechnicalException {
        final Set<Team> teams = new HashSet<>();
        teams.add(team);

        when(team.isPrivateTeam()).thenReturn(false);
        when(teamRepository.findAll(true)).thenReturn(teams);

        final Set<TeamEntity> teamEntities = teamService.findAll(true);

        assertNotNull(teamEntities);
        assertEquals(1, teamEntities.size());
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotFindAllBecauseTechnicalException() throws TechnicalException {
        when(teamRepository.findAll(true)).thenThrow(TechnicalException.class);

        teamService.findAll(true);
    }
}
