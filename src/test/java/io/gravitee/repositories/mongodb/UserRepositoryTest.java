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
package io.gravitee.repositories.mongodb;

import java.util.ArrayList;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Assert;

import io.gravitee.repositories.mongodb.internal.model.Team;
import io.gravitee.repositories.mongodb.internal.model.User;
import io.gravitee.repositories.mongodb.internal.team.TeamRepository;
import io.gravitee.repositories.mongodb.internal.user.UserRepository;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes={ RepositoryConfiguration.class})
public class UserRepositoryTest {

	private static Integer NB_USERS_TEAM = 24;

	@Autowired
    private TeamRepository teamRepository;
   
	@Autowired
    private UserRepository userRepository;

    @Before
    public void init(){
    	
    	Team team = new Team();
    	team.setMembers(new ArrayList<User>());
    	team.setName("teamTest");
    	team.setDescription("Sample team description");
    	
    	for(int i=0; i<NB_USERS_TEAM; i++){
	    	User user = new User();
	    	user.setMail("sample@gmail.com");
	    	user.setUsername("sample");
	    	team.getMembers().add(userRepository.save(user));
    	}
    	
    	teamRepository.save(team);
    }
	
	@Test 
	public void createUserTest(){
		
    	User user = new User();
    	user.setMail("sample@gmail.com");
    	user.setUsername("sample");
    	
    	userRepository.save(user);
	}
	
	@Test 
	public void findByTeamTest(){
    	Set<User> users = userRepository.findByTeam("teamTest");
    	
    	Assert.notNull(users);
    	Assert.isTrue(users.size() == NB_USERS_TEAM, "Invalid nb users found");
	}
	
 
}
