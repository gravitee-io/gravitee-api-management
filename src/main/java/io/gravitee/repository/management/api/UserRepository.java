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
package io.gravitee.repository.management.api;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.User;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface UserRepository {

	/**
	 * Create a {@link User}
	 *
	 * @param user user to create
	 * @return User created
	 */
	User create(User user) throws TechnicalException;

	/**
	 * Update a {@link User}
	 *
	 * @param user user to update
	 * @return User updated
	 */
	User update(User user) throws TechnicalException;

	/**
	 * Find a {@link User} by name
	 *
	 * @param username Name of the searched user
	 * @return Option user found
	 */
	Optional<User> findByUsername(String username) throws TechnicalException;
	/**
	 * Find a list of {@link User} by name
	 *
	 * @param usernames Names of the searched users
	 * @return list of users found
	 */
	Set<User> findByUsernames(List<String> usernames) throws TechnicalException;

	/**
	 * Find all {@link User}s
	 *
	 * @return Users found
	 */
	Set<User> findAll() throws TechnicalException;
}
