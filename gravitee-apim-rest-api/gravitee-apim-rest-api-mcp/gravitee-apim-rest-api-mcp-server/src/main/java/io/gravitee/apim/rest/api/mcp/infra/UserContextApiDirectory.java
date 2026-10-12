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
package io.gravitee.apim.rest.api.mcp.infra;

import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.user.domain_service.UserContextLoader;
import io.gravitee.apim.core.user.model.UserContext;
import io.gravitee.apim.rest.api.mcp.core.model.ApiRef;
import io.gravitee.apim.rest.api.mcp.core.port.ApiDirectory;
import java.util.List;

public class UserContextApiDirectory implements ApiDirectory {

    private final UserContextLoader userContextLoader;

    public UserContextApiDirectory(UserContextLoader userContextLoader) {
        this.userContextLoader = userContextLoader;
    }

    @Override
    public List<ApiRef> visibleTo(AuditInfo caller) {
        return userContextLoader
            .loadApis(new UserContext(caller))
            .apis()
            .orElse(List.of())
            .stream()
            .map(api -> new ApiRef(api.getId(), api.getName()))
            .toList();
    }
}
