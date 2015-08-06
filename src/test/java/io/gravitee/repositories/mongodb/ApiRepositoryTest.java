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

import java.util.List;
import java.util.Set;

import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import io.gravitee.repositories.mongodb.internal.api.ApiRepository;
import io.gravitee.repositories.mongodb.internal.model.Api;
import io.gravitee.repositories.mongodb.internal.model.Team;
import io.gravitee.repositories.mongodb.internal.model.User;
import io.gravitee.repositories.mongodb.internal.team.TeamRepository;
import io.gravitee.repositories.mongodb.internal.user.UserRepository;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { RepositoryConfiguration.class })
public class ApiRepositoryTest {

	@Autowired
	private ApiRepository apiRepository;

	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private TeamRepository teamRepository;

	@Test
	public void createApiTest() {

		try {
			User creator = new User();
			creator.setMail("sample@mail.com");
			creator.setUsername("myusername");
			userRepository.save(creator);

			Team owner = new Team();
			owner.setName("myteamname");
			owner.setDescription("Team used for unit tests");
			teamRepository.save(owner);
			
			Api api = new Api();
			api.setName("sample");
			api.setVersion("1");
			api.setCreator(creator);
			api.setOwner(owner);

			apiRepository.save(api);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

	@Test
	public void findByCreatorIdTest() {
		List<Api> apis = apiRepository.findByCreatorId(new ObjectId("55c346a8d4c60e0dd348183d"));
		System.out.println(apis);
	
		Assert.assertNotNull(apis);
	}

	@Test
	public void findByCreatorNameTest() {
		Set<Api> apis = apiRepository.findByCreator("testcaseusername");
		System.out.println(apis);
	
		Assert.assertNotNull(apis);
	}
	
	@Test
	public void findByTeamNameTest() {
		Set<Api> apis = apiRepository.findByTeam("testcaseteamname");
		System.out.println(apis);
	
		Assert.assertNotNull(apis);
	}
	
	@Test
	public void findAllTest() {
		List<Api> apis = apiRepository.findAll();
		
		Assert.assertNotNull(apis);
		Assert.assertFalse("Fail to resolve api in findAll", apis.isEmpty());
	}	
	
	@Test
	public void findByNameTest() {
		Api api = apiRepository.findByName("sample");
		Assert.assertNotNull(api);
	}

	@Test
	public void start() {
		apiRepository.start("sample");
	}

	@Test
	public void stop() {
		apiRepository.stop("sample");
	}

	@Test
	public void findAll() {
		List<Api> apis = apiRepository.findAll();
		Assert.assertNotNull(apis);
	}

	@Test
	public void deleteApiTest() {
		apiRepository.delete("sample");
	}

}
