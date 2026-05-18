package io.gravitee.gamma.module.authz;

import io.gravitee.gamma.module.authz.rest.AuthzResource;
import io.gravitee.apim.plugin.gamma.api.GammaModule;

public class AuthorizationModule implements GammaModule {

  @Override
  public Class<?> restResource() {
    return AuthzResource.class;
  }
}
