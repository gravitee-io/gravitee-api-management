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
package io.gravitee.gamma.module.platform.rest.resource.dto.secret;

import io.gravitee.gamma.module.platform.core.secret.model.Secret;
import java.util.Date;
import java.util.List;

public final class SecretDtos {

    private SecretDtos() {}

    public record CreateSecretRequest(String key, String value) {
        @Override
        public String toString() {
            return "CreateSecretRequest[key=" + key + ", value=***]";
        }
    }

    public record RotateSecretRequest(String value) {
        @Override
        public String toString() {
            return "RotateSecretRequest[value=***]";
        }
    }

    public record SecretResponse(String id, String key, String path, Date createdAt, Date updatedAt) {
        public static SecretResponse from(Secret secret) {
            return new SecretResponse(
                secret.id(),
                secret.key(),
                secret.path(),
                Date.from(secret.createdAt()),
                Date.from(secret.updatedAt())
            );
        }
    }

    public record SecretsPageResponse(List<SecretResponse> data, Pagination pagination) {}

    public record Pagination(int page, int perPage, long pageItemsCount, long pageCount, long totalCount) {}
}
