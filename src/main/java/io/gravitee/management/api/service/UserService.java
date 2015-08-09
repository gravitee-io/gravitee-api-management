package io.gravitee.management.api.service;

import io.gravitee.management.api.model.UserEntity;

import java.util.Optional;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public interface UserService {

    Optional<UserEntity> findByName(String username);
}
