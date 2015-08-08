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
package io.gravitee.repository.api;

import java.util.Optional;
import java.util.Set;

import io.gravitee.repository.model.User;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public interface UserRepository {

	User create(User user);
	
	Optional<User> findByUsername(String username);

    Set<User> findAll();

    Set<User> findByTeam(String teamName);
}
