package io.gravitee.repositories.mongodb.internal.user;

import java.util.List;

import io.gravitee.repositories.mongodb.internal.model.TeamMemberMongo;

public interface UserMongoRepositoryCustom {

    List<TeamMemberMongo> findByTeam(String teamName);
}
