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
package io.gravitee.repository.mongodb;

import io.gravitee.repository.management.api.TeamMembershipRepository;
import io.gravitee.repository.management.model.Member;
import io.gravitee.repository.management.model.Team;
import io.gravitee.repository.management.model.TeamRole;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Date;
import java.util.Set;
import java.util.function.Predicate;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { TestRepositoryConfiguration.class })
public class TeamMembershipRepositoryTest extends  AbstractMongoDBTest{
	
	private static final String TESTCASES_PATH = "/data/team-membership-tests/";
	
	private static final int NB_SAMPLE_TEAM_MEMBERS_TESTCASES = 6; 
	private static final int NB_ADDED_TEAM_MEMBERS_TESTCASES = 3;
	private static final int NB_UPDATE_TEAM_MEMBERS_TESTCASES = 4;
	private static final int NB_REMOVED_TEAM_MEMBERS_TESTCASES = 4; 
	private static final int NB_TEAMS_USER2_TESTCASES = 4; 
	
	@Autowired
	private TeamMembershipRepository membershipRepository;
	
	private Logger logger = LoggerFactory.getLogger(TeamMembershipRepositoryTest.class);

	@Override
	protected String getTestCasesPath() {
		return TESTCASES_PATH;
	}

	@Test
	public void getTeamMembersTest() {

		try {
	
			Set<Member> members = membershipRepository.listMembers("team1");
			
			Assert.assertNotNull("Team members is null", members);
			Assert.assertEquals("Invalid number of member user name.", NB_SAMPLE_TEAM_MEMBERS_TESTCASES, members.size());
			
		} catch (Exception e) {
			logger.error("Error while testing listMembers", e);
			Assert.fail("Error while testing listMembers");	
		}
	}

	@Test
	public void addTeamMemberTest() {

		String teamName = "team-add-member";
		String memberName = "added-member";
		
		try {
			Member member = new Member();
			member.setUsername(memberName);
			member.setRole(TeamRole.MEMBER);
			member.setUpdatedAt(new Date());
	
			membershipRepository.addMember(teamName, member);
			
			Set<Member> membersUpdated = membershipRepository.listMembers(teamName);
			
			Assert.assertNotNull("Team members is null", teamName);
			Assert.assertEquals("Invalid number of member user name after adding one.", NB_ADDED_TEAM_MEMBERS_TESTCASES +1, membersUpdated.size());
	
			long validMembers = membersUpdated.stream().filter(new Predicate<Member>() {

				@Override
				public boolean test(Member t) {
					return t.getUsername().equals(memberName) && TeamRole.MEMBER.equals(t.getRole());
				}
			}).count();
			Assert.assertEquals("Invalid added member count found", 1, validMembers);
			
				
			
		} catch (Exception e) {
			logger.error("Error while testing listMembers", e);
			Assert.fail("Error while testing listMembers");	
		}
	}
	
	@Test
	public void updateTeamMemberTest() {

		String teamName = "team-update-member";
		String memberName = "updated-member";
		
		try {
	
			Member member = new Member();
			member.setUsername(memberName);
			member.setRole(TeamRole.ADMIN);
			member.setUpdatedAt(new Date());
			
			membershipRepository.updateMember(teamName, member);
			
			Set<Member> membersUpdated = membershipRepository.listMembers(teamName);
			
			Assert.assertNotNull("Team members is null", teamName);
			Assert.assertEquals("Invalid number of member user name after updating one.", NB_UPDATE_TEAM_MEMBERS_TESTCASES , membersUpdated.size());
	
			long validMembers = membersUpdated.stream().filter(new Predicate<Member>() {

				@Override
				public boolean test(Member t) {
					return t.getUsername().equals(memberName) && TeamRole.ADMIN.equals(t.getRole());
				}
			}).count();
			Assert.assertEquals("Invalid added member count found", 1, validMembers);
			
		} catch (Exception e) {
			logger.error("Error while testing listMembers", e);
			Assert.fail("Error while testing listMembers");	
		}
	}
	
	@Test
	public void removeTeamMemberTest() {

		String teamName = "team-delete-member";
		String memberName = "deleted-member";
		
		try {
	
			membershipRepository.deleteMember(teamName, memberName);
			
			Set<Member> membersRemaining = membershipRepository.listMembers(teamName);
			
			Assert.assertNotNull("Team members is null", teamName);
			Assert.assertEquals("Invalid number of member user name after removing one.", NB_REMOVED_TEAM_MEMBERS_TESTCASES-1 , membersRemaining.size());
	
			long validMembers = membersRemaining.stream().filter(new Predicate<Member>() {

				@Override
				public boolean test(Member t) {
					return t.getUsername().equals(memberName);
				}
			}).count();
			Assert.assertEquals("Invalid added member count found", 0, validMembers);
			
		} catch (Exception e) {
			logger.error("Error while testing deleteMember", e);
			Assert.fail("Error while testing deleteMember");	
		}
	}    
  
	@Test
	public void findTeamByUserTest() {

		try{
			Set<Team> teams = membershipRepository.findByUser("user2");
			Assert.assertNotNull("No teams found for the given user", teams);
			Assert.assertEquals("Invalid number of teams found for user",NB_TEAMS_USER2_TESTCASES, teams.size());
			
		} catch (Exception e) {
			logger.error("Error while testing findTeamByUser", e);
			Assert.fail("Error while testing findTeamByUser");	
		}
	}
	
	
	@Test
	public void getMemberTest() {

		try{
			Member member = membershipRepository.getMember("team1", "user1");
			Assert.assertNotNull("No team member found for the given user and team", member);
			
		} catch (Exception e) {
			logger.error("Error while testing getMember", e);
			Assert.fail("Error while testing getMember");	
		}
	}
}
