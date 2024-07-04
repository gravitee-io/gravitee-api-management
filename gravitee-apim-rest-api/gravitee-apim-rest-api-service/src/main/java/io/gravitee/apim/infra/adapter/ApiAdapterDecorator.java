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
package io.gravitee.apim.infra.adapter;

import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.model.context.OriginContext;
import io.gravitee.rest.api.model.federation.FederatedApiEntity;

public abstract class ApiAdapterDecorator implements ApiAdapter {

    private final ApiAdapter delegate;

    public ApiAdapterDecorator(ApiAdapter delegate) {
        this.delegate = delegate;
    }

    @Override
    public Api toRepository(io.gravitee.apim.core.api.model.Api source) {
        var api = delegate.toRepository(source);

        api.setOrigin(source.getOriginContext().name());
        if (source.getOriginContext() instanceof OriginContext.Integration integration) {
            api.setIntegrationId(integration.integrationId());
        } else if (source.getOriginContext() instanceof OriginContext.Kubernetes kub) {
            api.setMode(kub.mode().name().toLowerCase());
            api.setSyncFrom(kub.syncFrom());
        }

        return api;
    }

    @Override
    public io.gravitee.apim.core.api.model.Api toCoreModel(Api source) {
        var api = delegate.toCoreModel(source);
        api.setOriginContext(toOriginContext(source));
        return api;
    }

    @Override
    public FederatedApiEntity toFederatedApiEntity(Api source, PrimaryOwnerEntity primaryOwnerEntity) {
        var api = delegate.toFederatedApiEntity(source, primaryOwnerEntity);
        OriginContext originContext = toOriginContext(source);
        if (originContext instanceof OriginContext.Integration integrationCtx) {
            api.setOriginContext(integrationCtx);
        }
        api.setCategories(source.getCategories());
        return api;
    }

    public static OriginContext toOriginContext(Api source) {
        return switch (getOriginContextOrDefault(source)) {
            case MANAGEMENT -> new OriginContext.Management();
            case KUBERNETES -> new OriginContext.Kubernetes(
                source.getMode() != null
                    ? OriginContext.Kubernetes.Mode.valueOf(source.getMode().toUpperCase())
                    : OriginContext.Kubernetes.Mode.FULLY_MANAGED
            );
            case INTEGRATION -> new OriginContext.Integration(source.getIntegrationId());
        };
    }

    private static OriginContext.Origin getOriginContextOrDefault(Api source) {
        try {
            return source.getOrigin() == null
                ? OriginContext.Origin.MANAGEMENT
                : OriginContext.Origin.valueOf(source.getOrigin().toUpperCase());
        } catch (IllegalArgumentException e) {
            return OriginContext.Origin.MANAGEMENT;
        }
    }
}
