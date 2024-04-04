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

import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.model.context.KubernetesContext;
import io.gravitee.rest.api.model.context.ManagementContext;
import io.gravitee.rest.api.model.context.OriginContext;

public abstract class ApiAdapterDecorator implements ApiAdapter {

    private final ApiAdapter delegate;

    public ApiAdapterDecorator(ApiAdapter delegate) {
        this.delegate = delegate;
    }

    @Override
    public Api toRepository(io.gravitee.apim.core.api.model.Api source) {
        var api = delegate.toRepository(source);

        switch (source.getOriginContext().getOrigin()) {
            case MANAGEMENT -> {
                api.setOrigin(OriginContext.Origin.MANAGEMENT.name().toLowerCase());
            }
            case KUBERNETES -> {
                api.setOrigin(OriginContext.Origin.KUBERNETES.name().toLowerCase());
                api.setMode(((KubernetesContext) source.getOriginContext()).getMode().name().toLowerCase());
            }
        }

        return api;
    }

    @Override
    public io.gravitee.apim.core.api.model.Api toCoreModel(Api source) {
        var api = delegate.toCoreModel(source);

        OriginContext.Origin origin;
        try {
            if (source.getOrigin() == null) {
                origin = OriginContext.Origin.MANAGEMENT;
            } else {
                origin = OriginContext.Origin.valueOf(source.getOrigin().toUpperCase());
            }
        } catch (IllegalArgumentException e) {
            origin = OriginContext.Origin.MANAGEMENT;
        }

        switch (origin) {
            case MANAGEMENT -> {
                api.setOriginContext(new ManagementContext());
            }
            case KUBERNETES -> {
                api.setOriginContext(new KubernetesContext(KubernetesContext.Mode.valueOf(source.getMode().toUpperCase())));
            }
        }

        return api;
    }
}
