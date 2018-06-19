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
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.User;
import org.junit.Assert;
import org.junit.Test;

import java.util.Date;
import java.util.List;
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

        assertNotNull("User created is null", userCreated);

        Optional<User> optional = userRepository.findByUsername(username);

        Assert.assertTrue("Unable to find saved user", optional.isPresent());
        User userFound = optional.get();

        assertEquals("Invalid saved user name.", user.getUsername(), userFound.getUsername());
        assertEquals("Invalid saved user mail.", user.getEmail(), userFound.getEmail());
    }

    @Test
    public void shouldUpdate() throws Exception {
        Optional<User> optional = userRepository.findById("id2update");
        Assert.assertTrue("userRepository to update not found", optional.isPresent());

        final User user = optional.get();
        user.setUsername("usernameUpdated");
        user.setSource("sourceUpdated");
        user.setSourceId("sourceIdUpdated");
        user.setPassword("passwordUpdated");
        user.setEmail("emailUpdated");
        user.setFirstname("firstnameUpdated");
        user.setLastname("lastnameUpdated");
        user.setPicture("pictureUpdated");
        user.setCreatedAt(new Date(1439032010883L));
        user.setUpdatedAt(new Date(1439042010883L));
        user.setLastConnectionAt(new Date(1439052010883L));

        long nbUsersBeforeUpdate = userRepository.search(
                new PageableBuilder().pageNumber(0).pageSize(Integer.MAX_VALUE).build()
        ).getTotalElements();
        userRepository.update(user);
        long nbUsersAfterUpdate = userRepository.search(
                new PageableBuilder().pageNumber(0).pageSize(Integer.MAX_VALUE).build()
        ).getTotalElements();

        assertEquals(nbUsersBeforeUpdate, nbUsersAfterUpdate);

        Optional<User> optionalUpdated = userRepository.findById("id2update");
        Assert.assertTrue("User to update not found", optionalUpdated.isPresent());

        final User userUpdated = optionalUpdated.get();
        assertEquals("Invalid saved username", "usernameUpdated", userUpdated.getUsername());
        assertEquals("Invalid saved source", "sourceUpdated", userUpdated.getSource());
        assertEquals("Invalid saved sourceId", "sourceIdUpdated", userUpdated.getSourceId());
        assertEquals("Invalid saved password", "passwordUpdated", userUpdated.getPassword());
        assertEquals("Invalid saved email", "emailUpdated", userUpdated.getEmail());
        assertEquals("Invalid saved firstname", "firstnameUpdated", userUpdated.getFirstname());
        assertEquals("Invalid saved lastname", "lastnameUpdated", userUpdated.getLastname());
        assertEquals("Invalid saved picture", "pictureUpdated", userUpdated.getPicture());
        assertEquals("Invalid saved createDate", new Date(1439032010883L), userUpdated.getCreatedAt());
        assertEquals("Invalid saved updateDate", new Date(1439042010883L), userUpdated.getUpdatedAt());
        assertEquals("Invalid saved lastConnection", new Date(1439052010883L), userUpdated.getLastConnectionAt());
    }

    @Test
    public void shouldSearchAll() throws Exception {
        List<User> users = userRepository.search(
                new PageableBuilder().pageNumber(0).pageSize(Integer.MAX_VALUE).build()
        ).getContent();

        Assert.assertNotNull(users);
        assertEquals("Invalid user numbers in find all", 8, users.size());
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

    @Test
    public void shouldDelete() throws Exception {
        assertTrue("user2delete exists", userRepository.findById("user2delete").isPresent());
        userRepository.delete("user2delete");
        assertFalse("user2delete not exists", userRepository.findById("user2delete").isPresent());
    }
}
