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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.gravitee.repositories.mongodb.internal.application.ApplicationMongoRepository;
import io.gravitee.repositories.mongodb.internal.model.ApplicationMongo;
import io.gravitee.repositories.mongodb.internal.model.TeamMongo;
import io.gravitee.repositories.mongodb.internal.model.UserMongo;
import io.gravitee.repositories.mongodb.internal.team.TeamMongoRepository;
import io.gravitee.repositories.mongodb.internal.user.UserMongoRepository;
import io.gravitee.repositories.mongodb.mapper.GraviteeMapper;
import io.gravitee.repository.api.ApplicationRepository;
import io.gravitee.repository.model.Application;
import io.gravitee.repository.model.OwnerType;
import io.gravitee.repository.model.Team;

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
		return mapApplications(applications);
	}


	@Override
	public Application create(Application application) {
		ApplicationMongo applicationMongo = mapApplication(application);
		ApplicationMongo applicationMongoCreated = internalApplicationRepo.insert(applicationMongo);
		return mapApplication(applicationMongoCreated);
	}

	@Override
	public Application update(Application application) {
		
    	ApplicationMongo applicationMongo = internalApplicationRepo.findOne(application.getName());
		
		//Update 
		applicationMongo.setDescription(application.getDescription());
		applicationMongo.setKey(application.getKey());
		applicationMongo.setUpdatedAt(application.getUpdatedAt());
		applicationMongo.setType(application.getType());
		
		if (OwnerType.USER.equals(application.getOwnerType())) {
			applicationMongo.setOwner(internalUserRepo.findOne(application.getOwner()));
		} else {
			applicationMongo.setOwner(internalTeamRepo.findOne(application.getOwner()));
		}
		
		//Don't change invariant other creation information
		//FIXME can i change application name ? update application references
		
		ApplicationMongo applicationMongoUpdated = internalApplicationRepo.save(applicationMongo);
		return mapApplication(applicationMongoUpdated);
	}

	@Override
	public Optional<Application> findByName(String applicationName) {
		ApplicationMongo application = internalApplicationRepo.findOne(applicationName);
		return Optional.ofNullable(mapApplication(application));
	}

	@Override
	public void delete(String apiName) {
		internalApplicationRepo.delete(apiName);
	}
	

	@Override
	public Set<Application> findByUser(String username) {
		
		List<ApplicationMongo> applications = internalApplicationRepo.findByUser(username);
		return mapApplications(applications);
	}


	@Override
	public Set<Application> findByTeam(String teamName) {
	
		List<ApplicationMongo> applications = internalApplicationRepo.findByTeam(teamName);
		return mapApplications(applications);
	}
	
	@Override
	public int countByUser(String username) {
		return (int) internalApplicationRepo.countByUser(username);

	}

	@Override
	public int countByTeam(String teamName) {
		return (int) internalApplicationRepo.countByTeam(teamName);	
	}
	
	private Set<Application> mapApplications(Collection<ApplicationMongo> applications){
		
		Set<Application> res = new HashSet<>();
		for (ApplicationMongo application : applications) {
			res.add(mapApplication(application));
		}
		return res;
	}
	
	private Application mapApplication(ApplicationMongo  applicationMongo) {

		if(applicationMongo == null){
			return null;
		}
		
		Application application = mapper.map(applicationMongo, Application.class);

		if(applicationMongo.getOwner() != null){
			application.setOwner(applicationMongo.getOwner().getName());
			if(applicationMongo.getOwner() instanceof UserMongo){
				application.setOwnerType(OwnerType.USER);
			}else{
				application.setOwnerType(OwnerType.TEAM);
			}
		}
		if(applicationMongo.getCreator()!= null){
			application.setCreator(applicationMongo.getCreator().getName());
		}
		
		return application;
	}
	
	private ApplicationMongo mapApplication(Application application) {

		if(application == null){
			return null;
		}
		
		ApplicationMongo applicationMongo = mapper.map(application, ApplicationMongo.class);

		if (OwnerType.USER.equals(application.getOwnerType())) {
			applicationMongo.setOwner(internalUserRepo.findOne(application.getOwner()));
		} else {
			applicationMongo.setOwner(internalTeamRepo.findOne(application.getOwner()));
		}
		applicationMongo.setCreator(internalUserRepo.findOne(application.getCreator()));
		
		return applicationMongo;
	}

	
}
