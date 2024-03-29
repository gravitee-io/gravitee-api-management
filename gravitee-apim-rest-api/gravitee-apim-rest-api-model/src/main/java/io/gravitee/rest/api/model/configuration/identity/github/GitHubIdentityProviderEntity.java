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
package io.gravitee.rest.api.model.configuration.identity.github;

import io.gravitee.rest.api.model.configuration.identity.IdentityProviderType;
import io.gravitee.rest.api.model.configuration.identity.SocialIdentityProviderEntity;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GitHubIdentityProviderEntity extends SocialIdentityProviderEntity {

    private static final String AUTHORIZATION_URL = "https://github.com/login/oauth/authorize";
    private static final String ACCESS_TOKEN_URL = "https://github.com/login/oauth/access_token";
    private static final String USER_INFO_URL = "https://api.github.com/user";

    private static final String AUTHORIZATION_HEADER = "token %s";

    @Override
    public IdentityProviderType getType() {
        return IdentityProviderType.GITHUB;
    }

    @Override
    public String getAuthorizationEndpoint() {
        return AUTHORIZATION_URL;
    }

    @Override
    public String getTokenEndpoint() {
        return ACCESS_TOKEN_URL;
    }

    @Override
    public String getUserInfoEndpoint() {
        return USER_INFO_URL;
    }

    @Override
    public String getAuthorizationHeader() {
        return AUTHORIZATION_HEADER;
    }

    @Override
    public List<String> getRequiredUrlParams() {
        return null;
    }

    @Override
    public List<String> getOptionalUrlParams() {
        return Collections.singletonList("scope");
    }

    @Override
    public List<String> getScopes() {
        return Collections.singletonList("user:email");
    }

    @Override
    public String getDisplay() {
        return null;
    }

    @Override
    public String getColor() {
        return null;
    }

    @Override
    public Map<String, String> getUserProfileMapping() {
        HashMap<String, String> userProfileMapping = new HashMap<>();
        userProfileMapping.put(UserProfile.ID, isEmailRequired() ? "email" : "id");
        userProfileMapping.put(UserProfile.SUB, "id");
        userProfileMapping.put(UserProfile.EMAIL, "email");
        userProfileMapping.put(UserProfile.FIRSTNAME, "{#jsonPath(#profile, '$.name').split(' ')[0]}");
        userProfileMapping.put(
            UserProfile.LASTNAME,
            "{#jsonPath(#profile, '$.name').split(' ').length == 1 ? #jsonPath(#profile, '$.name').split(' ')[0] : #jsonPath(#profile, '$.name').split(' ')[1]}"
        );
        userProfileMapping.put(UserProfile.PICTURE, "avatar_url");

        return userProfileMapping;
    }
}
