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
package io.gravitee.apim.rest.api.automation.resource;

import static io.gravitee.rest.api.model.permissions.RolePermissionAction.CREATE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.DELETE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.READ;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.UPDATE;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.plugin.gamma.api.automation.AutomationContext;
import io.gravitee.apim.plugin.gamma.api.automation.AutomationIssue;
import io.gravitee.apim.plugin.gamma.api.automation.GammaAutomationPort;
import io.gravitee.apim.plugin.gamma.api.automation.ResourceKind;
import io.gravitee.apim.rest.api.automation.exception.AutomationResourceKindNotFoundException;
import io.gravitee.apim.rest.api.automation.exception.GammaModuleUnavailableException;
import io.gravitee.apim.rest.api.automation.exception.HRIDNotFoundException;
import io.gravitee.apim.rest.api.automation.spring.GammaAutomationPorts;
import io.gravitee.common.http.MediaType;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import io.gravitee.rest.api.service.exceptions.ForbiddenFeatureException;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Generic automation surface of a Gamma module, mounted at
 * {@code /organizations/{orgId}/environments/{envId}/{module}}.
 *
 * <p>The module owns the typed contract; this resource owns every automation convention and enforces it
 * before a call reaches the module: license, permission, HRID validation, deterministic id derivation,
 * dry-run semantics, the state envelope and the error format.
 *
 * <p>All three verbs share one path template on purpose: JAX-RS selects the matching template before it
 * looks at the HTTP method, so a collection template and an item template that both match a two-segment
 * path would turn {@code PUT} on the collection into a 405. The resource splits the path itself instead —
 * an HRID is a single segment, so the item path is always {@code <kind path>/<hrid>}.
 */
public class GammaModuleAutomationResource extends AbstractResource {

    /** Same grammar as the {@code Hrid} schema of the Automation API document. */
    private static final Pattern HRID_PATTERN = Pattern.compile("^[a-zA-Z0-9][a-zA-Z0-9_-]+[a-zA-Z0-9]$");
    private static final int HRID_MAX_LENGTH = 256;

    private static final String HRID_FIELD = "hrid";
    private static final String ID_FIELD = "id";
    private static final String ENVIRONMENT_ID_FIELD = "environmentId";
    private static final String ORGANIZATION_ID_FIELD = "organizationId";
    private static final String ERRORS_FIELD = "errors";
    private static final String SEVERE_FIELD = "severe";
    private static final String WARNING_FIELD = "warning";

    @Inject
    private GammaAutomationPorts gammaAutomationPorts;

    @Inject
    private LicenseManager licenseManager;

    // /automation/.../aim
    @PUT
    @Path("/{path: .+}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createOrUpdate(
        @PathParam("module") String module,
        @PathParam("path") String kindPath,
        @QueryParam("dryRun") boolean dryRun,
        @NotNull ObjectNode spec
    ) {
        var port = port(module);
        var kind = kind(port, module, kindPath);
        checkLicense(port);
        checkPermission(kind, CREATE, UPDATE);

        var hrid = requireHrid(spec);
        var audit = getAuditInfo();
        var id = idOf(audit, module, kind.path(), hrid);
        var context = context(audit, module);

        var result = dryRun ? port.validate(context, kind, id, spec) : port.upsert(context, kind, id, spec);
        var state = stamp(result.view(), id, hrid, audit, result.issues());

        // A dry run is a preview: severe findings are its payload, so it always answers 200.
        // A real apply that produced severe findings persisted nothing — it must not report success.
        var applyFailed = !dryRun && result.hasSevere();
        return Response.status(applyFailed ? Response.Status.BAD_REQUEST : Response.Status.OK).entity(state).build();
    }

    @GET
    @Path("/{path: .+}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@PathParam("module") String module, @PathParam("path") String path) {
        var port = port(module);
        var item = ItemPath.parse(path);
        var kind = kind(port, module, item.kindPath());
        checkLicense(port);
        checkPermission(kind, READ);

        var audit = getAuditInfo();
        var id = idOf(audit, module, kind.path(), item.hrid());

        return port
            .findById(context(audit, module), kind, id)
            .map(view -> Response.ok(stamp(view, id, item.hrid(), audit, List.of())).build())
            .orElseThrow(() -> new HRIDNotFoundException(item.hrid()));
    }

    @DELETE
    @Path("/{path: .+}")
    public Response delete(@PathParam("module") String module, @PathParam("path") String path) {
        var port = port(module);
        var item = ItemPath.parse(path);
        var kind = kind(port, module, item.kindPath());
        checkLicense(port);
        checkPermission(kind, DELETE);

        var audit = getAuditInfo();
        var id = idOf(audit, module, kind.path(), item.hrid());

        if (!port.deleteById(context(audit, module), kind, id)) {
            throw new HRIDNotFoundException(item.hrid());
        }
        return Response.noContent().build();
    }

    private GammaAutomationPort port(String module) {
        return gammaAutomationPorts.module(module).orElseThrow(() -> new GammaModuleUnavailableException(module));
    }

    private static ResourceKind kind(GammaAutomationPort port, String module, String kindPath) {
        return port.kind(kindPath).orElseThrow(() -> new AutomationResourceKindNotFoundException(module, kindPath));
    }

    private void checkLicense(GammaAutomationPort port) {
        port
            .licenseFeature()
            .ifPresent(feature -> {
                if (!licenseManager.getPlatformLicense().isFeatureEnabled(feature)) {
                    throw new ForbiddenFeatureException(feature);
                }
            });
    }

    private void checkPermission(ResourceKind kind, RolePermissionAction... acls) {
        // Automation resources are addressed per environment; any other scope is a module authoring error,
        // and answering it with a permission grant would fail open.
        if (kind.permission().getScope() != RoleScope.ENVIRONMENT) {
            throw new IllegalStateException(
                "Automation resource kind [" + kind.path() + "] declares a non-environment permission " + kind.permission()
            );
        }
        var executionContext = GraviteeContext.getExecutionContext();
        if (!hasPermission(executionContext, kind.permission(), executionContext.getEnvironmentId(), acls)) {
            throw new ForbiddenAccessException();
        }
    }

    private static String requireHrid(ObjectNode spec) {
        JsonNode hrid = spec.get(HRID_FIELD);
        if (hrid == null || !hrid.isTextual()) {
            throw new BadRequestException("The spec must carry a textual [" + HRID_FIELD + "]");
        }
        var value = hrid.asText();
        if (value.length() > HRID_MAX_LENGTH || !HRID_PATTERN.matcher(value).matches()) {
            throw new BadRequestException(
                "[" + HRID_FIELD + "] must match " + HRID_PATTERN.pattern() + " and be at most " + HRID_MAX_LENGTH + " characters"
            );
        }
        return value;
    }

    private static String idOf(AuditInfo audit, String module, String kindPath, String hrid) {
        return HRIDToUUID.gamma().context(audit).module(module).kind(kindPath).hrid(hrid).id();
    }

    private static AutomationContext context(AuditInfo audit, String module) {
        return new AutomationContext(audit.organizationId(), audit.environmentId(), (kindPath, hrid) ->
            idOf(audit, module, kindPath, hrid)
        );
    }

    /**
     * The module's view plus the envelope every automation state carries; the module never sets these. The hrid is
     * an automation identifier: it comes from the request (body on apply, path on read), so modules need not store it.
     */
    private static ObjectNode stamp(ObjectNode view, String id, String hrid, AuditInfo audit, List<AutomationIssue> issues) {
        var state = view.deepCopy();
        state.put(HRID_FIELD, hrid);
        state.put(ID_FIELD, id);
        state.put(ENVIRONMENT_ID_FIELD, audit.environmentId());
        state.put(ORGANIZATION_ID_FIELD, audit.organizationId());
        state.remove(ERRORS_FIELD);
        if (!issues.isEmpty()) {
            var errors = state.putObject(ERRORS_FIELD);
            var severe = errors.putArray(SEVERE_FIELD);
            var warning = errors.putArray(WARNING_FIELD);
            issues.forEach(issue -> (issue.isSevere() ? severe : warning).add(issue.message()));
        }
        return state;
    }

    private record ItemPath(String kindPath, String hrid) {
        static ItemPath parse(String path) {
            int separator = path.lastIndexOf('/');
            if (separator <= 0 || separator == path.length() - 1) {
                throw new BadRequestException("Expected <kind path>/<hrid>, got [" + path + "]");
            }
            return new ItemPath(path.substring(0, separator), path.substring(separator + 1));
        }
    }
}
