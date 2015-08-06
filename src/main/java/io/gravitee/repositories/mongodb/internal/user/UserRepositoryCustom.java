package io.gravitee.repositories.mongodb.internal.user;

import java.util.Set;

import io.gravitee.repositories.mongodb.internal.model.User;

public interface UserRepositoryCustom {

    Set<User> findByTeam(String teamName);
}
