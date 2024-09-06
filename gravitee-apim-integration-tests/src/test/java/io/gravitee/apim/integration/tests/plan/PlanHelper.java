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
package io.gravitee.apim.integration.tests.plan;

import static io.gravitee.apim.integration.tests.plan.oauth2.MockOAuth2Resource.RESOURCE_ID;
import static io.gravitee.policy.jwt.alg.Signature.HMAC_HS256;
import static io.gravitee.policy.v3.jwt.resolver.KeyResolver.GIVEN_KEY;
import static java.time.temporal.ChronoUnit.HOURS;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.Plan;
import io.gravitee.definition.model.plugins.resources.Resource;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.policy.jwt.configuration.JWTPolicyConfiguration;
import io.gravitee.policy.oauth2.configuration.OAuth2PolicyConfiguration;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.HttpClient;
import java.net.URL;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PlanHelper {

    public static final String PLAN_KEYLESS_ID = "plan-keyless-id";
    public static final String PLAN_APIKEY_ID = "plan-apikey-id";
    public static final String PLAN_JWT_ID = "plan-jwt-id";
    public static final String PLAN_OAUTH2_ID = "plan-oauth2-id";
    public static final String PLAN_MTLS_ID = "plan-mtls-id";
    public static final String APPLICATION_ID = "application-id";
    public static final String SUBSCRIPTION_ID = "subscription-id";

    public static final String JWT_CLIENT_ID = "jwt-client-id";
    public static final String JWT_SECRET;

    public static String OAUTH2_CLIENT_ID = "oauth2-client-id";
    public static String OAUTH2_SUCCESS_TOKEN = "success-token";
    public static String OAUTH2_UNAUTHORIZED_TOKEN_WITHOUT_CLIENT_ID = "unauthorized-token-without-client-id";
    public static String OAUTH2_UNAUTHORIZED_WITH_INVALID_PAYLOAD = "unauthorized-token-with-invalid-payload";
    public static String OAUTH2_UNAUTHORIZED_TOKEN = "unauthorized-token";

    static {
        SecureRandom random = new SecureRandom();
        byte[] sharedSecret = new byte[32];
        random.nextBytes(sharedSecret);
        JWT_SECRET = new String(sharedSecret);
    }

    public static String getApiPath(final String apiId) {
        return "/" + apiId;
    }

    /**
     * Generate the Subscription object that would be returned by the SubscriptionService
     *
     * @return the Subscription object
     */
    public static Subscription createSubscription(final String apiId, final String planId, final boolean isExpired) {
        final Subscription subscription = new Subscription();
        subscription.setApplication(APPLICATION_ID);
        subscription.setId(SUBSCRIPTION_ID);
        subscription.setPlan(planId);
        subscription.setApi(apiId);
        if (isExpired) {
            subscription.setEndingAt(new Date(Instant.now().minus(1, HOURS).toEpochMilli()));
        }
        return subscription;
    }

    public static String generateJWT(long secondsToAdd) throws Exception {
        return generateJWT(secondsToAdd, Map.of());
    }

    public static String generateJWT(long secondsToAdd, Map<String, String> customClaims) throws Exception {
        JWTClaimsSet.Builder jwtClaimsSetBuilder = new JWTClaimsSet.Builder()
            .claim("client_id", JWT_CLIENT_ID)
            .expirationTime(Date.from(Instant.now().plusSeconds(secondsToAdd)));

        if (customClaims != null) {
            customClaims.forEach(jwtClaimsSetBuilder::claim);
        }

        SignedJWT signedJWT = new SignedJWT(new JWSHeader(HMAC_HS256.getAlg()), jwtClaimsSetBuilder.build());
        signedJWT.sign(new MACSigner(JWT_SECRET));
        return signedJWT.serialize();
    }

    public static void configurePlans(final Api api, final Set<String> planIds) {
        List<Plan> plans = new ArrayList<>();
        if (planIds.contains("KEY_LESS")) {
            io.gravitee.definition.model.Plan keylessPlan = new io.gravitee.definition.model.Plan();
            keylessPlan.setId(PLAN_KEYLESS_ID);
            keylessPlan.setApi(api.getId());
            keylessPlan.setSecurity("KEY_LESS");
            keylessPlan.setStatus("PUBLISHED");
            plans.add(keylessPlan);
        }

        if (planIds.contains("API_KEY")) {
            io.gravitee.definition.model.Plan apiKeyPlan = new io.gravitee.definition.model.Plan();
            apiKeyPlan.setId(PLAN_APIKEY_ID);
            apiKeyPlan.setApi(api.getId());
            apiKeyPlan.setSecurity("API_KEY");
            apiKeyPlan.setStatus("PUBLISHED");
            plans.add(apiKeyPlan);
        }

        if (planIds.contains("JWT")) {
            io.gravitee.definition.model.Plan jwtPlan = new Plan();
            jwtPlan.setId(PLAN_JWT_ID);
            jwtPlan.setApi(api.getId());
            jwtPlan.setSecurity("JWT");
            jwtPlan.setStatus("PUBLISHED");
            JWTPolicyConfiguration configuration = new JWTPolicyConfiguration();
            configuration.setSignature(HMAC_HS256);
            configuration.setResolverParameter(JWT_SECRET);
            configuration.setPublicKeyResolver(GIVEN_KEY);
            try {
                jwtPlan.setSecurityDefinition(new ObjectMapper().writeValueAsString(configuration));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to set JWT policy configuration", e);
            }
            plans.add(jwtPlan);
        }

        if (planIds.contains("OAUTH2")) {
            Resource resource = new Resource(RESOURCE_ID, RESOURCE_ID, new ObjectMapper().createObjectNode());
            api.getResources().add(resource);

            Plan oauth2Plan = new Plan();
            oauth2Plan.setId(PLAN_OAUTH2_ID);
            oauth2Plan.setApi(api.getId());
            oauth2Plan.setSecurity("OAUTH2");
            oauth2Plan.setStatus("PUBLISHED");

            OAuth2PolicyConfiguration configuration = new OAuth2PolicyConfiguration();
            configuration.setOauthResource(RESOURCE_ID);
            try {
                oauth2Plan.setSecurityDefinition(new ObjectMapper().writeValueAsString(configuration));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to set OAuth2 policy configuration", e);
            }
            plans.add(oauth2Plan);
        }
        if (plans.isEmpty()) {
            throw new IllegalArgumentException("No plan configured");
        }

        api.setPlans(plans);
    }

    public static void configurePlans(final io.gravitee.definition.model.v4.Api api, final Set<String> planIds) {
        List<io.gravitee.definition.model.v4.plan.Plan> plans = new ArrayList<>();
        if (planIds.contains("key-less")) {
            io.gravitee.definition.model.v4.plan.Plan keylessPlan = io.gravitee.definition.model.v4.plan.Plan
                .builder()
                .id(PLAN_KEYLESS_ID)
                .name("plan-name")
                .security(PlanSecurity.builder().type("key-less").build())
                .status(PlanStatus.PUBLISHED)
                .mode(PlanMode.STANDARD)
                .build();
            plans.add(keylessPlan);
        }

        if (planIds.contains("api-key")) {
            io.gravitee.definition.model.v4.plan.Plan apiKeyPlan = io.gravitee.definition.model.v4.plan.Plan
                .builder()
                .id(PLAN_APIKEY_ID)
                .name("plan-apikey-name")
                .security(PlanSecurity.builder().type("api-key").build())
                .status(PlanStatus.PUBLISHED)
                .mode(PlanMode.STANDARD)
                .build();
            plans.add(apiKeyPlan);
        }

        if (planIds.contains("jwt")) {
            try {
                JWTPolicyConfiguration configuration = new JWTPolicyConfiguration();
                configuration.setSignature(HMAC_HS256);
                configuration.setResolverParameter(JWT_SECRET);
                configuration.setPublicKeyResolver(GIVEN_KEY);

                io.gravitee.definition.model.v4.plan.Plan jwtPlan = io.gravitee.definition.model.v4.plan.Plan
                    .builder()
                    .id(PLAN_JWT_ID)
                    .name("plan-jwt-name")
                    .security(
                        PlanSecurity.builder().type("jwt").configuration(new ObjectMapper().writeValueAsString(configuration)).build()
                    )
                    .status(PlanStatus.PUBLISHED)
                    .mode(PlanMode.STANDARD)
                    .build();
                plans.add(jwtPlan);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to set JWT policy configuration", e);
            }
        }

        if (planIds.contains("oauth2")) {
            List<io.gravitee.definition.model.v4.resource.Resource> resources = new ArrayList<>();
            resources.add(io.gravitee.definition.model.v4.resource.Resource.builder().name(RESOURCE_ID).type(RESOURCE_ID).build());
            api.setResources(resources);
            try {
                OAuth2PolicyConfiguration configuration = new OAuth2PolicyConfiguration();
                configuration.setOauthResource(RESOURCE_ID);
                io.gravitee.definition.model.v4.plan.Plan oauth2Plan = io.gravitee.definition.model.v4.plan.Plan
                    .builder()
                    .id(PLAN_OAUTH2_ID)
                    .name("plan-oauth2-name")
                    .security(
                        PlanSecurity.builder().type("oauth2").configuration(new ObjectMapper().writeValueAsString(configuration)).build()
                    )
                    .status(PlanStatus.PUBLISHED)
                    .mode(PlanMode.STANDARD)
                    .build();
                plans.add(oauth2Plan);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to set OAuth2 policy configuration", e);
            }
        }

        if (planIds.contains("mtls")) {
            io.gravitee.definition.model.v4.plan.Plan mtlsPlan = io.gravitee.definition.model.v4.plan.Plan
                .builder()
                .id(PLAN_MTLS_ID)
                .name("plan-name")
                .security(PlanSecurity.builder().type("mtls").build())
                .status(PlanStatus.PUBLISHED)
                .mode(PlanMode.STANDARD)
                .build();
            plans.add(mtlsPlan);
        }
        if (plans.isEmpty()) {
            throw new IllegalArgumentException("No plan configured");
        }

        api.setPlans(plans);
    }

    public static HttpClient createTrustedHttpClient(Vertx vertx, int gatewayPort, boolean withCert) {
        var options = new HttpClientOptions().setSsl(true).setTrustAll(true).setDefaultPort(gatewayPort).setDefaultHost("localhost");
        if (withCert) {
            options =
                options.setPemKeyCertOptions(
                    new PemKeyCertOptions()
                        .addCertPath(getUrl("plans/mtls/client.cer").getPath())
                        .addKeyPath(getUrl("plans/mtls/client.key").getPath())
                );
        }

        return vertx.createHttpClient(options);
    }

    public static URL getUrl(String name) {
        return PlanHelper.class.getClassLoader().getResource(name);
    }
}
