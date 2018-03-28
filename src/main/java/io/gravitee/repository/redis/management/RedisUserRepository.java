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

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.UserRepository;
import io.gravitee.repository.management.api.search.Pageable;
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
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class RedisUserRepository implements UserRepository {

    @Autowired
    private UserRedisRepository userRedisRepository;

    @Override
    public Optional<User> findById(String userId) throws TechnicalException {
        RedisUser redisUser = this.userRedisRepository.find(userId);
        return Optional.ofNullable(convert(redisUser));
    }

    @Override
    public User create(User user) throws TechnicalException {
        RedisUser redisUser = userRedisRepository.saveOrUpdate(convert(user));
        return convert(redisUser);
    }

    @Override
    public void delete(String id) throws TechnicalException {
        userRedisRepository.delete(id);
    }

    @Override
    public User update(User user) throws TechnicalException {
        if (user == null || user.getUsername() == null) {
            throw new IllegalStateException("User to update must have a username");
        }

        final RedisUser redisUser = userRedisRepository.find(user.getId());

        if (redisUser == null) {
            throw new IllegalStateException(String.format("No user found with username [%s]", user.getUsername()));
        }

        RedisUser redisUserUdated = userRedisRepository.saveOrUpdate(convert(user));
        return convert(redisUserUdated);
    }

    @Override
    public Optional<User> findByUsername(String username) throws TechnicalException {
        RedisUser redisUser = userRedisRepository.findByUsername(username);
        return Optional.ofNullable(convert(redisUser));
    }

    @Override
    public Set<User> findByIds(List<String> ids) throws TechnicalException {
        return userRedisRepository.find(ids).stream()
                .map(this::convert)
                .collect(Collectors.toSet());
    }

    @Override
    public Page<User> search(Pageable pageable) throws TechnicalException {
        Page<RedisUser> redisUserPage = userRedisRepository.search(pageable);
        return new Page<>(
                redisUserPage.getContent()
                        .stream()
                        .map(this::convert)
                        .collect(Collectors.toList()),
                redisUserPage.getPageNumber(),
                Long.valueOf(redisUserPage.getPageElements()).intValue(),
                redisUserPage.getTotalElements());
    }

    private User convert(RedisUser redisUser) {
        if (redisUser == null) {
            return null;
        }

        User user = new User();
        user.setId(redisUser.getId());
        user.setUsername(redisUser.getUsername());
        user.setEmail(redisUser.getEmail());
        user.setFirstname(redisUser.getFirstname());
        user.setLastname(redisUser.getLastname());
        user.setPassword(redisUser.getPassword());
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
        redisUser.setId(user.getId());
        redisUser.setUsername(user.getUsername());
        redisUser.setEmail(user.getEmail());
        redisUser.setFirstname(user.getFirstname());
        redisUser.setLastname(user.getLastname());
        redisUser.setPassword(user.getPassword());
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
