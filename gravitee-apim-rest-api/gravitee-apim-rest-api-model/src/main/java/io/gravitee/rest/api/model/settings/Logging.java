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
import io.gravitee.rest.api.model.annotations.ParameterKey;
import io.gravitee.rest.api.model.parameters.Key;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Logging {

    @ParameterKey(Key.LOGGING_DEFAULT_MAX_DURATION)
    private Long maxDurationMillis;

    private Audit audit = new Audit();
    private User user = new User();

    public Long getMaxDurationMillis() {
        return maxDurationMillis;
    }

    public void setMaxDurationMillis(Long maxDurationMillis) {
        this.maxDurationMillis = maxDurationMillis;
    }

    public Audit getAudit() {
        return audit;
    }

    public void setAudit(Audit audit) {
        this.audit = audit;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public static class Audit {

        @ParameterKey(Key.LOGGING_AUDIT_ENABLED)
        private Boolean enabled;

        private AuditTrail trail = new AuditTrail();

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public AuditTrail getTrail() {
            return trail;
        }

        public void setTrail(AuditTrail trail) {
            this.trail = trail;
        }

        public static class AuditTrail {

            @ParameterKey(Key.LOGGING_AUDIT_TRAIL_ENABLED)
            private Boolean enabled;

            public Boolean getEnabled() {
                return enabled;
            }

            public void setEnabled(Boolean enabled) {
                this.enabled = enabled;
            }
        }
    }

    public static class User {

        @ParameterKey(Key.LOGGING_USER_DISPLAYED)
        private Boolean displayed;

        public Boolean getDisplayed() {
            return displayed;
        }

        public void setDisplayed(Boolean displayed) {
            this.displayed = displayed;
        }
    }
}
