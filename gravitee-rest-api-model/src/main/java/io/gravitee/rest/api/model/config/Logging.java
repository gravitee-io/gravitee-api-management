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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public class Audit {
        @ParameterKey(Key.LOGGING_AUDIT_ENABLED)
        private Boolean enabled;
        private Audit.AuditTrail trail = new Audit.AuditTrail();

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public Audit.AuditTrail getTrail() {
            return trail;
        }

        public void setTrail(Audit.AuditTrail trail) {
            this.trail = trail;
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public class AuditTrail {
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public class User {
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
