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
package io.gravitee.apim.core.api_key.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ApiKeyEntity {

    @EqualsAndHashCode.Include
    private String id;

    private String key;

    private String application;

    private List<String> subscriptions = new ArrayList<>();

    private Date expireAt;

    private Date createdAt;

    private Date updatedAt;

    private boolean revoked;

    private Date revokedAt;

    private boolean paused;

    /**
     * Number of days before the expiration of this API Key when the last pre-expiration notification was sent
     */
    private Integer daysToExpirationOnLastNotification;

    public ApiKeyEntity revoke() {
        var now = new Date();
        return this.toBuilder().revoked(true).updatedAt(now).revokedAt(now).build();
    }

    public boolean isExpired() {
        return this.expireAt != null && new Date().after(this.getExpireAt());
    }

    @JsonIgnore
    public boolean canBeRevoked() {
        return !this.isRevoked() && !this.isExpired();
    }
}
