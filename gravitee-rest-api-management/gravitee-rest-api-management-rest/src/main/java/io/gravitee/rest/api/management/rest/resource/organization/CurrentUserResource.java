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
package io.gravitee.rest.api.management.rest.resource.organization;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import io.gravitee.common.http.MediaType;
import io.gravitee.common.util.Maps;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.GroupRepository;
import io.gravitee.repository.management.model.Group;
import io.gravitee.rest.api.exception.InvalidImageException;
import io.gravitee.rest.api.idp.api.authentication.UserDetailRole;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.management.rest.model.PagedResult;
import io.gravitee.rest.api.management.rest.model.TokenEntity;
import io.gravitee.rest.api.management.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.rest.resource.TokensResource;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.security.cookies.CookieGenerator;
import io.gravitee.rest.api.security.filter.TokenAuthenticationFilter;
import io.gravitee.rest.api.security.utils.ImageUtils;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.JWTHelper;
import io.gravitee.rest.api.service.common.JWTHelper.Claims;
import io.gravitee.rest.api.service.exceptions.UserNotFoundException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.inject.Inject;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.gravitee.rest.api.management.rest.model.TokenType.BEARER;
import static io.gravitee.rest.api.service.common.JWTHelper.DefaultValues.DEFAULT_JWT_EXPIRE_AFTER;
import static io.gravitee.rest.api.service.common.JWTHelper.DefaultValues.DEFAULT_JWT_ISSUER;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"Current User"})
public class CurrentUserResource extends AbstractResource {

    public static final String IDP_SOURCE_MEMORY = "memory";
    private static Logger LOG = LoggerFactory.getLogger(CurrentUserResource.class);

    @Inject
    private UserService userService;
    @Context
    private HttpServletResponse response;
    @Inject
    private TaskService taskService;
    @Context
    private ResourceContext resourceContext;
    @Inject
    private ConfigurableEnvironment environment;
    @Inject
    private CookieGenerator cookieGenerator;
    @Inject
    private TagService tagService;
    @Inject
    private EnvironmentService environmentService;
    @Inject
    private GroupRepository groupRepository;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get the authenticated user")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Authenticated user", response = UserDetails.class),
            @ApiResponse(code = 401, message = "Unauthorized user"),
            @ApiResponse(code = 500, message = "Internal server error")})
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
                response.addCookie(cookieGenerator.generate(TokenAuthenticationFilter.AUTH_COOKIE_NAME, null));
                return status(Response.Status.UNAUTHORIZED).build();
            }

            List<GrantedAuthority> authorities = new ArrayList<>(details.getAuthorities());

            UserDetails userDetails = new UserDetails(userEntity.getId(), password, authorities);
            userDetails.setId(userEntity.getId());
            // in case of memory user, look at the repository layer to get value updated by the user through the MyAccount page
            userDetails.setFirstname(IDP_SOURCE_MEMORY.equals(userEntity.getSource()) && userEntity.getFirstname() != null ? userEntity.getFirstname() : details.getFirstname());
            userDetails.setLastname(IDP_SOURCE_MEMORY.equals(userEntity.getSource()) && userEntity.getLastname() != null ? userEntity.getLastname() : details.getLastname());
            userDetails.setSource(userEntity.getSource());
            userDetails.setSourceId(userEntity.getSourceId());
            userDetails.setPrimaryOwner(userEntity.isPrimaryOwner());
            userDetails.setCreatedAt(userEntity.getCreatedAt());
            userDetails.setUpdatedAt(userEntity.getUpdatedAt());
            userDetails.setLastConnectionAt(userEntity.getLastConnectionAt());

            if (details.getEmail() == null && IDP_SOURCE_MEMORY.equals(userEntity.getSource()) && userEntity.getEmail() != null) {
                userDetails.setEmail(userEntity.getEmail());
            } else {
                userDetails.setEmail(details.getEmail());
            }

            if (userEntity.getNewsletterSubscribed() == null && userEntity.getFirstConnectionAt() != null) {
                long diffInMs = Math.abs(new Date().getTime() - userEntity.getFirstConnectionAt().getTime());
                long diff = TimeUnit.DAYS.convert(diffInMs, TimeUnit.MILLISECONDS);
                userDetails.setDisplayNewsletterSubscription(diff >= 7);
            } else {
                userDetails.setDisplayNewsletterSubscription(false);
            }

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

            final Set<MembershipEntity> memberships = membershipService.getMembershipsByMemberAndReference(
                    MembershipMemberType.USER, userId, MembershipReferenceType.GROUP);
            if (!memberships.isEmpty()) {
                final Map<String, Set<String>> userGroups = new HashMap<>();
                environmentService.findByOrganization(GraviteeContext.getCurrentOrganization()).forEach(environment -> {
                    try {
                        final Set<Group> groups = groupRepository.findAllByEnvironment(environment.getId());
                        userGroups.put(environment.getName(), new HashSet<>());
                        memberships.stream()
                                .map(MembershipEntity::getReferenceId)
                                .forEach(groupId -> {
                                    final Optional<Group> optionalGroup = groups.stream()
                                            .filter(group -> groupId.equals(group.getId())).findFirst();
                                    optionalGroup.ifPresent(entity ->
                                            userGroups.get(environment.getName()).add(entity.getName()));
                                });
                        userDetails.setGroupsByEnvironment(userGroups);
                    } catch (TechnicalException e) {
                        LOG.error("Error while trying to get groups of the user " + userId, e);
                    }
                });
            }

            userDetails.setFirstLogin(1 == userEntity.getLoginCount());
            if (userEntity.getCustomFields() != null) {
                userDetails.setCustomFields(userEntity.getCustomFields());
            }
            return ok(userDetails, MediaType.APPLICATION_JSON).build();
        } else {
            return ok().build();
        }
    }

    @DELETE
    @ApiOperation(value = "Delete the current logged user")
    @ApiResponses({
            @ApiResponse(code = 204, message = "Current user successfully deleted"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public Response deleteCurrentUser() {

        if (isAuthenticated()) {
            userService.delete(getAuthenticatedUser());
            logoutCurrentUser();
            return Response.noContent().build();
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
    }

    @PUT
    @ApiOperation(value = "Update the authenticated user")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Updated user", response = UserEntity.class),
            @ApiResponse(code = 400, message = "Invalid user profile"),
            @ApiResponse(code = 404, message = "User not found"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public Response updateCurrentUser(@Valid @NotNull final UpdateUserEntity user) {
        UserEntity userEntity = userService.findById(getAuthenticatedUser());
        try {
            if (user.getPicture() != null) {
                user.setPicture(ImageUtils.verifyAndRescale(user.getPicture()).toBase64());
            } else {
                // preserve the picture if the input picture is empty
                user.setPicture(userEntity.getPicture());
            }
        } catch (InvalidImageException e) {
            throw new BadRequestException("Invalid image format");
        }

        return ok(userService.update(userEntity.getId(), user)).build();
    }

    @POST
    @Path("/subscribeNewsletter")
    @ApiOperation(value = "Subscribe to the newsletter the authenticated user")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Updated user", response = UserEntity.class),
            @ApiResponse(code = 400, message = "Invalid user profile"),
            @ApiResponse(code = 404, message = "User not found"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public Response subscribeNewsletterToCurrentUser(@Valid @NotNull final String email) {
        UserEntity userEntity = userService.findById(getAuthenticatedUser());
        UpdateUserEntity user = new UpdateUserEntity(userEntity);
        user.setNewsletter(true);
        return ok(userService.update(userEntity.getId(), user, email)).build();
    }

    @GET
    @Path("avatar")
    @ApiOperation(value = "Get user's avatar")
    @ApiResponses({
            @ApiResponse(code = 200, message = "User's avatar"),
            @ApiResponse(code = 404, message = "User not found"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public Response getCurrentUserPicture(@Context Request request) {
        String userId = userService.findById(getAuthenticatedUser()).getId();
        PictureEntity picture = userService.getPicture(userId);

        if (picture instanceof UrlPictureEntity) {
            return Response.temporaryRedirect(URI.create(((UrlPictureEntity) picture).getUrl())).build();
        }

        InlinePictureEntity image = (InlinePictureEntity) picture;
        if (image == null || image.getContent() == null) {
            return Response.ok().build();
        }

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
            List<Map<String, String>> authorities = userDetails.getAuthorities().stream().map(authority -> Maps.<String, String>builder().put("authority", authority.getAuthority()).build()).collect(Collectors.toList());

            // We must also load permissions from repository for configured management or portal role
            Set<RoleEntity> roles = membershipService.getRoles(
                    MembershipReferenceType.ORGANIZATION,
                    GraviteeContext.getCurrentOrganization(),
                    MembershipMemberType.USER,
                    userDetails.getUsername());
            if (!roles.isEmpty()) {
                roles.forEach(role -> authorities.add(Maps.<String, String>builder().put("authority", role.getScope().toString() + ':' + role.getName()).build()));
            }

            this.environmentService.findByOrganization(GraviteeContext.getCurrentOrganization()).stream()
                    .flatMap(env -> membershipService.getRoles(
                            MembershipReferenceType.ENVIRONMENT,
                            env.getId(),
                            MembershipMemberType.USER,
                            userDetails.getUsername()).stream())
                    .filter(Objects::nonNull)
                    .forEach(role -> authorities.add(Maps.<String, String>builder().put("authority", role.getScope().toString() + ':' + role.getName()).build()));


            // JWT signer
            Algorithm algorithm = Algorithm.HMAC256(environment.getProperty("jwt.secret"));

            Date issueAt = new Date();
            Instant expireAt = issueAt.toInstant().plus(Duration.ofSeconds(environment.getProperty("jwt.expire-after",
                    Integer.class, DEFAULT_JWT_EXPIRE_AFTER)));

            final String token = JWT.create()
                    .withIssuer(environment.getProperty("jwt.issuer", DEFAULT_JWT_ISSUER))
                    .withIssuedAt(issueAt)
                    .withExpiresAt(Date.from(expireAt))
                    .withSubject(userDetails.getUsername())
                    .withClaim(JWTHelper.Claims.PERMISSIONS, authorities)
                    .withClaim(JWTHelper.Claims.EMAIL, userDetails.getEmail())
                    .withClaim(JWTHelper.Claims.FIRSTNAME, userDetails.getFirstname())
                    .withClaim(JWTHelper.Claims.LASTNAME, userDetails.getLastname())
                    .withJWTId(UUID.randomUUID().toString())
                    .sign(algorithm);


            final TokenEntity tokenEntity = new TokenEntity();
            tokenEntity.setType(BEARER);
            tokenEntity.setToken(token);

            final Cookie bearerCookie = cookieGenerator.generate(TokenAuthenticationFilter.AUTH_COOKIE_NAME, "Bearer%20" + token);
            servletResponse.addCookie(bearerCookie);

            return ok(tokenEntity).build();
        }
        return ok().build();
    }

    @POST
    @Path("/logout")
    @ApiOperation(value = "Logout")
    public Response logoutCurrentUser() {
        response.addCookie(cookieGenerator.generate(TokenAuthenticationFilter.AUTH_COOKIE_NAME, null));
        return Response.ok().build();
    }

    @GET
    @Path("/tasks")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get the user's tasks")
    @ApiResponses({
            @ApiResponse(code = 200, message = "User's tasks"),
            @ApiResponse(code = 404, message = "User not found"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public PagedResult getUserTasks() {
        List<TaskEntity> tasks = taskService.findAll(getAuthenticatedUserOrNull());
        Map<String, Map<String, Object>> metadata = taskService.getMetadata(tasks).getMetadata();
        PagedResult<TaskEntity> pagedResult = new PagedResult<>(tasks);
        pagedResult.setMetadata(metadata);
        return pagedResult;
    }

    @GET
    @Path("/tags")
    @ApiOperation(value = "Get the user's allowed sharding tags")
    @ApiResponses({
            @ApiResponse(code = 200, message = "User's sharding tags"),
            @ApiResponse(code = 404, message = "User not found"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserShardingTags() {
        return Response.ok(tagService.findByUser(getAuthenticatedUser())).build();
    }

    @Path("/notifications")
    public UserNotificationsResource getUserNotificationsResource() {
        return resourceContext.getResource(UserNotificationsResource.class);
    }

    @Path("/tokens")
    public TokensResource getTokensResource() {
        return resourceContext.getResource(TokensResource.class);
    }
}
