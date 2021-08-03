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
package io.gravitee.rest.api.model.settings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class CommonAuthentication {

    private GoogleAuthentication google = new GoogleAuthentication();
    private GithubAuthentication github = new GithubAuthentication();
    private OAuth2Authentication oauth2 = new OAuth2Authentication();

    public GoogleAuthentication getGoogle() {
        return google;
    }

    public void setGoogle(GoogleAuthentication google) {
        this.google = google;
    }

    public GithubAuthentication getGithub() {
        return github;
    }

    public void setGithub(GithubAuthentication github) {
        this.github = github;
    }

    public OAuth2Authentication getOauth2() {
        return oauth2;
    }

    public void setOauth2(OAuth2Authentication oauth2) {
        this.oauth2 = oauth2;
    }

    public static class GoogleAuthentication {

        //        @ParameterKey(Key.AUTHENTICATION_GOOGLE_CLIENTID)
        private String clientId;

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }
    }

    public static class GithubAuthentication {

        //        @ParameterKey(Key.AUTHENTICATION_GITHUB_CLIENTID)
        private String clientId;

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }
    }

    public static class OAuth2Authentication {

        //        @ParameterKey(Key.AUTHENTICATION_OAUTH2_CLIENTID)
        private String clientId;

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }
    }
}
