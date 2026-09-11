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

import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.subscription_form.exception.SubscriptionFormNotFoundException;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormId;
import io.gravitee.apim.core.subscription_form.use_case.DeleteSubscriptionFormUseCase;
import io.gravitee.apim.core.subscription_form.use_case.GetSubscriptionFormUseCase;
import io.gravitee.apim.rest.api.automation.exception.HRIDNotFoundException;
import io.gravitee.apim.rest.api.automation.mapper.SubscriptionFormMapper;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

public class SubscriptionFormResource extends AbstractResource {

    @Inject
    private GetSubscriptionFormUseCase getSubscriptionFormUseCase;

    @Inject
    private DeleteSubscriptionFormUseCase deleteSubscriptionFormUseCase;

    @Inject
    private ApiCrudService apiCrudService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_SUBSCRIPTION_FORM, acls = RolePermissionAction.READ) })
    public Response getSubscriptionFormByHrid(@PathParam("hrid") String hrid) {
        var auditInfo = getAuditInfo();
        var formId = HRIDToUUID.subscriptionForm().context(auditInfo).hrid(hrid).id();
        try {
            var output = getSubscriptionFormUseCase.execute(
                new GetSubscriptionFormUseCase.Input(auditInfo.environmentId(), SubscriptionFormId.of(formId))
            );
            var form = output.subscriptionForm();
            var apiHrids = SubscriptionFormMapper.INSTANCE.toApiHrids(form.getApiIds(), apiCrudService, auditInfo);
            return Response.ok(SubscriptionFormMapper.INSTANCE.toState(form, hrid, auditInfo, apiHrids)).build();
        } catch (SubscriptionFormNotFoundException e) {
            throw new HRIDNotFoundException(hrid);
        }
    }

    @DELETE
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_SUBSCRIPTION_FORM, acls = RolePermissionAction.DELETE) })
    public Response deleteSubscriptionFormByHrid(@PathParam("hrid") String hrid) {
        var auditInfo = getAuditInfo();
        var formId = HRIDToUUID.subscriptionForm().context(auditInfo).hrid(hrid).id();
        try {
            deleteSubscriptionFormUseCase.execute(
                new DeleteSubscriptionFormUseCase.Input(auditInfo.environmentId(), SubscriptionFormId.of(formId))
            );
        } catch (SubscriptionFormNotFoundException e) {
            throw new HRIDNotFoundException(hrid);
        }
        return Response.noContent().build();
    }
}
