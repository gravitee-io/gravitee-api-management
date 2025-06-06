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
package io.gravitee.rest.api.service.common;

import io.gravitee.apim.core.audit.model.AuditInfo;

/**
 * Helps to build consistent and idempotent IDs
 */
public final class IdBuilder {

    private String hrid;
    private String environmentId;
    private String organizationId;
    private String extraId;

    private IdBuilder() {
        // no op
    }

    /**
     * Create a new builder
     * @param auditInfo rest audi info object
     * @param hrid the human-readable ID to mingle with audit info
     * @return a  builder
     */
    public static IdBuilder builder(AuditInfo auditInfo, String hrid) {
        var builder = new IdBuilder();
        builder.hrid = hrid;
        builder.organizationId = auditInfo.organizationId();
        builder.environmentId = auditInfo.environmentId();
        return builder;
    }

    /**
     * Create a new builder
     * @param executionContext gravitee execution context
     * @param hrid the human-readable ID to mingle with audit info
     * @return a  builder
     */
    public static IdBuilder builder(ExecutionContext executionContext, String hrid) {
        var builder = new IdBuilder();
        builder.hrid = hrid;
        builder.organizationId = executionContext.getOrganizationId();
        builder.environmentId = executionContext.getEnvironmentId();
        return builder;
    }

    /**
     * Adds an extra token to form IDs
     * @param extraId an ID to add
     * @return the build instance
     */
    public IdBuilder withExtraId(String extraId) {
        this.extraId = extraId;
        return this;
    }

    /**
     * Build a unique across all org ID that is the across all environment of a same org
     * @return a UUID built for data given by the builder
     */
    public String buildCrossId() {
        return UuidString.generateFrom(organizationId, hrid);
    }

    /**
     * Build a unique across all orgs and env
     * @return a UUID built for data given by the builder
     */
    public String buildId() {
        if (extraId == null) {
            return UuidString.generateFrom(buildCrossId(), environmentId);
        }
        return UuidString.generateFrom(buildCrossId(), environmentId, extraId);
    }
}
