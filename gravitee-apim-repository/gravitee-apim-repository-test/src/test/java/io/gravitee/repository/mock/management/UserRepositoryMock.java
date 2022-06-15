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
package io.gravitee.repository.mock.management;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

import io.gravitee.repository.management.api.UserRepository;
import io.gravitee.repository.management.api.search.UserCriteria;
import io.gravitee.repository.management.model.User;
import io.gravitee.repository.management.model.UserStatus;
import io.gravitee.repository.mock.AbstractRepositoryMock;
import java.util.Date;
import java.util.HashSet;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class UserRepositoryMock extends AbstractRepositoryMock<UserRepository> {

    public UserRepositoryMock() {
        super(UserRepository.class);
    }

    @Override
    protected void prepare(UserRepository userRepository) throws Exception {
        final User user = mock(User.class);
        when(user.getId()).thenReturn("createuser1");
        when(user.getOrganizationId()).thenReturn("DEFAULT");
        when(user.getPassword()).thenReturn("New pwd");
        when(user.getEmail()).thenReturn("createuser1@gravitee.io");
        when(user.getLoginCount()).thenReturn(123L);
        when(user.getFirstConnectionAt()).thenReturn(new Date(1439052010883L));
        when(user.getNewsletterSubscribed()).thenReturn(null);

        final User userUpdated = mock(User.class);
        when(userUpdated.getId()).thenReturn("id2update");
        when(userUpdated.getOrganizationId()).thenReturn("new_DEFAULT");
        when(userUpdated.getSource()).thenReturn("sourceUpdated");
        when(userUpdated.getSourceId()).thenReturn("sourceIdUpdated");
        when(userUpdated.getPassword()).thenReturn("passwordUpdated");
        when(userUpdated.getEmail()).thenReturn("emailUpdated");
        when(userUpdated.getFirstname()).thenReturn("firstnameUpdated");
        when(userUpdated.getLastname()).thenReturn("lastnameUpdated");
        when(userUpdated.getPicture()).thenReturn("pictureUpdated");
        when(userUpdated.getCreatedAt()).thenReturn(new Date(1439032010883L));
        when(userUpdated.getUpdatedAt()).thenReturn(new Date(1439042010883L));
        when(userUpdated.getLastConnectionAt()).thenReturn(new Date(1439052010883L));
        when(userUpdated.getStatus()).thenReturn(UserStatus.ARCHIVED);
        when(userUpdated.getLoginCount()).thenReturn(123L);
        when(userUpdated.getFirstConnectionAt()).thenReturn(new Date(1439052010883L));
        when(userUpdated.getNewsletterSubscribed()).thenReturn(true);

        final User user0 = mock(User.class);
        when(user0.getId()).thenReturn("user0");
        final User user1 = mock(User.class);
        when(user1.getId()).thenReturn("user1");
        final User user2 = mock(User.class);
        when(user2.getId()).thenReturn("user2");
        final User user3 = mock(User.class);
        when(user3.getId()).thenReturn("user3");
        final User user4 = mock(User.class);
        when(user4.getId()).thenReturn("user4");
        final User user5 = mock(User.class);
        when(user5.getId()).thenReturn("user5");
        final User id2update = mock(User.class);
        when(id2update.getId()).thenReturn("id2update");
        final User user2delete = mock(User.class);
        when(user2delete.getId()).thenReturn("user2delete");
        final User userSpecialChar = mock(User.class);
        when(userSpecialChar.getId()).thenReturn("idSpecialChar");
        io.gravitee.common.data.domain.Page<User> searchAllResult = new io.gravitee.common.data.domain.Page<>(
            asList(user0, user1, user3, user5, user2, user4, id2update, userSpecialChar, user2delete),
            0,
            0,
            8
        );

        when(userRepository.search(isNull(UserCriteria.class), any())).thenReturn(searchAllResult);
        when(userRepository.search(argThat(o -> o != null && (o.getStatuses() == null || o.getStatuses().length == 0)), any()))
            .thenReturn(searchAllResult);
        when(
            userRepository.search(
                argThat(
                    o ->
                        o != null &&
                        o.getStatuses() != null &&
                        o.getStatuses().length == 1 &&
                        UserStatus.ARCHIVED.equals(o.getStatuses()[0])
                ),
                any()
            )
        )
            .thenReturn(new io.gravitee.common.data.domain.Page<>(singletonList(mock(User.class)), 0, 0, 1));
        when(
            userRepository.search(
                argThat(
                    o -> o != null && o.getStatuses() != null && o.getStatuses().length == 1 && UserStatus.ACTIVE.equals(o.getStatuses()[0])
                ),
                any()
            )
        )
            .thenReturn(
                new io.gravitee.common.data.domain.Page<>(
                    asList(
                        user,
                        mock(User.class),
                        mock(User.class),
                        mock(User.class),
                        mock(User.class),
                        mock(User.class),
                        mock(User.class)
                    ),
                    0,
                    0,
                    6
                )
            );
        when(userRepository.search(argThat(o -> o != null && o.getStatuses() == null && o.hasNoStatus()), any()))
            .thenReturn(new io.gravitee.common.data.domain.Page<>(asList(mock(User.class)), 0, 0, 1));
        when(userRepository.search(argThat(o -> o != null && "DEFAULT".equals(o.getOrganizationId())), any()))
            .thenReturn(new io.gravitee.common.data.domain.Page<>(asList(user0), 0, 0, 1));

        when(userRepository.create(any(User.class))).thenReturn(user);
        when(userRepository.findById("user0")).thenReturn(of(user));
        when(userRepository.findById("id2update")).thenReturn(of(userUpdated));

        when(user.getId()).thenReturn("createuser1");
        when(user.getEmail()).thenReturn("createuser1@gravitee.io");
        when(user.getStatus()).thenReturn(UserStatus.ACTIVE);

        when(userRepository.findBySource("gravitee", "createuser1", "DEFAULT")).thenReturn(of(user));

        when(userRepository.update(argThat(o -> o != null && "id2update".equals(o.getId())))).thenReturn(userUpdated);
        when(userRepository.update(argThat(o -> o == null || "unknown".equals(o.getId())))).thenThrow(new IllegalStateException());

        when(userRepository.findByEmail("user0@gravitee.io", "DEFAULT")).thenReturn(of(user0));
        when(userRepository.findByEmail("usER0@gravitee.io", "DEFAULT")).thenReturn(of(user0));

        when(userRepository.findBySource("gravitee", "user1", "DEV")).thenReturn(of(user1));
        when(userRepository.findBySource("gravitee", "USER1", "DEV")).thenReturn(of(user1));
        when(userRepository.findBySource("gravitee", "user", "DEV")).thenReturn(empty());
        when(userRepository.findBySource("sourceSpecialChar", "sourceIdSpecialChar+test@me", "DEV")).thenReturn(of(userSpecialChar));
        when(userRepository.findById("user1")).thenReturn(of(user1));
        when(userRepository.findByIds(asList("user1", "user5"))).thenReturn(new HashSet<>(asList(user1, user5)));

        when(userRepository.findById("user2delete")).thenReturn(of(new User()), empty());
    }
}
