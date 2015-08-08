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
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.gravitee.repositories.mongodb.internal.application.ApplicationMongoRepository;
import io.gravitee.repositories.mongodb.internal.model.ApplicationMongo;
import io.gravitee.repositories.mongodb.internal.team.TeamMongoRepository;
import io.gravitee.repositories.mongodb.internal.user.UserMongoRepository;
import io.gravitee.repositories.mongodb.mapper.GraviteeMapper;
import io.gravitee.repository.api.ApplicationRepository;
import io.gravitee.repository.model.Application;
import io.gravitee.repository.model.OwnerType;

@Component
public class ApplicationRepositoryImpl implements ApplicationRepository{

	@Autowired
	private ApplicationMongoRepository internalApplicationRepo;

	@Autowired
	private TeamMongoRepository internalTeamRepo;
	
	@Autowired
	private UserMongoRepository internalUserRepo;
	
	@Autowired
	private GraviteeMapper mapper;

	@Override
	public Set<Application> findAll() {
		
		List<ApplicationMongo> applications = internalApplicationRepo.findAll();
		return mapper.collection2set(applications, ApplicationMongo.class, Application.class);
	}


	//TODO externalize
	private ApplicationMongo mapApplication(Application application) {

		ApplicationMongo apiMongo = mapper.map(application, ApplicationMongo.class);

		if (OwnerType.USER.equals(application.getOwnerType())) {
			apiMongo.setOwner(internalUserRepo.findOne(application.getOwner()));
		} else {
			apiMongo.setOwner(internalTeamRepo.findOne(application.getOwner()));
		}
		return apiMongo;
	}


	@Override
	public Application create(Application application) {
		ApplicationMongo applicationMongo = mapApplication(application);
		ApplicationMongo applicationMongoCreated = internalApplicationRepo.insert(applicationMongo);
		return mapper.map(applicationMongoCreated, Application.class);
	}

	@Override
	public Application update(Application application) {
		ApplicationMongo applicationMongo =	mapApplication(application);
		ApplicationMongo applicationMongoUpdated = internalApplicationRepo.save(applicationMongo);
		return mapper.map(applicationMongoUpdated, Application.class);
	}

	@Override
	public Optional<Application> findByName(String applicationName) {
		ApplicationMongo application = internalApplicationRepo.findOne(applicationName);
		return Optional.ofNullable(mapper.map(application, Application.class));
	}

	@Override
	public void delete(String apiName) {
		internalApplicationRepo.delete(apiName);
	}
	

	@Override
	public Set<Application> findByUser(String username) {
		List<ApplicationMongo> applications = internalApplicationRepo.findByUser(username);
		return mapper.collection2set(applications, ApplicationMongo.class, Application.class);
	}


	@Override
	public Set<Application> findByTeam(String teamName) {
	
		List<ApplicationMongo> applications = internalApplicationRepo.findByTeam(teamName);
		return mapper.collection2set(applications, ApplicationMongo.class, Application.class);
	}
	
	@Override
	public int countByUser(String username) {
		return (int) internalApplicationRepo.countByUser(username);

	}

	@Override
	public int countByTeam(String teamName) {
		return (int) internalApplicationRepo.countByTeam(teamName);	
	}
	
}
