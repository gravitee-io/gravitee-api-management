package io.gravitee.gamma.module.authz;

import io.gravitee.apim.plugin.gamma.api.GammaModule;
import io.gravitee.gamma.module.authz.rest.AuthzResource;

public class AuthorizationModule implements GammaModule {

    @Override
    public Class<?> restResource() {
        return AuthzResource.class;
    }
}
