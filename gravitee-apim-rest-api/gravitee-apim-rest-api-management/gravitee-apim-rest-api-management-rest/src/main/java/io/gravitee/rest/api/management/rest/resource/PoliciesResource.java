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
package io.gravitee.rest.api.management.rest.resource;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.PolicyDevelopmentEntity;
import io.gravitee.rest.api.model.PolicyEntity;
import io.gravitee.rest.api.model.PolicyListItem;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.PolicyService;
import io.gravitee.rest.api.service.impl.swagger.policy.PolicyOperationVisitorManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;

/**
 * Defines the REST resources to manage Policy.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Plugins")
public class PoliciesResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private PolicyService policyService;

    @Inject
    private PolicyOperationVisitorManager policyOperationVisitorManager;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List policies", description = "User must have the MANAGEMENT_API[READ] permission to use this service")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API, acls = RolePermissionAction.READ) })
    public Collection<PolicyListItem> getPolicies(
        @QueryParam("expand") List<String> expand,
        @QueryParam("withResource") Boolean withResource
    ) {
        Stream<PolicyListItem> stream = policyService.findAll(withResource).stream().map(this::convert);

        if (expand != null && !expand.isEmpty()) {
            for (String s : expand) {
                switch (s) {
                    case "schema":
                        stream = stream.peek(policyListItem -> policyListItem.setSchema(policyService.getSchema(policyListItem.getId())));
                    case "icon":
                        stream = stream.peek(policyListItem -> policyListItem.setIcon(policyService.getIcon(policyListItem.getId())));
                    default:
                        break;
                }
            }
        }

        return stream.sorted(Comparator.comparing(PolicyListItem::getName)).collect(Collectors.toList());
    }

    @Path("{policy}")
    public PolicyResource getPolicyResource() {
        return resourceContext.getResource(PolicyResource.class);
    }

    @GET
    @Path("swagger")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "List policies which are handling Swagger / OAI definition",
        description = "These policies are used when importing an OAI to create an API"
    )
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API, acls = RolePermissionAction.READ) })
    public List<PolicyListItem> getSwaggerPolicy() {
        return policyOperationVisitorManager
            .getPolicyVisitors()
            .stream()
            .filter(operationVisitor -> operationVisitor.display())
            .map(
                operationVisitor -> {
                    PolicyListItem item = new PolicyListItem();
                    item.setId(operationVisitor.getId());
                    item.setName(operationVisitor.getName());
                    return item;
                }
            )
            .sorted(Comparator.comparing(PolicyListItem::getName))
            .collect(Collectors.toList());
    }

    private PolicyListItem convert(PolicyEntity policy) {
        PolicyListItem item = new PolicyListItem();
        item.setId(policy.getId());
        item.setName(policy.getName());
        item.setDescription(policy.getDescription());
        item.setVersion(policy.getVersion());
        item.setType(policy.getType());
        item.setCategory(policy.getCategory());
        PolicyDevelopmentEntity development = policy.getDevelopment();
        if (development != null) {
            item.setOnRequest(development.getOnRequestMethod() != null);
            item.setOnResponse(development.getOnResponseMethod() != null);
        } else {
            item.setOnRequest(true);
            item.setOnResponse(false);
        }
        return item;
    }
}
