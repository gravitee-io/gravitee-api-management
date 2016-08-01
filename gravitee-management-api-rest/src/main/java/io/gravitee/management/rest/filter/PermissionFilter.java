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
package io.gravitee.management.rest.filter;

import io.gravitee.management.service.exceptions.ForbiddenAccessException;
import io.gravitee.management.service.exceptions.UnauthorizedAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.security.Principal;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
abstract class PermissionFilter<T extends Annotation> implements ContainerRequestFilter {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Class<T> annotation;

    PermissionFilter(Class<T> annotation) {
        this.annotation = annotation;
    }

    @Context
    protected ResourceInfo resourceInfo;

    @Inject
    protected SecurityContext securityContext;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (securityContext.isUserInRole("ADMIN")) {
            logger.debug("User [{}] has full access because of its ADMIN role",
                    securityContext.getUserPrincipal().getName());
            return;
        }

        filter(getRequiredPermission(), requestContext);
    }

    protected abstract void filter(T permissions, ContainerRequestContext requestContext);

    protected T getRequiredPermission() {
        T permission = resourceInfo.getResourceMethod().getDeclaredAnnotation(annotation);

        if (permission == null) {
            return resourceInfo.getResourceClass().getDeclaredAnnotation(annotation);
        }

        return permission;
    }

    protected void sendSecurityError(ContainerRequestContext requestContext) {
        Principal principal = securityContext.getUserPrincipal();
        if (principal != null) {
            throw new ForbiddenAccessException();
        } else {
            throw new UnauthorizedAccessException();
        }
    }
}
