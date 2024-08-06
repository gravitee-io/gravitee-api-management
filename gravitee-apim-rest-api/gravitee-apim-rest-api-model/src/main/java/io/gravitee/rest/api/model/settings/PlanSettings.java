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
package io.gravitee.rest.api.model.settings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.gravitee.rest.api.model.annotations.ParameterKey;
import io.gravitee.rest.api.model.parameters.Key;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlanSettings {

    private PlanSecurity security;

    public PlanSettings() {
        security = new PlanSecurity();
    }

    public PlanSecurity getSecurity() {
        return security;
    }

    public void setSecurity(PlanSecurity security) {
        this.security = security;
    }

    public static class PlanSecurity {

        @ParameterKey(Key.PLAN_SECURITY_APIKEY_ENABLED)
        private Enabled apikey;

        @ParameterKey(Key.PLAN_SECURITY_APIKEY_CUSTOM_ALLOWED)
        private Enabled customApiKey;

        @ParameterKey(Key.PLAN_SECURITY_APIKEY_SHARED_ALLOWED)
        private Enabled sharedApiKey;

        @ParameterKey(Key.PLAN_SECURITY_OAUTH2_ENABLED)
        private Enabled oauth2;

        @ParameterKey(Key.PLAN_SECURITY_KEYLESS_ENABLED)
        private Enabled keyless;

        @ParameterKey(Key.PLAN_SECURITY_JWT_ENABLED)
        private Enabled jwt;

        @ParameterKey(Key.PLAN_SECURITY_PUSH_ENABLED)
        private Enabled push;

        @ParameterKey(Key.PLAN_SECURITY_MTLS_ENABLED)
        private Enabled mtls;

        public Enabled getApikey() {
            return apikey;
        }

        public void setApikey(Enabled apikey) {
            this.apikey = apikey;
        }

        public Enabled getCustomApiKey() {
            return customApiKey;
        }

        public void setCustomApiKey(Enabled customApiKey) {
            this.customApiKey = customApiKey;
        }

        public Enabled getOauth2() {
            return oauth2;
        }

        public void setOauth2(Enabled oauth2) {
            this.oauth2 = oauth2;
        }

        public Enabled getKeyless() {
            return keyless;
        }

        public void setKeyless(Enabled keyless) {
            this.keyless = keyless;
        }

        public Enabled getJwt() {
            return jwt;
        }

        public void setJwt(Enabled jwt) {
            this.jwt = jwt;
        }

        public Enabled getMtls() {
            return mtls;
        }

        public void setMtls(Enabled mtls) {
            this.mtls = mtls;
        }

        public Enabled getSharedApiKey() {
            return sharedApiKey;
        }

        public void setSharedApiKey(Enabled sharedApiKey) {
            this.sharedApiKey = sharedApiKey;
        }

        public Enabled getPush() {
            return push;
        }

        public void setPush(Enabled push) {
            this.push = push;
        }
    }
}
