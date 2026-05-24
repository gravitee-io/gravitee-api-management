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
package io.gravitee.gamma.authorization.rest.exception;

import io.gravitee.gamma.authorization.service.exception.AuthzCascadeTooLargeException;
import io.gravitee.gamma.authorization.service.exception.AuthzEntityNotFoundException;
import io.gravitee.gamma.authorization.service.exception.AuthzInvalidArgumentException;
import io.gravitee.gamma.authorization.service.exception.AuthzInvalidEntityIdException;
import io.gravitee.gamma.authorization.service.exception.AuthzInvalidStatusTransitionException;
import io.gravitee.gamma.authorization.service.exception.AuthzPolicyNotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.util.concurrent.Callable;

/**
 * Maps authz domain exceptions to JAX-RS {@link WebApplicationException} responses inline,
 * removing the need to register plugin-specific {@code ExceptionMapper}s globally in
 * {@code GammaModuleApplication}. That global registration would force {@code gamma-rest-api}
 * to take a compile-time dependency on this module, which then bundles the plugin jar into
 * {@code rest-api/lib/} — corrupting plugin classloading via parent-first delegation.
 *
 * <p>APIM-standard exceptions ({@code UnauthorizedAccessException}, {@code ForbiddenAccessException})
 * are NOT caught here — they extend {@code AbstractManagementException} and are handled by
 * {@code ManagementExceptionMapper}, which is part of the standard set registered in
 * {@code GammaModuleApplication}.
 *
 * <p>Mirrors the AIM module's {@code IdentityCalls} pattern.
 */
public final class AuthzCalls {

    private AuthzCalls() {}

    public static <T> T execute(Callable<T> action) {
        try {
            return action.call();
        } catch (AuthzPolicyNotFoundException e) {
            throw error(Status.NOT_FOUND, "PolicyNotFound", e);
        } catch (AuthzEntityNotFoundException e) {
            throw error(Status.NOT_FOUND, "EntityNotFound", e);
        } catch (AuthzCascadeTooLargeException e) {
            throw error(Status.REQUEST_ENTITY_TOO_LARGE, "CascadeTooLarge", e);
        } catch (AuthzInvalidStatusTransitionException e) {
            throw error(Status.CONFLICT, "InvalidStatusTransition", e);
        } catch (AuthzInvalidEntityIdException e) {
            throw error(Status.BAD_REQUEST, e.code().name(), e);
        } catch (AuthzInvalidArgumentException e) {
            throw error(Status.BAD_REQUEST, "InvalidArgument", e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static WebApplicationException error(Status status, String code, Exception cause) {
        Response response = Response.status(status)
            .type(MediaType.APPLICATION_JSON)
            .entity(new ErrorBody(code, cause.getMessage()))
            .build();
        return new WebApplicationException(response);
    }
}
