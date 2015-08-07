package io.gravitee.repositories.mongodb;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.gravitee.repositories.mongodb.internal.model.TeamMemberMongo;
import io.gravitee.repositories.mongodb.internal.model.UserMongo;
import io.gravitee.repositories.mongodb.internal.user.UserMongoRepository;
import io.gravitee.repositories.mongodb.mapper.GraviteeMapper;
import io.gravitee.repository.api.UserRepository;
import io.gravitee.repository.model.User;

@Component
public class UserRepositoryImpl implements UserRepository{

	@Autowired
	private UserMongoRepository internalUserRepo;

	@Autowired
	private GraviteeMapper mapper;

	@Override
	public User findByUsername(String username) {
		
		UserMongo user = internalUserRepo.findOne(username);
		return mapper.map(user, User.class);
	}

	@Override
	public Set<User> findAll() {
		
		List<UserMongo> users = internalUserRepo.findAll();
		Set<User> res = mapper.collection2set(users, UserMongo.class, User.class);
		return res;
	}

	@Override
	public Set<User> findByTeam(String teamName) {
		
		List<TeamMemberMongo> members = internalUserRepo.findByTeam(teamName);
	
		Set<User> res = new HashSet<>();
		for (TeamMemberMongo member : members) {
			res.add(mapper.map(member.getMember(), User.class));
		}
		
		return res;
	}

	@Override
	public User create(User user) {
		
		UserMongo userMongo = mapper.map(user, UserMongo.class);
		UserMongo createdUserMongo = internalUserRepo.insert(userMongo);
		return mapper.map(createdUserMongo, User.class);
	}

	
}
