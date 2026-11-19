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
package io.gravitee.rest.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class InvalidateRoleCacheCommandEntity {

    private String referenceType;
    private String referenceId;
    private String memberType;
    private String memberId;

    /**
     * When true, invalidate every cached permission entry for {@code referenceType}/{@code referenceId}
     * (all users), instead of a single member. Used when an application's groups change and group-derived
     * application permissions must be recomputed immediately.
     * Omitted from JSON when false so existing single-member commands stay unchanged.
     */
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private boolean invalidateAllMembers;

    @JsonProperty("created_at")
    private Date createdAt;

    @JsonProperty("updated_at")
    private Date updatedAt;
}
