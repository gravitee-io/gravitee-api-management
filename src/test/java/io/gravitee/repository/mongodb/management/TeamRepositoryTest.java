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
package io.gravitee.repository.mongodb.management;

import io.gravitee.repository.management.api.TeamRepository;
import io.gravitee.repository.management.model.Team;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.Optional;
import java.util.Set;

public class TeamRepositoryTest extends  AbstractMongoDBTest{
	
	private static final String TESTCASES_PATH = "/data/team-tests/";
	
	private static final int NB_TEAMS_TESTCASES = 2; 
	
	private static final int NB_PUBLIC_TEAMS_TESTCASES = 1;
	
	@Autowired
	private TeamRepository teamRepository;
	
	private Logger logger = LoggerFactory.getLogger(TeamRepositoryTest.class);

	@Override
	protected String getTestCasesPath() {
		return TESTCASES_PATH;
	}

	@Test
	public void createTeamTest() {

		try {
	
			String teamname = "team-created";
			
			Team team = new Team();
			team.setName(teamname);
			team.setEmail(String.format("%s@gravitee.io", teamname));
			team.setDescription("Sample description");
			team.setPrivateTeam(true);
			team.setCreatedAt(new Date());
			team.setUpdatedAt(new Date());
			
			Team userCreated =  teamRepository.create(team);
			
			Assert.assertNotNull("Team created is null", userCreated);
			
			Optional<Team> optional = teamRepository.findByName(teamname);
			
			Assert.assertTrue("Unable to find saved team", optional.isPresent());
			Team teamFound = optional.get();
			
			Assert.assertEquals("Invalid saved team name.", 		team.getName(),  		teamFound.getName());
			Assert.assertEquals("Invalid saved team mail.",			team.getEmail(), 		teamFound.getEmail());
			Assert.assertEquals("Invalid saved team description.", 	team.getDescription(),	teamFound.getDescription());
			Assert.assertEquals("Invalid saved team visibility.",	team.isPrivateTeam(),	teamFound.isPrivateTeam());
			Assert.assertEquals("Invalid saved team creationDate.", team.getCreatedAt(),	teamFound.getCreatedAt());
			Assert.assertEquals("Invalid saved team updateDate.",	team.getUpdatedAt(),	teamFound.getUpdatedAt());
	
			
		} catch (Exception e) {
			logger.error("Error while testing createTeam", e);
			Assert.fail("Error while testing createTeam");	
		}
	}

	@Test
	public void findByNameTest(){
		
		try{
			String teamname = "team2";
			Optional<Team> optional = teamRepository.findByName(teamname);
			
			Assert.assertTrue("Unable to find saved team", optional.isPresent());
			Team teamFound = optional.get();
			
			Assert.assertEquals("Invalid saved team name.", 		teamname,  					teamFound.getName());
			Assert.assertEquals("Invalid saved team mail.",			"team2@gravitee.io", 		teamFound.getEmail());
			Assert.assertEquals("Invalid saved team description.", 	"Sample team2 description",	teamFound.getDescription());
			Assert.assertEquals("Invalid saved team visibility.",	true,						teamFound.isPrivateTeam());
			Assert.assertEquals("Invalid saved team creationDate.", getIsoDate("2015-08-08T08:20:10.883Z"),	teamFound.getCreatedAt());
			Assert.assertEquals("Invalid saved team updateDate.",	getIsoDate("2015-08-08T08:20:10.883Z"),	teamFound.getUpdatedAt());
			
		} catch (Exception e) {
			logger.error("Error while testing findByName", e);
			Assert.fail("Error while testing findByName");	
		}
	}
	
	@Test
	public void updateTest(){
		try{	
			String teamname = "team1";
			
			String newDescription = "updated-team1 description";
			String newEmail = "updated-team1@gravitee.io";
			boolean newVisibility = false;
			Date udpatedAt = new Date();
			
			Team team = new Team();
			team.setName(teamname);
			team.setEmail(newEmail);
			team.setDescription(newDescription);
			team.setPrivateTeam(newVisibility);
			team.setUpdatedAt(udpatedAt);
			
			teamRepository.update(team);
			
			Optional<Team> optional = teamRepository.findByName(teamname);
			Team updatedTeam = optional.get();
			
			Assert.assertTrue("Team modified not found", optional.isPresent());
			
			Assert.assertEquals("Invalid updated team description.", newDescription, updatedTeam.getDescription());
			Assert.assertEquals("Invalid updated team email.", newEmail, updatedTeam.getEmail());
			Assert.assertEquals("Invalid updated team visibility.", newVisibility, updatedTeam.isPrivateTeam() );
			Assert.assertEquals("Invalid updated team updatedAt date.", udpatedAt, updatedTeam.getUpdatedAt());
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Error while testing update", e);
			Assert.fail("Error while testing update");	
		}
	}

	
	@Test
	public void deleteTest(){
		try{
			String teamname = "team1";
			
			int nbTeamBefore = teamRepository.findAll(false).size();
			teamRepository.delete("team1");
			
			Optional<Team> optional = teamRepository.findByName(teamname);
			int nbTeamAfter = teamRepository.findAll(false).size();
			
			Assert.assertFalse("Deleted team always present", optional.isPresent());
			Assert.assertEquals("Invalid number of team after deletion", nbTeamBefore -1, nbTeamAfter);
			
		} catch (Exception e) {
			logger.error("Error while testing delete", e);
			Assert.fail("Error while testing delete");	
		}
	}
	

	@Test
	public void findAllTest() {
		try{
			Set<Team> teams = teamRepository.findAll(false);
				
			Assert.assertNotNull(teams);
			Assert.assertEquals("Invalid user numbers in find all", NB_TEAMS_TESTCASES, teams.size());
			
		}catch(Exception e){
			logger.error("Error while finding all team",e);
			Assert.fail("Error while finding all team");
		}
	}	

	@Test
	public void findAllPublicTest() {
		try{
			Set<Team> teams = teamRepository.findAll(true);
				
			Assert.assertNotNull(teams);
			Assert.assertEquals("Invalid user numbers in find all", NB_PUBLIC_TEAMS_TESTCASES, teams.size());
			
		}catch(Exception e){
			logger.error("Error while finding all public team",e);
			Assert.fail("Error while finding all public team");
		}
	}	

}
