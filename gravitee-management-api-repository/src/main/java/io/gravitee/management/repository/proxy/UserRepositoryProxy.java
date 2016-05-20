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
package io.gravitee.management.repository.proxy;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.UserRepository;
import io.gravitee.repository.management.model.User;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
@Component
public class UserRepositoryProxy extends AbstractProxy<UserRepository> implements UserRepository {

    @Override
    public User create(User user) throws TechnicalException {
        return target.create(user);
    }

    @Override
    public User update(User user) throws TechnicalException {
        return target.update(user);
    }

    @Override
    public Set<User> findAll() throws TechnicalException {
        return target.findAll();
    }

    @Override
    public Optional<User> findByUsername(String s) throws TechnicalException {
        return target.findByUsername(s);
    }
}
