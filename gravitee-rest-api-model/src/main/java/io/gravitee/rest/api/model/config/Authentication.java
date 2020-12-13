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
package io.gravitee.rest.api.model.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.gravitee.rest.api.model.annotations.ParameterKey;
import io.gravitee.rest.api.model.parameters.Key;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Authentication {
    @ParameterKey(Key.AUTHENTICATION_FORCELOGIN_ENABLED)
    private ConsoleConfigEntity.Enabled forceLogin;
    @ParameterKey(Key.AUTHENTICATION_LOCALLOGIN_ENABLED)
    private ConsoleConfigEntity.Enabled localLogin;
    private ConsoleConfigEntity.GoogleAuthentication google;
    private ConsoleConfigEntity.GithubAuthentication github;
    private ConsoleConfigEntity.OAuth2Authentication oauth2;

    public Authentication() {
        google = new ConsoleConfigEntity.GoogleAuthentication();
        github = new ConsoleConfigEntity.GithubAuthentication();
        oauth2 = new ConsoleConfigEntity.OAuth2Authentication();
    }

    public ConsoleConfigEntity.Enabled getForceLogin() {
        return forceLogin;
    }

    public void setForceLogin(ConsoleConfigEntity.Enabled forceLogin) {
        this.forceLogin = forceLogin;
    }

    public ConsoleConfigEntity.Enabled getLocalLogin() {
        return localLogin;
    }

    public void setLocalLogin(ConsoleConfigEntity.Enabled localLogin) {
        this.localLogin = localLogin;
    }

    public ConsoleConfigEntity.GoogleAuthentication getGoogle() {
        return google;
    }

    public void setGoogle(ConsoleConfigEntity.GoogleAuthentication google) {
        this.google = google;
    }

    public ConsoleConfigEntity.GithubAuthentication getGithub() {
        return github;
    }

    public void setGithub(ConsoleConfigEntity.GithubAuthentication github) {
        this.github = github;
    }

    public ConsoleConfigEntity.OAuth2Authentication getOauth2() {
        return oauth2;
    }

    public void setOauth2(ConsoleConfigEntity.OAuth2Authentication oauth2) {
        this.oauth2 = oauth2;
    }
}
