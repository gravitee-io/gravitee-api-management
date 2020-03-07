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
package io.gravitee.management.rest.resource;

import io.gravitee.common.http.MediaType;
import io.gravitee.management.model.PolicyEntity;
import io.gravitee.management.model.PolicyListItem;
import io.gravitee.management.model.permissions.RolePermission;
import io.gravitee.management.model.permissions.RolePermissionAction;
import io.gravitee.management.rest.security.Permission;
import io.gravitee.management.rest.security.Permissions;
import io.gravitee.management.service.PolicyService;
import io.gravitee.management.service.impl.swagger.policy.PolicyOperationVisitorManager;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Defines the REST resources to manage Policy.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Path("/policies")
@Api(tags = {"Plugin", "Policy"})
public class PoliciesResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private PolicyService policyService;

    @Inject
    private PolicyOperationVisitorManager policyOperationVisitorManager;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "List policies")
    @Permissions({
            @Permission(value = RolePermission.MANAGEMENT_API, acls = RolePermissionAction.READ)
    })
    public Collection<PolicyListItem> listPolicies(@QueryParam("expand") List<String> expand) {
        Stream<PolicyListItem> stream = policyService.findAll().stream().map(this::convert);

        if(expand!=null && !expand.isEmpty()) {
            for (String s : expand) {
                switch (s) {
                    case "schema":
                        stream = stream.map(policyListItem -> {
                            policyListItem.setSchema(policyService.getSchema(policyListItem.getId()));
                            return policyListItem;
                        });
                        break;
                    default: break;
                }
            }
        }

        return stream
                .sorted(Comparator.comparing(PolicyListItem::getName))
                .collect(Collectors.toList());
    }

    @Path("{policy}")
    public PolicyResource getPolicyResource() {
        return resourceContext.getResource(PolicyResource.class);
    }

    @GET
    @Path("swagger")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "List Swagger policies")
    @Permissions({
            @Permission(value = RolePermission.MANAGEMENT_API, acls = RolePermissionAction.READ)
    })
    public List<PolicyListItem> getSwaggerPolicy() {
        return policyOperationVisitorManager.getPolicyVisitors()
                .stream()
                .map(operationVisitor -> {
                    PolicyListItem item = new PolicyListItem();
                    item.setId(operationVisitor.getId());
                    item.setName(operationVisitor.getName());
                    return item;
                })
                .collect(Collectors.toList());
    }

    private PolicyListItem convert(PolicyEntity policy) {
        PolicyListItem item = new PolicyListItem();

        item.setId(policy.getId());
        item.setName(policy.getName());
        item.setDescription(policy.getDescription());
        item.setVersion(policy.getVersion());
        item.setType(policy.getType());

        return item;
    }
}
