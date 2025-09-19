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
package io.gravitee.rest.api.management.rest.resource.organization;

import static io.gravitee.rest.api.management.rest.model.TokenType.BEARER;
import static io.gravitee.rest.api.service.common.JWTHelper.DefaultValues.DEFAULT_JWT_EXPIRE_AFTER;
import static io.gravitee.rest.api.service.common.JWTHelper.DefaultValues.DEFAULT_JWT_ISSUER;
import static jakarta.ws.rs.core.Response.ok;
import static jakarta.ws.rs.core.Response.status;

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
import io.gravitee.rest.api.management.rest.model.TokenEntity;
import io.gravitee.rest.api.management.rest.model.wrapper.TaskEntityPagedResult;
import io.gravitee.rest.api.management.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.rest.resource.TokensResource;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.security.cookies.CookieGenerator;
import io.gravitee.rest.api.security.filter.TokenAuthenticationFilter;
import io.gravitee.rest.api.security.utils.ImageUtils;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.TagService;
import io.gravitee.rest.api.service.TaskService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.JWTHelper;
import io.gravitee.rest.api.service.common.JWTHelper.Claims;
import io.gravitee.rest.api.service.exceptions.UserNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.*;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Current User")
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
    @Operation(summary = "Get the authenticated user")
    @ApiResponse(
        responseCode = "200",
        description = "Authenticated user",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = UserDetails.class))
    )
    @ApiResponse(responseCode = "401", description = "Unauthorized user")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Response getCurrentUser() {
        if (isAuthenticated()) {
            final UserDetails details = getAuthenticatedUserDetails();
            final String userId = details.getUsername();
            final String password = details.getPassword() != null ? details.getPassword() : "";
            UserEntity userEntity;
            try {
                userEntity = userService.findByIdWithRoles(GraviteeContext.getExecutionContext(), userId);
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
            userDetails.setFirstname(
                IDP_SOURCE_MEMORY.equals(userEntity.getSource()) && userEntity.getFirstname() != null
                    ? userEntity.getFirstname()
                    : details.getFirstname()
            );
            userDetails.setLastname(
                IDP_SOURCE_MEMORY.equals(userEntity.getSource()) && userEntity.getLastname() != null
                    ? userEntity.getLastname()
                    : details.getLastname()
            );
            userDetails.setSource(userEntity.getSource());
            userDetails.setSourceId(userEntity.getSourceId());
            userDetails.setPrimaryOwner(userEntity.isPrimaryOwner());
            userDetails.setCreatedAt(userEntity.getCreatedAt());
            userDetails.setUpdatedAt(userEntity.getUpdatedAt());
            userDetails.setLastConnectionAt(userEntity.getLastConnectionAt());
            userDetails.setOrganizationId(userEntity.getOrganizationId());

            if (details.getEmail() == null && IDP_SOURCE_MEMORY.equals(userEntity.getSource()) && userEntity.getEmail() != null) {
                userDetails.setEmail(userEntity.getEmail());
            } else {
                userDetails.setEmail(details.getEmail());
            }

            boolean newsletterEnabled = environment.getProperty("newsletter.enabled", boolean.class, true);
            if (newsletterEnabled && userEntity.getNewsletterSubscribed() == null && userEntity.getFirstConnectionAt() != null) {
                long diffInMs = Math.abs(new Date().getTime() - userEntity.getFirstConnectionAt().getTime());
                long diff = TimeUnit.DAYS.convert(diffInMs, TimeUnit.MILLISECONDS);
                userDetails.setDisplayNewsletterSubscription(diff >= 7);
            } else {
                userDetails.setDisplayNewsletterSubscription(false);
            }

            //convert UserEntityRoles to UserDetailsRoles
            userDetails.setRoles(
                userEntity
                    .getRoles()
                    .stream()
                    .map(userEntityRole -> {
                        UserDetailRole userDetailRole = new UserDetailRole();
                        userDetailRole.setScope(userEntityRole.getScope().name());
                        userDetailRole.setName(userEntityRole.getName());
                        userDetailRole.setPermissions(userEntityRole.getPermissions());
                        return userDetailRole;
                    })
                    .collect(Collectors.toList())
            );

            final Set<MembershipEntity> memberships = membershipService.getMembershipsByMemberAndReference(
                MembershipMemberType.USER,
                userId,
                MembershipReferenceType.GROUP
            );
            if (!memberships.isEmpty()) {
                final Map<String, Set<String>> userGroups = new HashMap<>();
                environmentService
                    .findByUser(GraviteeContext.getCurrentOrganization(), userId)
                    .forEach(environment -> {
                        try {
                            final Set<Group> groups = groupRepository.findAllByEnvironment(environment.getId());
                            userGroups.put(environment.getId(), new HashSet<>());
                            memberships
                                .stream()
                                .map(MembershipEntity::getReferenceId)
                                .forEach(groupId -> {
                                    final Optional<Group> optionalGroup = groups
                                        .stream()
                                        .filter(group -> groupId.equals(group.getId()))
                                        .findFirst();
                                    optionalGroup.ifPresent(entity -> userGroups.get(environment.getId()).add(entity.getName()));
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
    @Operation(summary = "Delete the current logged user")
    @ApiResponse(responseCode = "204", description = "Current user successfully deleted")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Response deleteCurrentUser() {
        if (isAuthenticated()) {
            userService.delete(GraviteeContext.getExecutionContext(), getAuthenticatedUser());
            logoutCurrentUser();
            return Response.noContent().build();
        } else {
            return status(Response.Status.UNAUTHORIZED).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Update the authenticated user")
    @ApiResponse(
        responseCode = "200",
        description = "Updated user",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = UserEntity.class))
    )
    @ApiResponse(responseCode = "400", description = "Invalid user profile")
    @ApiResponse(responseCode = "404", description = "User not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Response updateCurrentUser(@Valid @NotNull final UpdateUserEntity user) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        UserEntity userEntity = userService.findById(executionContext, getAuthenticatedUser());
        try {
            if (user.getPicture() != null) {
                user.setPicture(ImageUtils.verifyAndRescale(user.getPicture()).toBase64());
            } else {
                // preserve the picture if the input picture is empty
                user.setPicture(userEntity.getPicture());
            }
        } catch (InvalidImageException e) {
            LOG.warn("Invalid image format", e);
            throw new BadRequestException("Invalid image format : " + e.getMessage());
        }

        return ok(userService.update(executionContext, userEntity.getId(), user)).build();
    }

    @GET
    @Path("avatar")
    @Operation(summary = "Get user's avatar")
    @ApiResponse(
        responseCode = "200",
        description = "User's avatar",
        content = @Content(mediaType = "*/*", schema = @Schema(type = "string", format = "binary"))
    )
    @ApiResponse(responseCode = "404", description = "User not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Response getCurrentUserPicture(@Context Request request) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        String userId = userService.findById(executionContext, getAuthenticatedUser()).getId();
        PictureEntity picture = userService.getPicture(executionContext, userId);

        if (picture instanceof UrlPictureEntity) {
            return Response.temporaryRedirect(URI.create(((UrlPictureEntity) picture).getUrl())).build();
        }

        InlinePictureEntity image = (InlinePictureEntity) picture;
        if (image == null || image.getContent() == null) {
            return ok().build();
        }

        EntityTag etag = new EntityTag(Integer.toString(new String(image.getContent()).hashCode()));
        Response.ResponseBuilder builder = request.evaluatePreconditions(etag);

        if (builder != null) {
            return builder.build();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(image.getContent(), 0, image.getContent().length);

        return ok().entity(baos).tag(etag).type(image.getType()).build();
    }

    @POST
    @Path("/login")
    @Operation(summary = "Login")
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(final @Context HttpHeaders headers, final @Context HttpServletResponse servletResponse) {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            // JWT signer
            final Map<String, Object> claims = new HashMap<>();
            claims.put(Claims.ISSUER, environment.getProperty("jwt.issuer", DEFAULT_JWT_ISSUER));

            final UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            // Manage authorities, initialize it with dynamic permissions from the IDP
            List<Map<String, String>> authorities = userDetails
                .getAuthorities()
                .stream()
                .map(authority -> Maps.<String, String>builder().put("authority", authority.getAuthority()).build())
                .collect(Collectors.toList());

            // We must also load permissions from repository for configured management or portal role
            Set<RoleEntity> roles = membershipService.getRoles(
                MembershipReferenceType.ORGANIZATION,
                GraviteeContext.getCurrentOrganization(),
                MembershipMemberType.USER,
                userDetails.getUsername()
            );
            if (!roles.isEmpty()) {
                roles.forEach(role ->
                    authorities.add(
                        Maps.<String, String>builder().put("authority", role.getScope().toString() + ':' + role.getName()).build()
                    )
                );
            }

            this.environmentService.findByOrganization(GraviteeContext.getCurrentOrganization())
                .stream()
                .flatMap(env ->
                    membershipService
                        .getRoles(MembershipReferenceType.ENVIRONMENT, env.getId(), MembershipMemberType.USER, userDetails.getUsername())
                        .stream()
                )
                .filter(Objects::nonNull)
                .forEach(role ->
                    authorities.add(
                        Maps.<String, String>builder().put("authority", role.getScope().toString() + ':' + role.getName()).build()
                    )
                );

            // JWT signer
            Algorithm algorithm = Algorithm.HMAC256(environment.getProperty("jwt.secret"));

            Date issueAt = new Date();
            Instant expireAt = issueAt
                .toInstant()
                .plus(Duration.ofSeconds(environment.getProperty("jwt.expire-after", Integer.class, DEFAULT_JWT_EXPIRE_AFTER)));

            final String token = JWT.create()
                .withIssuer(environment.getProperty("jwt.issuer", DEFAULT_JWT_ISSUER))
                .withIssuedAt(issueAt)
                .withExpiresAt(Date.from(expireAt))
                .withSubject(userDetails.getUsername())
                .withClaim(JWTHelper.Claims.PERMISSIONS, authorities)
                .withClaim(JWTHelper.Claims.EMAIL, userDetails.getEmail())
                .withClaim(JWTHelper.Claims.FIRSTNAME, userDetails.getFirstname())
                .withClaim(JWTHelper.Claims.LASTNAME, userDetails.getLastname())
                .withClaim(JWTHelper.Claims.ORG, userDetails.getOrganizationId())
                .withJWTId(UUID.randomUUID().toString())
                .sign(algorithm);

            final TokenEntity tokenEntity = new TokenEntity();
            tokenEntity.setType(BEARER);
            tokenEntity.setToken(token);

            final Cookie bearerCookie = cookieGenerator.generate(TokenAuthenticationFilter.AUTH_COOKIE_NAME, "Bearer%20" + token);
            servletResponse.addCookie(bearerCookie);

            return ok(tokenEntity).build();
        }
        return status(Response.Status.UNAUTHORIZED).build();
    }

    @POST
    @Path("/logout")
    @Operation(summary = "Logout")
    public Response logoutCurrentUser() {
        response.addCookie(cookieGenerator.generate(TokenAuthenticationFilter.AUTH_COOKIE_NAME, null));
        return ok().build();
    }

    @GET
    @Path("/tasks")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get the user's tasks")
    @ApiResponse(
        responseCode = "200",
        description = "User's tasks",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TaskEntityPagedResult.class))
    )
    @ApiResponse(responseCode = "404", description = "User not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public TaskEntityPagedResult getUserTasks() {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        List<TaskEntity> tasks = taskService.findAll(executionContext, getAuthenticatedUserOrNull());
        Map<String, Map<String, Object>> metadata = taskService.getMetadata(executionContext, tasks).toMap();
        TaskEntityPagedResult pagedResult = new TaskEntityPagedResult(tasks);
        pagedResult.setMetadata(metadata);
        return pagedResult;
    }

    @GET
    @Path("/tags")
    @Operation(summary = "Get the user's allowed sharding tags")
    @ApiResponse(
        responseCode = "200",
        description = "User's sharding tags",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            array = @ArraySchema(schema = @Schema(implementation = String.class), uniqueItems = true)
        )
    )
    @ApiResponse(responseCode = "404", description = "User not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserShardingTags() {
        return ok(
            tagService.findByUser(getAuthenticatedUser(), GraviteeContext.getCurrentOrganization(), TagReferenceType.ORGANIZATION)
        ).build();
    }

    @Path("/notifications")
    public UserNotificationsResource getUserNotificationsResource() {
        return resourceContext.getResource(UserNotificationsResource.class);
    }

    @Path("/tokens")
    public TokensResource getTokensResource() {
        return resourceContext.getResource(TokensResource.class);
    }

    @Path("/newsletter")
    public NewsletterResource getNewsletterResource() {
        return resourceContext.getResource(NewsletterResource.class);
    }
}
