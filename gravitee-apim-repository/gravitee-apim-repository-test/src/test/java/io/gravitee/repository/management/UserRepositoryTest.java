/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.management;

import static io.gravitee.repository.utils.DateUtils.compareDate;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.UserCriteria;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.User;
import io.gravitee.repository.management.model.UserStatus;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class UserRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/user-tests/";
    }

    @Test
    public void createUserTest() throws Exception {
        String username = "createuser1";

        User user = new User();
        user.setId("createuser1");
        user.setOrganizationId("DEFAULT");
        user.setCreatedAt(new Date());
        user.setUpdatedAt(user.getCreatedAt());
        user.setEmail(String.format("%s@gravitee.io", username));
        user.setStatus(UserStatus.ACTIVE);
        user.setSource("gravitee");
        user.setSourceId("createuser1");
        user.setLoginCount(123);
        user.setFirstConnectionAt(new Date(1439052010883L));
        user.setNewsletterSubscribed(false);
        User userCreated = userRepository.create(user);

        assertNotNull(userCreated, "User created is null");

        Optional<User> optional = userRepository.findBySource("gravitee", "createuser1", "DEFAULT");

        assertTrue(optional.isPresent(), "Unable to find saved user");
        User userFound = optional.get();

        assertEquals(user.getOrganizationId(), userFound.getOrganizationId(), "Invalid saved organization id.");
        assertEquals(user.getId(), userFound.getId(), "Invalid saved user name.");
        assertEquals(user.getEmail(), userFound.getEmail(), "Invalid saved user mail.");
        assertEquals(user.getStatus(), userFound.getStatus(), "Invalid saved user status.");
        assertEquals(user.getLoginCount(), userFound.getLoginCount(), "Invalid saved user login count.");
        assertEquals(user.getFirstConnectionAt(), userFound.getFirstConnectionAt(), "Invalid saved user first connection at.");
        assertEquals(user.getNewsletterSubscribed(), false, "Invalid saved user newsletter.");
    }

    @Test
    public void shouldUpdate() throws Exception {
        Optional<User> optional = userRepository.findById("id2update");
        assertTrue(optional.isPresent(), "userRepository to update not found");

        final User user = optional.get();
        user.setSource("sourceUpdated");
        user.setOrganizationId("new_DEFAULT");
        user.setSourceId("sourceIdUpdated");
        user.setPassword("passwordUpdated");
        user.setEmail("emailUpdated");
        user.setFirstname("firstnameUpdated");
        user.setLastname("lastnameUpdated");
        user.setPicture("pictureUpdated");
        user.setStatus(UserStatus.ARCHIVED);
        user.setCreatedAt(new Date(1439032010883L));
        user.setUpdatedAt(new Date(1439042010883L));
        user.setLastConnectionAt(new Date(1439052010883L));
        user.setLoginCount(123);
        user.setNewsletterSubscribed(true);
        user.setFirstConnectionAt(new Date(1439052010883L));

        long nbUsersBeforeUpdate = userRepository
            .search(null, new PageableBuilder().pageNumber(0).pageSize(Integer.MAX_VALUE).build())
            .getTotalElements();
        userRepository.update(user);
        long nbUsersAfterUpdate = userRepository
            .search(null, new PageableBuilder().pageNumber(0).pageSize(Integer.MAX_VALUE).build())
            .getTotalElements();

        assertEquals(nbUsersBeforeUpdate, nbUsersAfterUpdate);

        Optional<User> optionalUpdated = userRepository.findById("id2update");
        assertTrue(optionalUpdated.isPresent(), "User to update not found");

        final User userUpdated = optionalUpdated.get();
        assertEquals("new_DEFAULT", userUpdated.getOrganizationId(), "Invalid saved organization id.");
        assertEquals("sourceUpdated", userUpdated.getSource(), "Invalid saved source");
        assertEquals("sourceIdUpdated", userUpdated.getSourceId(), "Invalid saved sourceId");
        assertEquals("passwordUpdated", userUpdated.getPassword(), "Invalid saved password");
        assertEquals("emailUpdated", userUpdated.getEmail(), "Invalid saved email");
        assertEquals("firstnameUpdated", userUpdated.getFirstname(), "Invalid saved firstname");
        assertEquals("lastnameUpdated", userUpdated.getLastname(), "Invalid saved lastname");
        assertEquals("pictureUpdated", userUpdated.getPicture(), "Invalid saved picture");
        assertTrue(compareDate(new Date(1439032010883L), userUpdated.getCreatedAt()), "Invalid saved createDate");
        assertTrue(compareDate(new Date(1439042010883L), userUpdated.getUpdatedAt()), "Invalid saved updateDate");
        assertTrue(compareDate(new Date(1439052010883L), userUpdated.getLastConnectionAt()), "Invalid saved lastConnection");
        assertEquals(UserStatus.ARCHIVED, userUpdated.getStatus(), "Invalid status");
        assertEquals(123, userUpdated.getLoginCount(), "Invalid saved user login count.");
        assertEquals(true, userUpdated.getNewsletterSubscribed(), "Invalid saved user newsletter subscribed");
        assertEquals(user.getFirstConnectionAt(), userUpdated.getFirstConnectionAt(), "Invalid saved user first connection at.");
    }

    @Test
    public void shouldSearchAllWithEnvironment() throws Exception {
        List<User> users = userRepository
            .search(
                new UserCriteria.Builder().organizationId("DEFAULT").build(),
                new PageableBuilder().pageNumber(0).pageSize(Integer.MAX_VALUE).build()
            )
            .getContent();

        assertNotNull(users);
        assertEquals(1, users.size(), "Invalid user numbers in search");
        assertEquals("user0", users.getFirst().getId());
    }

    @Test
    public void shouldSearchAllWithNullCriteria() throws Exception {
        List<User> users = userRepository
            .search(null, new PageableBuilder().pageNumber(0).pageSize(Integer.MAX_VALUE).build())
            .getContent();

        assertNotNull(users);
        assertEquals(11, users.size(), "Invalid user numbers in search");
        assertEquals("id2update", users.get(0).getId());
        assertEquals("idSpecialChar", users.get(1).getId());
        assertEquals("user0", users.get(2).getId());
        assertEquals("user1", users.get(3).getId());
        assertEquals("user2", users.get(4).getId());
        assertEquals("user2delete", users.get(5).getId());
        assertEquals("user3", users.get(6).getId());
        assertEquals("user3delete", users.get(7).getId());
        assertEquals("user4", users.get(8).getId());
        assertEquals("user4delete", users.get(9).getId());
        assertEquals("user5", users.get(10).getId());
    }

    @Test
    public void shouldSearchAllWithEmptyCriteria() throws Exception {
        List<User> users = userRepository
            .search(new UserCriteria.Builder().build(), new PageableBuilder().pageNumber(0).pageSize(Integer.MAX_VALUE).build())
            .getContent();
    }

    @Test
    public void shouldSearchArchivedUsers() throws Exception {
        List<User> users = userRepository
            .search(
                new UserCriteria.Builder().statuses(UserStatus.ARCHIVED).build(),
                new PageableBuilder().pageNumber(0).pageSize(Integer.MAX_VALUE).build()
            )
            .getContent();

        Assertions.assertNotNull(users);
        assertEquals(1, users.size(), "Invalid user numbers in find archived");
    }

    @Test
    public void findUserBySourceCaseInsensitive() throws Exception {
        Optional<User> user1 = userRepository.findBySource("gravitee", "user1", "DEV");
        Optional<User> user1Upper = userRepository.findBySource("gravitee", "USER1", "DEV");
        assertTrue(user1.isPresent());
        assertTrue(user1Upper.isPresent());
        assertEquals(user1.get().getId(), user1Upper.get().getId());
    }

    @Test
    public void findUserByEmail() throws Exception {
        List<User> user1 = userRepository.findByEmail("user0@gravitee.io", "DEFAULT");
        List<User> user1Upper = userRepository.findByEmail("usER0@gravitee.io", "DEFAULT");
        assertFalse(user1.isEmpty());
        assertFalse(user1Upper.isEmpty());
        assertEquals(user1.getFirst().getId(), user1Upper.getFirst().getId());
    }

    @Test
    public void findUserBySourceSpecialCharacters() throws Exception {
        Optional<User> user = userRepository.findBySource("sourceSpecialChar", "sourceIdSpecialChar+test@me [IT] & others", "DEV");
        assertTrue(user.isPresent());
    }

    @Test
    public void shouldSearchUsersWithNoStatus() throws Exception {
        List<User> users = userRepository
            .search(new UserCriteria.Builder().noStatus().build(), new PageableBuilder().pageNumber(0).pageSize(Integer.MAX_VALUE).build())
            .getContent();

        Assertions.assertNotNull(users);
        assertEquals(1, users.size(), "Invalid user numbers in find no status");
    }

    @Test
    public void shouldSearchActiveUsers() throws Exception {
        List<User> users = userRepository
            .search(
                new UserCriteria.Builder().statuses(UserStatus.ACTIVE).build(),
                new PageableBuilder().pageNumber(0).pageSize(Integer.MAX_VALUE).build()
            )
            .getContent();

        Assertions.assertNotNull(users);
        assertEquals(9, users.size(), "Invalid user numbers in find active");
    }

    @Test
    public void findUserBySourceTest() throws Exception {
        Optional<User> user = userRepository.findBySource("gravitee", "user1", "DEV");
        Assertions.assertTrue(user.isPresent());
    }

    @Test
    public void shouldNotUpdateUnknownUser() throws Exception {
        assertThrows(IllegalStateException.class, () -> {
            User unknownUser = new User();
            unknownUser.setId("unknown");
            userRepository.update(unknownUser);
            fail("An unknown user should not be updated");
        });
    }

    @Test
    public void shouldNotUpdateNull() throws Exception {
        assertThrows(IllegalStateException.class, () -> {
            userRepository.update(null);
            fail("A null user should not be updated");
        });
    }

    @Test
    public void shouldFindById() throws Exception {
        final Optional<User> optionalUser = userRepository.findById("user1");

        assertTrue(optionalUser.isPresent());
        assertEquals("user1", optionalUser.get().getId(), "User not found by its id");
    }

    @Test
    public void shouldFindByIds() throws Exception {
        final Set<User> users = userRepository.findByIds(asList("user1", "user5"));

        assertNotNull(users);
        assertEquals(2, users.size());
        assertTrue(users.stream().map(User::getId).toList().containsAll(asList("user1", "user5")));
    }

    @Test
    public void shouldDelete() throws Exception {
        assertTrue(userRepository.findById("user2delete").isPresent(), "user2delete exists");
        userRepository.delete("user2delete");
        assertFalse(userRepository.findById("user2delete").isPresent(), "user2delete not exists");
    }

    @Test
    public void should_delete_by_organization_id() throws Exception {
        Page<User> usersBeforeDelete = userRepository.search(
            new UserCriteria.Builder().organizationId("ToBeDeleted").build(),
            new PageableBuilder().pageNumber(0).pageSize(Integer.MAX_VALUE).build()
        );

        List<String> deleted = userRepository.deleteByOrganizationId("ToBeDeleted");

        long nbUsersAfterDelete = userRepository
            .search(
                new UserCriteria.Builder().organizationId("ToBeDeleted").build(),
                new PageableBuilder().pageNumber(0).pageSize(Integer.MAX_VALUE).build()
            )
            .getTotalElements();
        assertEquals(usersBeforeDelete.getTotalElements(), deleted.size());
        assertEquals(usersBeforeDelete.getContent().size(), deleted.size());
        assertTrue(usersBeforeDelete.getContent().stream().map(User::getId).toList().containsAll(deleted));
        assertEquals(0, nbUsersAfterDelete);
    }

    @Test
    public void shouldFailUniqueConstraintOrgIdSourceSourceId() throws Exception {
        final Callable<User> createUser = () -> {
            final var user = new User();

            // this triplet must be unique
            user.setOrganizationId("DEFAULT");
            user.setSource("gravitee");
            user.setSourceId("user@mail.com");

            user.setId(String.valueOf(new java.util.Random().nextInt()));
            user.setCreatedAt(new Date());
            user.setUpdatedAt(user.getCreatedAt());
            user.setEmail("user@mail.com");
            user.setStatus(UserStatus.ACTIVE);
            user.setLoginCount(123);
            user.setFirstConnectionAt(new Date(1439052010883L));
            user.setNewsletterSubscribed(false);

            return userRepository.create(user);
        };

        final var initialSize = userRepository.findAll().size();
        // first creation succeeds
        assertNotNull(createUser.call());
        // second creation fails
        assertThrows(TechnicalException.class, createUser::call);
        // only one user was created
        assertEquals(initialSize + 1, userRepository.findAll().size());
    }
}
