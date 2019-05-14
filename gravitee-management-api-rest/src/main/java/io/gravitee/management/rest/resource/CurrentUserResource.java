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

import com.auth0.jwt.JWTSigner;
import io.gravitee.common.http.MediaType;
import io.gravitee.management.idp.api.authentication.UserDetailRole;
import io.gravitee.management.idp.api.authentication.UserDetails;
import io.gravitee.management.model.*;
import io.gravitee.management.rest.model.PagedResult;
import io.gravitee.management.rest.model.TokenEntity;
import io.gravitee.management.security.cookies.JWTCookieGenerator;
import io.gravitee.management.service.TaskService;
import io.gravitee.management.service.UserService;
import io.gravitee.management.service.common.JWTHelper.Claims;
import io.gravitee.management.service.exceptions.ForbiddenAccessException;
import io.gravitee.management.service.exceptions.UserNotFoundException;
import io.gravitee.repository.management.model.MembershipDefaultReferenceId;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.repository.management.model.RoleScope;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.*;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

import static io.gravitee.management.rest.model.TokenType.BEARER;
import static io.gravitee.management.service.common.JWTHelper.DefaultValues.DEFAULT_JWT_EXPIRE_AFTER;
import static io.gravitee.management.service.common.JWTHelper.DefaultValues.DEFAULT_JWT_ISSUER;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Path("/user")
@Api(tags = {"User"})
public class CurrentUserResource extends AbstractResource {

    private static Logger LOG = LoggerFactory.getLogger(CurrentUserResource.class);

    @Autowired
    private UserService userService;
    @Context
    private HttpServletResponse response;
    @Autowired
    private TaskService taskService;
    @Context
    private ResourceContext resourceContext;
    @Autowired
    private ConfigurableEnvironment environment;
    @Autowired
    private JWTCookieGenerator jwtCookieGenerator;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get the authenticated user")
    public Response getCurrentUser() {
        if (isAuthenticated()) {
            final UserDetails details = getAuthenticatedUserDetails();
            final String userId = details.getUsername();
            final String password = details.getPassword() != null ? details.getPassword() : "";
            UserEntity userEntity;
            try {
                userEntity = userService.findByIdWithRoles(userId);
            } catch (final UserNotFoundException unfe) {
                final String unfeMessage = "User '{}' does not exist.";
                if (LOG.isDebugEnabled()) {
                    LOG.info(unfeMessage, userId, unfe);
                } else {
                    LOG.info(unfeMessage, userId);
                }
                response.addCookie(jwtCookieGenerator.generate(null));
                return status(Response.Status.UNAUTHORIZED).build();
            }

            List<GrantedAuthority> authorities = new ArrayList<>(details.getAuthorities());

            UserDetails userDetails = new UserDetails(userEntity.getId(), password, authorities);
            userDetails.setId(userEntity.getId());
            userDetails.setFirstname(details.getFirstname());
            userDetails.setLastname(details.getLastname());
            userDetails.setUsername(userEntity.getUsername());
            userDetails.setEmail(details.getEmail());

            //convert UserEntityRoles to UserDetailsRoles
            userDetails.setRoles(userEntity.getRoles().
                    stream().
                    map(userEntityRole -> {
                        UserDetailRole userDetailRole = new UserDetailRole();
                        userDetailRole.setScope(userEntityRole.getScope().name());
                        userDetailRole.setName(userEntityRole.getName());
                        userDetailRole.setPermissions(userEntityRole.getPermissions());
                        return userDetailRole;
                    }).collect(Collectors.toList()));

            return ok(userDetails, MediaType.APPLICATION_JSON).build();
        } else {
            return ok().build();
        }
    }

    @PUT
    @ApiOperation(value = "Update user")
    public Response updateCurrentUser(@Valid @NotNull final UpdateUserEntity user) {
        if (!user.getUsername().equals(userService.findById(getAuthenticatedUser()).getUsername())) {
            throw new ForbiddenAccessException();
        }
        checkImage(user.getPicture());
        return ok(userService.update(user)).build();
    }

    @GET
    @Path("avatar")
    @ApiOperation(value = "Get user's avatar")
    public Response getCurrentUserPicture(@Context Request request) {
        String userId = userService.findById(getAuthenticatedUser()).getId();
        PictureEntity picture = userService.getPicture(userId);

        if (picture == null) {
            throw new NotFoundException();
        }

        if (picture instanceof UrlPictureEntity) {
            return Response.temporaryRedirect(URI.create(((UrlPictureEntity) picture).getUrl())).build();
        }

        InlinePictureEntity image = (InlinePictureEntity) picture;

        EntityTag etag = new EntityTag(Integer.toString(new String(image.getContent()).hashCode()));
        Response.ResponseBuilder builder = request.evaluatePreconditions(etag);

        if (builder != null) {
            return builder.build();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(image.getContent(), 0, image.getContent().length);

        return ok()
                .entity(baos)
                .tag(etag)
                .type(image.getType())
                .build();
    }

    @POST
    @Path("/login")
    @ApiOperation(value = "Login")
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(final @Context javax.ws.rs.core.HttpHeaders headers, final @Context HttpServletResponse servletResponse) {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            // JWT signer
            final Map<String, Object> claims = new HashMap<>();
            claims.put(Claims.ISSUER, environment.getProperty("jwt.issuer", DEFAULT_JWT_ISSUER));

            final UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            // Manage authorities, initialize it with dynamic permissions from the IDP
            Set<GrantedAuthority> authorities = new HashSet<>(userDetails.getAuthorities());

            // We must also load permissions from repository for configured management or portal role
            RoleEntity role = membershipService.getRole(
                    MembershipReferenceType.MANAGEMENT,
                    MembershipDefaultReferenceId.DEFAULT.toString(),
                    userDetails.getUsername(),
                    RoleScope.MANAGEMENT);
            if (role != null) {
                authorities.add(new SimpleGrantedAuthority(role.getScope().toString() + ':' + role.getName()));
            }

            role = membershipService.getRole(
                    MembershipReferenceType.PORTAL,
                    MembershipDefaultReferenceId.DEFAULT.toString(),
                    userDetails.getUsername(),
                    RoleScope.PORTAL);
            if (role != null) {
                authorities.add(new SimpleGrantedAuthority(role.getScope().toString() + ':' + role.getName()));
            }

            claims.put(Claims.PERMISSIONS, authorities);
            claims.put(Claims.SUBJECT, userDetails.getUsername());
            claims.put(Claims.EMAIL, userDetails.getEmail());
            claims.put(Claims.FIRSTNAME, userDetails.getFirstname());
            claims.put(Claims.LASTNAME, userDetails.getLastname());

            final JWTSigner.Options options = new JWTSigner.Options();
            options.setExpirySeconds(environment.getProperty("jwt.expire-after", Integer.class, DEFAULT_JWT_EXPIRE_AFTER));
            options.setIssuedAt(true);
            options.setJwtId(true);

            final String sign = new JWTSigner(environment.getProperty("jwt.secret")).sign(claims, options);
            final TokenEntity tokenEntity = new TokenEntity();
            tokenEntity.setType(BEARER);
            tokenEntity.setToken(sign);

            final Cookie bearerCookie = jwtCookieGenerator.generate("Bearer%20" + sign);
            servletResponse.addCookie(bearerCookie);

            return ok(tokenEntity).build();
        }
        return ok().build();
    }

    @POST
    @Path("/logout")
    @ApiOperation(value = "Logout")
    public Response logout() {
        response.addCookie(jwtCookieGenerator.generate(null));
        return Response.ok().build();
    }

    @GET
    @Path("/tasks")
    @Produces(MediaType.APPLICATION_JSON)
    public PagedResult getUserTasks() {
        List<TaskEntity> tasks = taskService.findAll(getAuthenticatedUserOrNull());
        Map<String, Map<String, Object>> metadata = taskService.getMetadata(tasks).getMetadata();
        PagedResult<TaskEntity> pagedResult = new PagedResult<>(tasks);
        pagedResult.setMetadata(metadata);
        return pagedResult;
    }


    @Path("/notifications")
    public UserNotificationsResource getUserNotificationsResource() {
        return resourceContext.getResource(UserNotificationsResource.class);
    }
}
