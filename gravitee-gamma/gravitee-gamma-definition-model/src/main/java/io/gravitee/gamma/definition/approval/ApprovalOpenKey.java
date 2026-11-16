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
package io.gravitee.gamma.definition.approval;

import java.util.Objects;

/**
 * Identifies the same held call while it waits for a decision, the same way on every gateway and in AIM: a client
 * calling again, on any node, finds the request already open instead of opening another.
 */
public final class ApprovalOpenKey {

    private ApprovalOpenKey() {}

    /**
     * The lowercase hex SHA-256 of the parts, each written as its length, a colon and its text, so that no text can be
     * moved from one part to the next; an absent subject is written {@code -}, which no length-prefixed part can be.
     *
     * @param subject the end user, or {@code null} when the call names none; every other part is required
     * @param argumentsDigest {@link ArgumentsDigest#of} of the call
     */
    public static String of(
        String environmentId,
        String apiId,
        String ruleId,
        String applicationId,
        String subject,
        String argumentsDigest
    ) {
        Objects.requireNonNull(environmentId, "environmentId");
        Objects.requireNonNull(apiId, "apiId");
        Objects.requireNonNull(ruleId, "ruleId");
        Objects.requireNonNull(applicationId, "applicationId");
        Objects.requireNonNull(argumentsDigest, "argumentsDigest");
        StringBuilder parts = new StringBuilder();
        for (String part : new String[] { environmentId, apiId, ruleId, applicationId, subject, argumentsDigest }) {
            parts.append(part == null ? "-" : part.length() + ":" + part).append('\n');
        }
        return Sha256.hex(parts.toString());
    }
}
