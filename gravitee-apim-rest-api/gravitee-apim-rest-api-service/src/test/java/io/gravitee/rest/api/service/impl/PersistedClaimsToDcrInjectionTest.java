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
package io.gravitee.rest.api.service.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.collections.Sets.newSet;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.Options;
import io.gravitee.repository.management.api.ClientRegistrationProviderRepository;
import io.gravitee.repository.management.api.UserRepository;
import io.gravitee.repository.management.model.ClientRegistrationProvider;
import io.gravitee.repository.management.model.User;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.NewApplicationEntity;
import io.gravitee.rest.api.model.OrganizationEntity;
import io.gravitee.rest.api.model.application.ApplicationSettings;
import io.gravitee.rest.api.model.application.OAuthClientSettings;
import io.gravitee.rest.api.model.configuration.identity.SocialIdentityProviderEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.OrganizationService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.converter.UserConverter;
import io.gravitee.rest.api.service.impl.configuration.application.registration.ClientRegistrationServiceImpl;
import io.gravitee.rest.api.service.impl.configuration.application.registration.client.register.ClientRegistrationResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Joins the two halves of the claims-to-DCR feature: a federated login captures whitelisted IdP claims onto the user,
 * and a later dynamic client registration injects those claims into the request body.
 *
 * <p>Each half already has its own tests, but both stub the seam between them — the injection tests hand
 * {@code ClientRegistrationService#register} a literal claim map, so nothing asserts that a real login actually
 * produces the map the DCR path later reads. That seam is {@code UserService#findIdpClaims}, which
 * {@code ApplicationServiceImpl} calls to feed the registration. A change to what login persists, or to how the claims
 * are read back, would leave both halves green and break the feature.
 *
 * <p>The user repository here is a small in-memory store rather than a plain stub, and it copies on both read and
 * write. That matters: production mutates the user instance before writing it, so a store that handed back the same
 * instance would stay green even if the repository write were deleted entirely — the mistake the APIM-14840 follow-up
 * had to correct in the existing purge tests. Copying forces the claims to survive a real round trip.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PersistedClaimsToDcrInjectionTest {

    private static final String ORGANIZATION = "organization#Id";
    private static final String ENVIRONMENT = "environment#Id";
    private static final ExecutionContext EXECUTION_CONTEXT = new ExecutionContext(ORGANIZATION, ENVIRONMENT);

    /** The subject of the userinfo fixture, which the profile mapping below uses as the user's id. */
    private static final String USER_ID = "janedoe@example.com";

    @InjectMocks
    private UserServiceImpl userService = new UserServiceImpl();

    @InjectMocks
    private ClientRegistrationServiceImpl clientRegistrationService = new ClientRegistrationServiceImpl();

    @Mock
    private UserRepository userRepository;

    @Mock
    private ClientRegistrationProviderRepository clientRegistrationProviderRepository;

    @Mock
    private SocialIdentityProviderEntity identityProvider;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private AuditService auditService;

    @Mock
    private MembershipService membershipService;

    @Mock
    private OrganizationService organizationService;

    @Mock
    private RoleService roleService;

    // A pure mapper: the real one, so the entity the login path builds is the one it would build in production.
    @Spy
    private UserConverter userConverter = new UserConverter();

    private final WireMockServer wireMockServer = new WireMockServer(Options.DYNAMIC_PORT);

    /** Stands in for the users table. Keyed by user id; values are copies, never the caller's instance. */
    private final Map<String, User> userStore = new HashMap<>();

    @BeforeEach
    public void setup() throws Exception {
        wireMockServer.start();

        User existing = new User();
        existing.setId(USER_ID);
        existing.setSourceId(USER_ID);
        existing.setOrganizationId(ORGANIZATION);
        existing.setEmail(USER_ID);
        existing.setFirstname("Jane");
        existing.setLastname("Doe");
        userStore.put(USER_ID, existing);

        when(userRepository.findBySource(any(), eq(USER_ID), eq(ORGANIZATION))).thenAnswer(invocation ->
            Optional.ofNullable(userStore.get(USER_ID)).map(User::new)
        );
        when(userRepository.findById(anyString())).thenAnswer(invocation ->
            Optional.ofNullable(userStore.get(invocation.<String>getArgument(0))).map(User::new)
        );
        when(userRepository.update(any(User.class))).thenAnswer(invocation -> {
            User written = new User(invocation.getArgument(0));
            userStore.put(written.getId(), written);
            return new User(written);
        });

        Map<String, String> userProfileMapping = new HashMap<>();
        userProfileMapping.put(SocialIdentityProviderEntity.UserProfile.EMAIL, "email");
        userProfileMapping.put(SocialIdentityProviderEntity.UserProfile.ID, "email");
        userProfileMapping.put(SocialIdentityProviderEntity.UserProfile.SUB, "sub");
        userProfileMapping.put(SocialIdentityProviderEntity.UserProfile.FIRSTNAME, "given_name");
        userProfileMapping.put(SocialIdentityProviderEntity.UserProfile.LASTNAME, "family_name");
        userProfileMapping.put(SocialIdentityProviderEntity.UserProfile.PICTURE, "picture");
        when(identityProvider.getUserProfileMapping()).thenReturn(userProfileMapping);

        when(organizationService.findById(ORGANIZATION)).thenReturn(new OrganizationEntity());

        EnvironmentEntity defaultEnv = new EnvironmentEntity();
        defaultEnv.setId(ENVIRONMENT);
        when(environmentService.findByOrganization(ORGANIZATION)).thenReturn(List.of(defaultEnv));
    }

    @AfterEach
    public void tearDown() {
        wireMockServer.stop();
    }

    @Test
    public void should_inject_the_claims_a_real_login_captured_into_the_dcr_request() throws Exception {
        // 'service_id' and 'job_id' are whitelisted and present in the userinfo fixture. 'email' is deliberately NOT
        // whitelisted, though the registration provider maps it — a claim login refused to keep must stay uninjectable.
        when(identityProvider.getPersistedClaimsWhitelist()).thenReturn(List.of("service_id", "job_id"));

        NewApplicationEntity application = givenAProviderMapping(
            Map.of("service_id", "metadata.organization", "job_id", "metadata.job", "email", "metadata.email")
        );

        login();

        // The seam under test: what the DCR path actually reads back, with nothing stubbed between the two halves.
        Map<String, String> claimsForRegistration = userService.findIdpClaims(EXECUTION_CONTEXT, USER_ID);
        assertNotNull(claimsForRegistration, "login persisted no claims, so the DCR path has nothing to inject");
        assertEquals(2, claimsForRegistration.size(), "only the whitelisted claims should survive login");

        clientRegistrationService.register(GraviteeContext.getExecutionContext(), application, claimsForRegistration);

        wireMockServer.verify(
            postRequestedFor(urlEqualTo("/registrationEp"))
                .withRequestBody(matchingJsonPath("$.metadata.organization", equalTo("585252525")))
                .withRequestBody(matchingJsonPath("$.metadata.job", equalTo("API_FRIENDLY_USER")))
        );
        // The whitelist filter survives the join: mapping a claim the login never stored injects nothing.
        wireMockServer.verify(0, postRequestedFor(urlEqualTo("/registrationEp")).withRequestBody(matchingJsonPath("$.metadata.email")));
    }

    @Test
    public void should_inject_nothing_after_login_purges_the_claims_it_had_stored() throws Exception {
        // The user starts out holding a claim from an earlier login, so clearing the whitelist has something to purge.
        // Without that seed the test is vacuous: an empty whitelist computes null claims, the write guard sees null on
        // both sides and skips the write, and every assertion below would hold with the login removed entirely.
        User seeded = new User(userStore.get(USER_ID));
        seeded.setIdpClaims(new HashMap<>(Map.of("service_id", "585252525")));
        userStore.put(USER_ID, seeded);

        // No whitelist means login retains nothing (APIM-14840), so the mapped claim must be purged before the DCR call.
        when(identityProvider.getPersistedClaimsWhitelist()).thenReturn(List.of());

        NewApplicationEntity application = givenAProviderMapping(Map.of("service_id", "metadata.organization"));

        login();

        Map<String, String> claimsForRegistration = userService.findIdpClaims(EXECUTION_CONTEXT, USER_ID);
        assertNull(claimsForRegistration, "login should have purged the previously stored claims");

        ClientRegistrationResponse registration = clientRegistrationService.register(
            GraviteeContext.getExecutionContext(),
            application,
            claimsForRegistration
        );
        assertNotNull(registration, "the registration itself must still succeed");

        // A registration really was sent — without this the negative check below would also pass if no POST happened.
        wireMockServer.verify(postRequestedFor(urlEqualTo("/registrationEp")));
        wireMockServer.verify(
            0,
            postRequestedFor(urlEqualTo("/registrationEp")).withRequestBody(matchingJsonPath("$.metadata.organization"))
        );
    }

    /** Drives a federated login with the shared OAuth2 fixtures, which is what writes the claims to the store. */
    private void login() throws IOException {
        String userInfo = IOUtils.toString(read("/oauth2/json/user_info_response_body.json"), Charset.defaultCharset());
        String accessToken = IOUtils.toString(read("/oauth2/jwt/access_token.jwt"), Charset.defaultCharset());
        String idToken = IOUtils.toString(read("/oauth2/jwt/id_token.jwt"), Charset.defaultCharset());

        userService.createOrUpdateUserFromSocialIdentityProvider(EXECUTION_CONTEXT, identityProvider, userInfo, accessToken, idToken);
    }

    /** Registers a client registration provider with the given claim mappings and stubs its DCR endpoints. */
    private NewApplicationEntity givenAProviderMapping(Map<String, String> claimMappings) throws Exception {
        NewApplicationEntity application = new NewApplicationEntity();
        ApplicationSettings applicationSettings = new ApplicationSettings();
        applicationSettings.setOauth(new OAuthClientSettings());
        application.setSettings(applicationSettings);

        ClientRegistrationProvider provider = new ClientRegistrationProvider();
        provider.setId("CRP_ID");
        provider.setName("name");
        provider.setDiscoveryEndpoint("http://localhost:" + wireMockServer.port() + "/am");
        provider.setClaimMappings(claimMappings);

        when(
            clientRegistrationProviderRepository.findAllByEnvironment(eq(GraviteeContext.getExecutionContext().getEnvironmentId()))
        ).thenReturn(newSet(provider));

        wireMockServer.stubFor(
            get(urlEqualTo("/am")).willReturn(
                aResponse().withBody(
                    "{\"token_endpoint\": \"http://localhost:" +
                        wireMockServer.port() +
                        "/tokenEp\",\"registration_endpoint\": \"http://localhost:" +
                        wireMockServer.port() +
                        "/registrationEp\"}"
                )
            )
        );
        wireMockServer.stubFor(
            post(urlEqualTo("/tokenEp")).willReturn(aResponse().withBody("{\"access_token\": \"myToken\",\"scope\": \"scope\"}"))
        );
        wireMockServer.stubFor(post(urlEqualTo("/registrationEp")).willReturn(aResponse().withBody("{ \"client_name\": \"gravitee\"}")));
        return application;
    }

    private InputStream read(String resource) {
        return this.getClass().getResourceAsStream(resource);
    }
}
