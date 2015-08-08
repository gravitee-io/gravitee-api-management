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
package io.gravitee.repositories.mongodb.internal;

import java.util.ArrayList;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import io.gravitee.repositories.mongodb.RepositoryConfiguration;
import io.gravitee.repositories.mongodb.internal.model.TeamMemberMongo;
import io.gravitee.repositories.mongodb.internal.model.TeamMongo;
import io.gravitee.repositories.mongodb.internal.model.UserMongo;
import io.gravitee.repositories.mongodb.internal.team.TeamMongoRepository;
import io.gravitee.repositories.mongodb.internal.user.UserMongoRepository;

@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes={ RepositoryConfiguration.class})
public class TeamRepositoryTest {

	private static final Integer NB_USERS_TEAM = 24;
	
	@Autowired
    private TeamMongoRepository teamRepository;
   
	@Autowired
    private UserMongoRepository userRepository;

	@Test
	public void createTeamTest() throws Exception {
		
    	TeamMongo team = new TeamMongo();
    	team.setMembers(new ArrayList<TeamMemberMongo>());
    	team.setName("teamTest");
    	team.setDescription("Sample team description");
    	
    	for(int i=0; i<NB_USERS_TEAM; i++){
	    	UserMongo user = new UserMongo();
	    	user.setMail("sample@gmail.com");
	    	user.setName("sample"+i);
	    	UserMongo saveUser = userRepository.save(user);
	    	TeamMemberMongo teamMemberMongo = new TeamMemberMongo();
	    	teamMemberMongo.setMember(saveUser);
	    	teamMemberMongo.setRole("MEMBER");
	    	team.getMembers().add(teamMemberMongo);
    	}
    	
    	teamRepository.insert(team);
	}
   
}
