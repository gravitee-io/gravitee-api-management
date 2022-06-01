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
package io.gravitee.rest.api.model.configuration.identity.google;

import io.gravitee.rest.api.model.configuration.identity.IdentityProviderType;
import io.gravitee.rest.api.model.configuration.identity.SocialIdentityProviderEntity;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GoogleIdentityProviderEntity extends SocialIdentityProviderEntity {

    private static final String AUTHORIZATION_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_URL = "https://www.googleapis.com/oauth2/v4/token";
    private static final String USER_INFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";
    private static final String GOOGLE_ACCESS_TOKEN_PROPERTY = "access_token";
    private static final String GOOGLE_AUTHORIZATION_HEADER = "Bearer %s";

    @Override
    public IdentityProviderType getType() {
        return IdentityProviderType.GOOGLE;
    }

    @Override
    public String getAuthorizationEndpoint() {
        return AUTHORIZATION_URL;
    }

    @Override
    public String getTokenEndpoint() {
        return TOKEN_URL;
    }

    @Override
    public String getUserInfoEndpoint() {
        return USER_INFO_URL;
    }

    @Override
    public List<String> getRequiredUrlParams() {
        return Collections.singletonList("scope");
    }

    @Override
    public List<String> getOptionalUrlParams() {
        return Arrays.asList("display", "state");
    }

    @Override
    public List<String> getScopes() {
        return Arrays.asList("profile", "email");
    }

    @Override
    public String getDisplay() {
        return "popup";
    }

    @Override
    public String getColor() {
        return null;
    }

    @Override
    public Map<String, String> getUserProfileMapping() {
        HashMap<String, String> userProfileMapping = new HashMap<>();
        userProfileMapping.put(UserProfile.ID, "email");
        userProfileMapping.put(UserProfile.SUB, "sub");
        userProfileMapping.put(UserProfile.EMAIL, "email");
        userProfileMapping.put(UserProfile.FIRSTNAME, "given_name");
        userProfileMapping.put(UserProfile.LASTNAME, "family_name");
        userProfileMapping.put(UserProfile.PICTURE, "picture");

        return userProfileMapping;
    }
}
