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

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class CommandCriteria {
    private String to;
    private String[] tags;
    private boolean notExpired;
    private String notFrom;
    private String notAckBy;

    private CommandCriteria(Builder builder) {
        this.to = builder.to;
        this.tags = builder.tags;
        this.notExpired = builder.notDeleted;
        this.notFrom = builder.notFrom;
        this.notAckBy = builder.notAckBy;
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

    @Override
    public String toString() {
        return "criteria {" +
                "to: " + to +
                "tags: " + tags +
                "notExpired: " + notExpired +
                "notFrom: " + notFrom +
                "notAckBy:" + notAckBy +
                "}";
    }

    public static class Builder {
        private String to;
        private String[] tags;
        private boolean notDeleted;
        private String notFrom;
        private String notAckBy;

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
    }
}
