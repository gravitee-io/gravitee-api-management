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
package io.gravitee.repository.management.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiCategory {

    private Id id;
    private String categoryKey;
    private int order;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Id {

        private String categoryId;
        private String apiId;
    }

    @JsonIgnore
    public String getApiId() {
        return this.id.getApiId();
    }

    public void setApiId(String apiId) {
        if (Objects.isNull(this.id)) {
            this.id = new Id();
        }
        this.id.setApiId(apiId);
    }

    @JsonIgnore
    public String getCategoryId() {
        return this.id.getCategoryId();
    }

    public void setCategoryId(String categoryId) {
        if (Objects.isNull(this.id)) {
            this.id = new Id();
        }
        this.id.setApiId(categoryId);
    }
}
