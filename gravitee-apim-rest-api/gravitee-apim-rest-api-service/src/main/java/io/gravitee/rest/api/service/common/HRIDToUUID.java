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
 * Top-level resources (API, Application, SharedPolicyGroup, Group, Portal) produce both {@code id()} and
 * {@code crossId()}.
 * Sub-resources (Plan, Page, Subscription) are scoped to a parent API and only produce {@code id()}.
 * Portal sub-resources (Portal Listing, Portal Documentation) are scoped to a parent Portal and only produce
 * {@code id()}.
 * API sub-resources (API Documentation) are scoped to a parent API (literal {@code "api"} discriminant in the
 * cross-id prevents collisions with top-level api ids and other api sub-resources sharing the same hrid grammar)
 * and only produce {@code id()}.
 * <p>
 * Examples:
 * <pre>
 * HRIDToUUID.api().context(audit).hrid("my-api").id()
 * HRIDToUUID.plan().context(audit).api("my-api").plan("my-plan").id()
 * HRIDToUUID.portalListing().context(audit).portal("my-portal").hrid("my-listing").id()
 * HRIDToUUID.portalDocumentation().context(audit).portal("my-portal").hrid("my-doc").id()
 * HRIDToUUID.apiDocumentation().context(audit).api("my-api").hrid("my-doc").id()
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

    public static TopLevelBuilder dictionary() {
        return new TopLevelBuilder();
    }

    public static TopLevelBuilder application() {
        return new TopLevelBuilder();
    }

    public static TopLevelBuilder sharedPolicyGroup() {
        return new TopLevelBuilder();
    }

    public static TopLevelBuilder group() {
        return new TopLevelBuilder();
    }

    public static TopLevelBuilder portal() {
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

    public static NavigationBuilder navigation() {
        return new NavigationBuilder();
    }

    public static PortalSubResourceBuilder portalListing() {
        return new PortalSubResourceBuilder();
    }

    public static PortalSubResourceBuilder portalDocumentation() {
        return new PortalSubResourceBuilder();
    }

    public static ApiSubResourceBuilder apiDocumentation() {
        return new ApiSubResourceBuilder();
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

        public String crossId() {
            return UuidString.generateFrom(apiCrossId, extraHrid);
        }
    }

    public static class NavigationBuilder {

        public NavigationWithContext context(AuditInfo audit) {
            return new NavigationWithContext(audit.organizationId(), audit.environmentId());
        }
    }

    public record NavigationWithContext(String organizationId, String environmentId) {
        public NavigationInPortal portal(String portalId) {
            return new NavigationInPortal(organizationId, environmentId, portalId);
        }
    }

    public record NavigationInPortal(String organizationId, String environmentId, String portalId) {
        public NavigationItemResult folder(String path) {
            return new NavigationItemResult(organizationId, environmentId, portalId, "folder", path);
        }

        public NavigationItemResult documentation(String contentId) {
            return new NavigationItemResult(organizationId, environmentId, portalId, "documentation", contentId);
        }
    }

    public record NavigationItemResult(String organizationId, String environmentId, String portalId, String kind, String identifier) {
        public String id() {
            return UuidString.generateFrom(organizationId, environmentId, portalId, kind, identifier);
        }
    }

    public static class PortalSubResourceBuilder {

        public PortalSubResourceWithContext context(AuditInfo audit) {
            return new PortalSubResourceWithContext(audit.organizationId(), audit.environmentId());
        }

        public PortalSubResourceWithContext context(ExecutionContext ctx) {
            return new PortalSubResourceWithContext(ctx.getOrganizationId(), ctx.getEnvironmentId());
        }
    }

    public record PortalSubResourceWithContext(String organizationId, String environmentId) {
        public PortalSubResourceWithPortal portal(String portalHrid) {
            return new PortalSubResourceWithPortal(UuidString.generateFrom(organizationId, "portal", portalHrid), environmentId);
        }
    }

    public record PortalSubResourceWithPortal(String portalCrossId, String environmentId) {
        public PortalSubResourceResult hrid(String hrid) {
            return new PortalSubResourceResult(portalCrossId, environmentId, hrid);
        }
    }

    public record PortalSubResourceResult(String portalCrossId, String environmentId, String hrid) {
        public String id() {
            return UuidString.generateFrom(portalCrossId, environmentId, hrid);
        }
    }

    public static class ApiSubResourceBuilder {

        public ApiSubResourceWithContext context(AuditInfo audit) {
            return new ApiSubResourceWithContext(audit.organizationId(), audit.environmentId());
        }

        public ApiSubResourceWithContext context(ExecutionContext ctx) {
            return new ApiSubResourceWithContext(ctx.getOrganizationId(), ctx.getEnvironmentId());
        }
    }

    public record ApiSubResourceWithContext(String organizationId, String environmentId) {
        public ApiSubResourceWithApi api(String apiHrid) {
            return new ApiSubResourceWithApi(UuidString.generateFrom(organizationId, "api", apiHrid), environmentId);
        }
    }

    public record ApiSubResourceWithApi(String apiCrossId, String environmentId) {
        public ApiSubResourceResult hrid(String hrid) {
            return new ApiSubResourceResult(apiCrossId, environmentId, hrid);
        }
    }

    public record ApiSubResourceResult(String apiCrossId, String environmentId, String hrid) {
        public String id() {
            return UuidString.generateFrom(apiCrossId, environmentId, hrid);
        }
    }
}
