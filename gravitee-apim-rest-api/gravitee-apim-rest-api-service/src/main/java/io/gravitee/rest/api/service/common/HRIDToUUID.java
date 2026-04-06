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
 * Fluent DSL for generating deterministic UUIDs from HRIDs.
 * <p>
 * Top-level resources (API, Application, SharedPolicyGroup) produce both {@code id()} and {@code crossId()}.
 * Sub-resources (Plan, Page, Subscription) are scoped to a parent API and only produce {@code id()}.
 * <p>
 * Examples:
 * <pre>
 * HRIDToUUID.api().context(audit).hrid("my-api").id()
 * HRIDToUUID.plan().context(audit).api("my-api").plan("my-plan").id()
 * </pre>
 *
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public final class HRIDToUUID {

    private HRIDToUUID() {}

    public static TopLevelBuilder api() {
        return new TopLevelBuilder();
    }

    public static TopLevelBuilder application() {
        return new TopLevelBuilder();
    }

    public static TopLevelBuilder sharedPolicyGroup() {
        return new TopLevelBuilder();
    }

    public static SubResourceBuilder plan() {
        return new SubResourceBuilder();
    }

    public static SubResourceBuilder page() {
        return new SubResourceBuilder();
    }

    public static SubResourceBuilder subscription() {
        return new SubResourceBuilder();
    }

    public static class TopLevelBuilder {

        public TopLevelWithContext context(AuditInfo audit) {
            return new TopLevelWithContext(audit.organizationId(), audit.environmentId());
        }

        public TopLevelWithContext context(ExecutionContext ctx) {
            return new TopLevelWithContext(ctx.getOrganizationId(), ctx.getEnvironmentId());
        }
    }

    public record TopLevelWithContext(String organizationId, String environmentId) {
        public TopLevelResult hrid(String hrid) {
            return new TopLevelResult(organizationId, environmentId, hrid);
        }
    }

    public record TopLevelResult(String organizationId, String environmentId, String hrid) {
        public String crossId() {
            return UuidString.generateFrom(organizationId, hrid);
        }

        public String id() {
            return UuidString.generateFrom(crossId(), environmentId);
        }
    }

    public static class SubResourceBuilder {

        public SubResourceWithContext context(AuditInfo audit) {
            return new SubResourceWithContext(audit.organizationId(), audit.environmentId());
        }

        public SubResourceWithContext context(ExecutionContext ctx) {
            return new SubResourceWithContext(ctx.getOrganizationId(), ctx.getEnvironmentId());
        }
    }

    public record SubResourceWithContext(String organizationId, String environmentId) {
        public SubResourceWithApi api(String apiHrid) {
            return new SubResourceWithApi(UuidString.generateFrom(organizationId, apiHrid), environmentId);
        }
    }

    public record SubResourceWithApi(String apiCrossId, String environmentId) {
        public SubResourceResult plan(String planHrid) {
            return new SubResourceResult(apiCrossId, environmentId, planHrid);
        }

        public SubResourceResult page(String pageHrid) {
            return new SubResourceResult(apiCrossId, environmentId, pageHrid);
        }

        public SubResourceResult subscription(String subscriptionHrid) {
            return new SubResourceResult(apiCrossId, environmentId, subscriptionHrid);
        }
    }

    public record SubResourceResult(String apiCrossId, String environmentId, String extraHrid) {
        public String id() {
            return UuidString.generateFrom(apiCrossId, environmentId, extraHrid);
        }
    }
}
