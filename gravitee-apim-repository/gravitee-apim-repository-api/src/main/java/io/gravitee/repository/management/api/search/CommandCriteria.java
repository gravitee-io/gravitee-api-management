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

import java.util.Arrays;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class CommandCriteria {

    private final String to;
    private final String[] tags;
    private final boolean notExpired;
    private final String notFrom;
    private final String notAckBy;
    private final String environmentId;
    private final String organizationId;

    private CommandCriteria(Builder builder) {
        this.to = builder.to;
        this.tags = builder.tags;
        this.notExpired = builder.notDeleted;
        this.notFrom = builder.notFrom;
        this.notAckBy = builder.notAckBy;
        this.environmentId = builder.environmentId;
        this.organizationId = builder.organizationId;
    }

    public String getTo() {
        return to;
    }

    public String[] getTags() {
        return tags;
    }

    public boolean isNotExpired() {
        return notExpired;
    }

    public String getNotFrom() {
        return notFrom;
    }

    public String getNotAckBy() {
        return notAckBy;
    }

    public String getEnvironmentId() {
        return environmentId;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    @Override
    public String toString() {
        return (
            "criteria {" +
            "to: " +
            to +
            "tags: " +
            Arrays.toString(tags) +
            "notExpired: " +
            notExpired +
            "notFrom: " +
            notFrom +
            "notAckBy:" +
            notAckBy +
            "environmentId:" +
            environmentId +
            "}"
        );
    }

    public static class Builder {

        private String to;
        private String[] tags;
        private boolean notDeleted;
        private String notFrom;
        private String notAckBy;
        private String environmentId;
        private String organizationId;

        public CommandCriteria build() {
            return new CommandCriteria(this);
        }

        public Builder to(String to) {
            this.to = to;
            return this;
        }

        public Builder tags(String... tags) {
            this.tags = tags;
            return this;
        }

        public Builder notDeleted() {
            this.notDeleted = true;
            return this;
        }

        public Builder notFrom(String from) {
            this.notFrom = from;
            return this;
        }

        public Builder notAckBy(String by) {
            this.notAckBy = by;
            return this;
        }

        public Builder environmentId(String environmentId) {
            this.environmentId = environmentId;
            return this;
        }

        public Builder organizationId(String organizationId) {
            this.organizationId = organizationId;
            return this;
        }
    }
}
