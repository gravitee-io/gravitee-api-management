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
package io.gravitee.repository;

import io.gravitee.repository.config.AbstractRepositoryTest;
import io.gravitee.repository.management.model.User;
import org.junit.Assert;
import org.junit.Test;

import java.util.Date;
import java.util.Optional;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.*;

public class UserRepositoryTest extends AbstractRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/user-tests/";
    }

    @Test
    public void createUserTest() throws Exception {
        String username = "createuser1";

        User user = new User();
        user.setId("user-id");
        user.setUsername(username);
        user.setCreatedAt(new Date());
        user.setUpdatedAt(user.getCreatedAt());
        user.setEmail(String.format("%s@gravitee.io", username));
        User userCreated = userRepository.create(user);

        Assert.assertNotNull("User created is null", userCreated);

        Optional<User> optional = userRepository.findByUsername(username);

        Assert.assertTrue("Unable to find saved user", optional.isPresent());
        User userFound = optional.get();

        assertEquals("Invalid saved user name.", user.getUsername(), userFound.getUsername());
        assertEquals("Invalid saved user mail.", user.getEmail(), userFound.getEmail());
    }

    @Test
    public void shouldUpdate() throws Exception {
        Optional<User> optional = userRepository.findById("user0");
        Assert.assertTrue("userRepository to update not found", optional.isPresent());

        final User user = optional.get();
        user.setPassword("New pwd");

        int nbUsersBeforeUpdate = userRepository.findAll().size();
        userRepository.update(user);
        int nbUsersAfterUpdate = userRepository.findAll().size();

        assertEquals(nbUsersBeforeUpdate, nbUsersAfterUpdate);

        Optional<User> optionalUpdated = userRepository.findById("user0");
        Assert.assertTrue("User to update not found", optionalUpdated.isPresent());

        final User userUpdated = optionalUpdated.get();
        assertEquals("Invalid saved user password.", "New pwd", userUpdated.getPassword());
    }

    @Test
    public void findAllTest() throws Exception {
        Set<User> users = userRepository.findAll();

        Assert.assertNotNull(users);
        assertEquals("Invalid user numbers in find all", 6, users.size());
    }

    @Test
    public void findUserByNameTest() throws Exception {
        Optional<User> user = userRepository.findByUsername("user0 name");
        Assert.assertTrue(user.isPresent());
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateUnknownUser() throws Exception {
        User unknownUser = new User();
        unknownUser.setId("unknown");
        userRepository.update(unknownUser);
        fail("An unknown user should not be updated");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateNull() throws Exception {
        userRepository.update(null);
        fail("A null user should not be updated");
    }

    @Test
    public void shouldFindById() throws Exception {
        final Optional<User> optionalUser = userRepository.findById("user1");

        assertTrue(optionalUser.isPresent());
        assertEquals("User not found by its id", "user1 name", optionalUser.get().getUsername());
    }

    @Test
    public void shouldFindByIds() throws Exception {
        final Set<User> users = userRepository.findByIds(asList("user1", "user5"));

        assertNotNull(users);
        assertEquals(2, users.size());
        assertTrue(users.stream().map(User::getUsername).collect(toList()).containsAll(asList("user1 name", "user5 name")));
    }
}
