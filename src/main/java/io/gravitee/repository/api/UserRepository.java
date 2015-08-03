package io.gravitee.repository.api;

import io.gravitee.repository.model.User;

import java.util.Set;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public interface UserRepository {

    User findByUsername(String username);

    Set<User> findAll();

    Set<User> findByTeam(String teamName);
}
