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
package io.gravitee.repository.redis.management;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.UserRepository;
import io.gravitee.repository.management.model.User;
import io.gravitee.repository.redis.management.internal.UserRedisRepository;
import io.gravitee.repository.redis.management.model.RedisUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class RedisUserRepository implements UserRepository {

    @Autowired
    private UserRedisRepository userRedisRepository;

    @Override
    public User create(User user) throws TechnicalException {
        RedisUser redisUser = userRedisRepository.saveOrUpdate(convert(user));
        return convert(redisUser);
    }

    @Override
    public User update(User user) throws TechnicalException {
        RedisUser redisUser = userRedisRepository.saveOrUpdate(convert(user));
        return convert(redisUser);
    }

    @Override
    public Optional<User> findByUsername(String username) throws TechnicalException {
        RedisUser redisUser = userRedisRepository.find(username);
        return Optional.ofNullable(convert(redisUser));
    }

    @Override
    public Set<User> findByUsernames(List<String> usernames) throws TechnicalException {
        return userRedisRepository.find(usernames).stream()
                .map(this::convert)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<User> findAll() throws TechnicalException {
        Set<RedisUser> users = userRedisRepository.findAll();

        return users.stream()
                .map(this::convert)
                .collect(Collectors.toSet());
    }

    private User convert(RedisUser redisUser) {
        if (redisUser == null) {
            return null;
        }

        User user = new User();
        user.setUsername(redisUser.getUsername());
        user.setEmail(redisUser.getEmail());
        user.setFirstname(redisUser.getFirstname());
        user.setLastname(redisUser.getLastname());
        user.setPassword(redisUser.getPassword());
        user.setRoles(redisUser.getRoles());
        user.setCreatedAt(new Date(redisUser.getCreatedAt()));
        user.setUpdatedAt(new Date(redisUser.getUpdatedAt()));
        user.setPicture(redisUser.getPicture());
        user.setSource(redisUser.getSource());
        user.setSourceId(redisUser.getSourceId());

        if (redisUser.getLastConnectionAt() != 0) {
            user.setLastConnectionAt(new Date(redisUser.getLastConnectionAt()));
        }

        return user;
    }

    private RedisUser convert(User user) {
        RedisUser redisUser = new RedisUser();
        redisUser.setUsername(user.getUsername());
        redisUser.setEmail(user.getEmail());
        redisUser.setFirstname(user.getFirstname());
        redisUser.setLastname(user.getLastname());
        redisUser.setPassword(user.getPassword());
        redisUser.setRoles(user.getRoles());
        redisUser.setCreatedAt(user.getCreatedAt().getTime());
        redisUser.setUpdatedAt(user.getUpdatedAt().getTime());
        redisUser.setPicture(user.getPicture());
        redisUser.setSource(user.getSource());
        redisUser.setSourceId(user.getSourceId());

        if (user.getLastConnectionAt() != null) {
            redisUser.setLastConnectionAt(user.getLastConnectionAt().getTime());
        }
        return redisUser;
    }
}
