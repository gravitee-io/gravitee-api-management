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

import io.gravitee.repository.management.model.UserReferenceType;
import io.gravitee.repository.management.model.UserStatus;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class UserCriteria {

    private UserStatus[] statuses;
    private boolean noStatus;
    private String referenceId;
    private UserReferenceType referenceType;

    UserCriteria(UserCriteria.Builder builder) {
        this.statuses = builder.statuses;
        this.noStatus = builder.noStatus;
        this.referenceId = builder.referenceId;
        this.referenceType = builder.referenceType;
    }

    public UserStatus[] getStatuses() {
        return statuses;
    }

    public boolean hasNoStatus() {
        return noStatus;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public UserReferenceType getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(UserReferenceType referenceType) {
        this.referenceType = referenceType;
    }




    public static class Builder {
        private UserStatus[] statuses;
        private boolean noStatus;
        private String referenceId;
        private UserReferenceType referenceType;

        public Builder statuses(UserStatus... statuses) {
            this.statuses = statuses;
            return this;
        }

        public Builder noStatus() {
            noStatus = true;
            return this;
        }

        public Builder referenceId(String referenceId) {
            this.referenceId = referenceId;
            return this;
        }

        public Builder referenceType(UserReferenceType referenceType) {
            this.referenceType = referenceType;
            return this;
        }

        public UserCriteria build() {
            return new UserCriteria(this);
        }
    }
}
