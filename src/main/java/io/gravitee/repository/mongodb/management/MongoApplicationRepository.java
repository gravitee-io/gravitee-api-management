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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.ApplicationMembership;
import io.gravitee.repository.management.model.MembershipType;
import io.gravitee.repository.mongodb.management.internal.application.ApplicationMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.ApplicationMongo;
import io.gravitee.repository.mongodb.management.internal.model.MemberMongo;
import io.gravitee.repository.mongodb.management.internal.model.UserMongo;
import io.gravitee.repository.mongodb.management.internal.user.UserMongoRepository;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author Gravitee.io Team
 */
@Component
public class MongoApplicationRepository implements ApplicationRepository {

	@Autowired
	private ApplicationMongoRepository internalApplicationRepo;
	
	@Autowired
	private UserMongoRepository internalUserRepo;
	
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
		ApplicationMongo applicationMongo = internalApplicationRepo.findOne(application.getName());
		
		//Update, but don't change invariant other creation information 
		applicationMongo.setDescription(application.getDescription());
		applicationMongo.setUpdatedAt(application.getUpdatedAt());
		applicationMongo.setType(application.getType());

		ApplicationMongo applicationMongoUpdated = internalApplicationRepo.save(applicationMongo);
		return mapApplication(applicationMongoUpdated);
	}

	@Override
	public Optional<Application> findById(String applicationName) throws TechnicalException {
		ApplicationMongo application = internalApplicationRepo.findOne(applicationName);
		return Optional.ofNullable(mapApplication(application));
	}

	@Override
	public void delete(String apiName) throws TechnicalException {
		internalApplicationRepo.delete(apiName);
	}
	

	@Override
	public Set<Application> findByUser(String username) throws TechnicalException {
		List<ApplicationMongo> applications = internalApplicationRepo.findByUser(username);
		return mapApplications(applications);
	}
	
	@Override
	public int countByUser(String username) throws TechnicalException {
		try{
			return internalApplicationRepo.countByUser(username);
		}catch(Exception e){
			throw new TechnicalException("Count by user failed", e);
		}
	}

	@Override
	public void addMember(ApplicationMembership membership) throws TechnicalException {
		ApplicationMongo applicationMongo = internalApplicationRepo.findOne(membership.getApplication());
		UserMongo userMongo = internalUserRepo.findOne(membership.getUser());

		MemberMongo memberMongo = new MemberMongo();
		memberMongo.setUser(userMongo);
		memberMongo.setType(membership.getMembershipType().toString());
		memberMongo.setCreatedAt(membership.getCreatedAt());
		memberMongo.setUpdatedAt(membership.getUpdatedAt());

		applicationMongo.getMembers().add(memberMongo);

		internalApplicationRepo.save(applicationMongo);
	}

	@Override
	public void deleteMember(String application, String username) throws TechnicalException {
		ApplicationMongo applicationMongo = internalApplicationRepo.findOne(application);
		MemberMongo memberToDelete = null;

		for (MemberMongo memberMongo : applicationMongo.getMembers()) {
			if (memberMongo.getUser().getName().equalsIgnoreCase(username)) {
				memberToDelete = memberMongo;
			}
		}

		if (memberToDelete != null) {
			applicationMongo.getMembers().remove(memberToDelete);
			internalApplicationRepo.save(applicationMongo);
		}
	}

	@Override
	public Collection<ApplicationMembership> getMembers(String application) throws TechnicalException {
		ApplicationMongo applicationMongo = internalApplicationRepo.findOne(application);
		List<MemberMongo> membersMongo = applicationMongo.getMembers();
		Set<ApplicationMembership> members = new HashSet<>(membersMongo.size());

		for (MemberMongo memberMongo : membersMongo) {
			ApplicationMembership member = new ApplicationMembership();
			member.setApplication(applicationMongo.getName());
			member.setUser(memberMongo.getUser().getName());
			member.setMembershipType(MembershipType.valueOf(memberMongo.getType()));
			member.setCreatedAt(memberMongo.getCreatedAt());
			member.setUpdatedAt(memberMongo.getUpdatedAt());
			members.add(member);
		}

		return members;
	}

	@Override
	public ApplicationMembership getMember(String application, String username) throws TechnicalException {
		Collection<ApplicationMembership> members = getMembers(application);
		for (ApplicationMembership member : members) {
			if (member.getUser().equalsIgnoreCase(username)) {
				return member;
			}
		}

		return null;
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
