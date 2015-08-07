package io.gravitee.repositories.mongodb;

import java.util.Set;

import org.springframework.stereotype.Component;

import io.gravitee.repository.api.TeamMembershipRepository;
import io.gravitee.repository.model.Member;
import io.gravitee.repository.model.TeamRole;

@Component
public class TeamMembershipRepositoryImpl implements TeamMembershipRepository {

	@Override
	public void addMember(String teamName, String username, TeamRole role) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateMember(String teamName, String username, TeamRole role) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deleteMember(String teamName, String username) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Set<Member> listMembers(String teamName) {
		// TODO Auto-generated method stub
		return null;
	}

}
