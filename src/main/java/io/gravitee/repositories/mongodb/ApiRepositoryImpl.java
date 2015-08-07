package io.gravitee.repositories.mongodb;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.gravitee.repositories.mongodb.internal.api.ApiMongoRepository;
import io.gravitee.repositories.mongodb.internal.model.ApiMongo;
import io.gravitee.repositories.mongodb.internal.team.TeamMongoRepository;
import io.gravitee.repositories.mongodb.internal.user.UserMongoRepository;
import io.gravitee.repositories.mongodb.mapper.GraviteeMapper;
import io.gravitee.repository.api.ApiRepository;
import io.gravitee.repository.model.Api;
import io.gravitee.repository.model.OwnerType;

@Component
public class ApiRepositoryImpl implements ApiRepository {

	@Autowired
	private ApiMongoRepository internalApiRepo;

	@Autowired
	private TeamMongoRepository internalTeamRepo;
	
	@Autowired
	private UserMongoRepository internalUserRepo;
	
	@Autowired
	private GraviteeMapper mapper;
	
	
	@Override
	public Optional<Api> findByName(String apiName) {
		
		ApiMongo apiMongo =  internalApiRepo.findOne(apiName);
		return  Optional.ofNullable(mapper.map(apiMongo, Api.class));
	}

	@Override
	public Set<Api> findAll() {
		
		List<ApiMongo> apis = internalApiRepo.findAll();
		return mapper.collection2set(apis, ApiMongo.class, Api.class);
	}

	//TODO externalize
	private ApiMongo mapApi(Api api){
		
		ApiMongo apiMongo = mapper.map(api, ApiMongo.class);
		
		if(OwnerType.USER.equals(api.getOwnerType())){
			apiMongo.setOwner(internalUserRepo.findOne(api.getOwner()));
		}else{
			apiMongo.setOwner(internalTeamRepo.findOne(api.getOwner()));
		}
		return apiMongo;
	}

	@Override
	public Api create(Api api) {
		
		ApiMongo apiMongo = mapApi(api);
		ApiMongo apiMongoCreated = internalApiRepo.insert(apiMongo);
		return mapper.map(apiMongoCreated, Api.class);
	}

	@Override
	public Api update(Api api) {
		
		ApiMongo apiMongo =	mapApi(api);
		ApiMongo apiMongoUpdated = internalApiRepo.save(apiMongo);
		return mapper.map(apiMongoUpdated, Api.class);
	}

	@Override
	public void delete(String apiName) {
		internalApiRepo.delete(apiName);
	}



	@Override
	public Set<Api> findByUser(String username) {
		List<ApiMongo> apis = internalApiRepo.findByUser(username);
		return mapper.collection2set(apis, ApiMongo.class, Api.class);
	}


	@Override
	public Set<Api> findByTeam(String teamName) {
	
		List<ApiMongo> apis = internalApiRepo.findByTeam(teamName);
		return mapper.collection2set(apis, ApiMongo.class, Api.class);
	}
	
	@Override
	public int countByUser(String username) {
		return (int) internalApiRepo.countByUser(username);

	}

	@Override
	public int countByTeam(String teamName) {
		return (int) internalApiRepo.countByTeam(teamName);	
	}
	
	
}
