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
package io.gravitee.repository.management.api.search;

import io.gravitee.repository.management.model.UserStatus;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class UserCriteria {

    private UserStatus[] statuses;
    private boolean noStatus;
    private String environment;

    UserCriteria(UserCriteria.Builder builder) {
        this.statuses = builder.statuses;
        this.noStatus = builder.noStatus;
        this.environment = builder.environment;
    }

    public UserStatus[] getStatuses() {
        return statuses;
    }

    public boolean hasNoStatus() {
        return noStatus;
    }

    
    public String getEnvironment() {
        return environment;
    }


    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public static class Builder {
        private UserStatus[] statuses;
        private boolean noStatus;
        private String environment;

        public Builder statuses(UserStatus... statuses) {
            this.statuses = statuses;
            return this;
        }

        public Builder noStatus() {
            noStatus = true;
            return this;
        }

        public Builder environment(String environment) {
            this.environment = environment;
            return this;
        }
        
        public UserCriteria build() {
            return new UserCriteria(this);
        }
    }
}
