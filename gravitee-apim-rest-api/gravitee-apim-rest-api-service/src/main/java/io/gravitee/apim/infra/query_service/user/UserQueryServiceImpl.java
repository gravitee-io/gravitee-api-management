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
package io.gravitee.apim.infra.query_service.user;

import io.gravitee.apim.core.user.model.User;
import io.gravitee.apim.core.user.model.UserSearchQuery;
import io.gravitee.apim.core.user.query_service.UserQueryService;
import io.gravitee.apim.infra.adapter.SearchUserAdapter;
import io.gravitee.rest.api.idp.core.authentication.IdentityManager;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserQueryServiceImpl implements UserQueryService {

    private final IdentityManager identityManager;

    @Override
    public List<User> search(UserSearchQuery searchQuery) {
        return identityManager.search(searchQuery.query()).stream().map(SearchUserAdapter.INSTANCE::toUser).toList();
    }
}
