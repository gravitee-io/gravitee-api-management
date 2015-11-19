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
import io.gravitee.repository.management.model.ApiMembership;
import io.gravitee.repository.management.model.MembershipType;
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
	public Optional<Api> findById(String apiName) throws TechnicalException {
		ApiMongo apiMongo =  internalApiRepo.findOne(apiName);
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
	public void delete(String apiName) throws TechnicalException {
		internalApiRepo.delete(apiName);
	}

	@Override
	public Set<Api> findByUser(String username, MembershipType membershipType) throws TechnicalException {
		return mapApis(
				(membershipType == null) ?
						internalApiRepo.findByUser(username, null) :
						internalApiRepo.findByUser(username, membershipType.toString()));
	}

	@Override
	public int countByUser(String username, MembershipType membershipType) throws TechnicalException {
		return (membershipType == null) ?
			internalApiRepo.countByUser(username, null) :
				internalApiRepo.countByUser(username, membershipType.toString());
	}

	@Override
	public Set<Api> findByApplication(String application) throws TechnicalException {
		List<ApiAssociationMongo> apiAssociationMongos = internalApiKeyRepo.findByApplication(application);
		return apiAssociationMongos.stream().map(t -> mapper.map(t.getApi(), Api.class)).collect(Collectors.toSet());
	}

	@Override
	public void addMember(ApiMembership membership) throws TechnicalException {
		ApiMongo apiMongo = internalApiRepo.findOne(membership.getApi());
		UserMongo userMongo = internalUserRepo.findOne(membership.getUser());

		MemberMongo memberMongo = new MemberMongo();
		memberMongo.setUser(userMongo);
		memberMongo.setType(membership.getMembershipType().toString());
		memberMongo.setCreatedAt(membership.getCreatedAt());
		memberMongo.setUpdatedAt(membership.getUpdatedAt());

		apiMongo.getMembers().add(memberMongo);

		internalApiRepo.save(apiMongo);
	}

	@Override
	public void deleteMember(String api, String username) throws TechnicalException {
		ApiMongo apiMongo = internalApiRepo.findOne(api);
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
	public Collection<ApiMembership> getMembers(String api) throws TechnicalException {
		ApiMongo apiMongo = internalApiRepo.findOne(api);
		List<MemberMongo> membersMongo = apiMongo.getMembers();
		Set<ApiMembership> members = new HashSet<>(membersMongo.size());

		for (MemberMongo memberMongo : membersMongo) {
			ApiMembership member = new ApiMembership();
			member.setApi(apiMongo.getName());
			member.setUser(memberMongo.getUser().getName());
			member.setMembershipType(MembershipType.valueOf(memberMongo.getType()));
			member.setCreatedAt(memberMongo.getCreatedAt());
			member.setUpdatedAt(memberMongo.getUpdatedAt());
			members.add(member);
		}

		return members;
	}

	@Override
	public ApiMembership getMember(String api, String username) throws TechnicalException {
		Collection<ApiMembership> members = getMembers(api);
		for (ApiMembership member : members) {
			if (member.getUser().equalsIgnoreCase(username)) {
				return member;
			}
		}

		return null;
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
