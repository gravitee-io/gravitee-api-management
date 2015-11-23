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
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.repository.management.model.MembershipType;
import io.gravitee.repository.management.model.Visibility;
import io.gravitee.repository.mongodb.management.internal.api.ApiMongoRepository;
import io.gravitee.repository.mongodb.management.internal.key.ApiKeyMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.ApiAssociationMongo;
import io.gravitee.repository.mongodb.management.internal.model.ApiMongo;
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
public class MongoApiRepository implements ApiRepository {

	@Autowired
	private ApiKeyMongoRepository internalApiKeyRepo;
	
	@Autowired
	private ApiMongoRepository internalApiRepo;
	
	@Autowired
	private UserMongoRepository internalUserRepo;
	
	@Autowired
	private GraviteeMapper mapper;
	
	@Override
	public Optional<Api> findById(String apiId) throws TechnicalException {
		ApiMongo apiMongo =  internalApiRepo.findOne(apiId);
		return Optional.ofNullable(mapApi(apiMongo));
	}

	@Override
	public Set<Api> findAll() throws TechnicalException {
		List<ApiMongo> apis = internalApiRepo.findAll();
		return mapApis(apis);
	}
	
	@Override
	public Api create(Api api) throws TechnicalException {
		ApiMongo apiMongo = mapApi(api);
		ApiMongo apiMongoCreated = internalApiRepo.insert(apiMongo);
		return mapApi(apiMongoCreated);
	}

	@Override
	public Api update(Api api) throws TechnicalException {
		ApiMongo apiMongo =	mapApi(api);
		ApiMongo apiMongoUpdated = internalApiRepo.save(apiMongo);
		return mapApi(apiMongoUpdated);
	}

	@Override
	public void delete(String apiId) throws TechnicalException {
		internalApiRepo.delete(apiId);
	}

	@Override
	public Set<Api> findByMember(String username, MembershipType membershipType, Visibility visibility) throws TechnicalException {
		return mapApis(internalApiRepo.findByMember(username, membershipType, visibility));
	}

	@Override
	public int countByUser(String username, MembershipType membershipType) throws TechnicalException {
		return (membershipType == null) ?
			internalApiRepo.countByUser(username, null) :
				internalApiRepo.countByUser(username, membershipType.toString());
	}

	@Override
	public Set<Api> findByApplication(String applicationId) throws TechnicalException {
		List<ApiAssociationMongo> apiAssociationMongos = internalApiKeyRepo.findByApplication(applicationId);
		return apiAssociationMongos.stream().map(t -> mapper.map(t.getApi(), Api.class)).collect(Collectors.toSet());
	}

	@Override
	public void saveMember(String apiId, String username, MembershipType membershipType) throws TechnicalException {
		ApiMongo apiMongo = internalApiRepo.findOne(apiId);
		UserMongo userMongo = internalUserRepo.findOne(username);

		Membership membership = getMember(apiId, username);
		if (membership == null) {
			MemberMongo memberMongo = new MemberMongo();
			memberMongo.setUser(userMongo);
			memberMongo.setType(membershipType.toString());
			memberMongo.setCreatedAt(new Date());
			memberMongo.setUpdatedAt(memberMongo.getCreatedAt());

			apiMongo.getMembers().add(memberMongo);

			internalApiRepo.save(apiMongo);
		} else {
			for (MemberMongo memberMongo : apiMongo.getMembers()) {
				if (memberMongo.getUser().getName().equalsIgnoreCase(username)) {
					memberMongo.setType(membershipType.toString());
					internalApiRepo.save(apiMongo);
					break;
				}
			}
		}
	}

	@Override
	public void deleteMember(String apiId, String username) throws TechnicalException {
		ApiMongo apiMongo = internalApiRepo.findOne(apiId);
		MemberMongo memberToDelete = null;

		for (MemberMongo memberMongo : apiMongo.getMembers()) {
			if (memberMongo.getUser().getName().equalsIgnoreCase(username)) {
				memberToDelete = memberMongo;
			}
		}

		if (memberToDelete != null) {
			apiMongo.getMembers().remove(memberToDelete);
			internalApiRepo.save(apiMongo);
		}
	}

	@Override
	public Membership getMember(String apiId, String username) throws TechnicalException {
		Collection<Membership> members = getMembers(apiId, null);
		for (Membership member : members) {
			if (member.getUser().equalsIgnoreCase(username)) {
				return member;
			}
		}

		return null;
	}

	@Override
	public Collection<Membership> getMembers(String apiId, MembershipType membershipType) throws TechnicalException {
		ApiMongo apiMongo = internalApiRepo.findOne(apiId);
		List<MemberMongo> membersMongo = apiMongo.getMembers();
		Set<Membership> members = new HashSet<>(membersMongo.size());

		for (MemberMongo memberMongo : membersMongo) {
			if (membershipType == null || (
					membershipType != null && memberMongo.getType().equalsIgnoreCase(membershipType.toString()))) {
				Membership member = new Membership();
				member.setUser(memberMongo.getUser().getName());
				member.setMembershipType(MembershipType.valueOf(memberMongo.getType()));
				member.setCreatedAt(memberMongo.getCreatedAt());
				member.setUpdatedAt(memberMongo.getUpdatedAt());
				members.add(member);
			}
		}

		return members;
	}

	private Set<Api> mapApis(Collection<ApiMongo> apis) {
		return apis.stream().map(this::mapApi).collect(Collectors.toSet());
	}

	private ApiMongo mapApi(Api api){
		return (api == null) ? null : mapper.map(api, ApiMongo.class);
	}

	private Api mapApi(ApiMongo apiMongo){
		return (apiMongo == null) ? null : mapper.map(apiMongo, Api.class);
	}
}
