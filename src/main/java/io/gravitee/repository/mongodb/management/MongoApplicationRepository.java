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

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.mongodb.management.internal.application.ApplicationMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.ApplicationMongo;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoApplicationRepository implements ApplicationRepository {

	@Autowired
	private ApplicationMongoRepository internalApplicationRepo;

	@Autowired
	private GraviteeMapper mapper;

	@Override
	public Set<Application> findAll() throws TechnicalException {
		List<ApplicationMongo> applications = internalApplicationRepo.findAll();
		return mapApplications(applications);
	}

	@Override
	public Application create(Application application) throws TechnicalException {
		ApplicationMongo applicationMongo = mapApplication(application);
		ApplicationMongo applicationMongoCreated = internalApplicationRepo.insert(applicationMongo);
		return mapApplication(applicationMongoCreated);
	}

	@Override
	public Application update(Application application) throws TechnicalException {
		ApplicationMongo applicationMongo = internalApplicationRepo.findOne(application.getId());
		
		// Update, but don't change invariant other creation information
		applicationMongo.setName(application.getName());
		applicationMongo.setDescription(application.getDescription());
		applicationMongo.setUpdatedAt(application.getUpdatedAt());
		applicationMongo.setType(application.getType());
		applicationMongo.setGroup(application.getGroup());

		ApplicationMongo applicationMongoUpdated = internalApplicationRepo.save(applicationMongo);
		return mapApplication(applicationMongoUpdated);
	}

	@Override
	public Optional<Application> findById(String applicationId) throws TechnicalException {
		ApplicationMongo application = internalApplicationRepo.findOne(applicationId);
		return Optional.ofNullable(mapApplication(application));
	}

	@Override
	public Set<Application> findByIds(List<String> ids) throws TechnicalException {
		return mapApplications(internalApplicationRepo.findByIds(ids));
	}

	@Override
	public Set<Application> findByGroups(List<String> groupIds) throws TechnicalException {
		return mapApplications(internalApplicationRepo.findByGroups(groupIds));
	}

	@Override
	public void delete(String applicationId) throws TechnicalException {
		internalApplicationRepo.delete(applicationId);
	}

	private Set<Application> mapApplications(Collection<ApplicationMongo> applications){
		return applications.stream().map(this::mapApplication).collect(Collectors.toSet());
	}
	
	private Application mapApplication(ApplicationMongo applicationMongo) {
		return (applicationMongo == null) ? null : mapper.map(applicationMongo, Application.class);
	}
	
	private ApplicationMongo mapApplication(Application application) {
		return (application == null) ? null : mapper.map(application, ApplicationMongo.class);
	}
}
