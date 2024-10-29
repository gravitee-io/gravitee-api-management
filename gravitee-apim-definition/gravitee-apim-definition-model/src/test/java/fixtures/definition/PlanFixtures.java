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
package fixtures.definition;

import io.gravitee.definition.model.federation.FederatedPlan;
import io.gravitee.definition.model.federation.SubscriptionParameter;
import io.gravitee.definition.model.v4.nativeapi.NativePlan;
import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class PlanFixtures {

    private static final String PLAN_ID = "my-plan";
    private static final String PLAN_NAME = "My plan";
    private static final String KEYLESS_PLAN_ID = "keyless";
    private static final String KEYLESS_PLAN_NAME = "Keyless";
    private static final String KEYLESS_PLAN_SECURITY_TYPE = "key-less";
    private static final String API_KEY_PLAN_ID = "apikey";
    private static final String API_KEY_PLAN_NAME = "API Key";
    private static final String API_KEY_PLAN_SECURITY_TYPE = "api-key";

    private static final Supplier<Plan.PlanBuilder> BASE_HTTP_V4 = () ->
        Plan.builder().id(PLAN_ID).name(PLAN_NAME).status(PlanStatus.PUBLISHED).mode(PlanMode.STANDARD);

    private static final Supplier<NativePlan.NativePlanBuilder> BASE_NATIVE_V4 = () ->
        NativePlan.builder().id(PLAN_ID).name(PLAN_NAME).status(PlanStatus.PUBLISHED).mode(PlanMode.STANDARD);

    private static final Supplier<io.gravitee.definition.model.Plan.PlanBuilder> BASE_V2 = () ->
        io.gravitee.definition.model.Plan.builder().id(PLAN_ID).name(PLAN_NAME).status("PUBLISHED");

    private PlanFixtures() {}

    public static class HttpV4Definition {

        private HttpV4Definition() {}

        public static Plan aKeylessV4() {
            return (Plan) BASE_HTTP_V4
                .get()
                .id(KEYLESS_PLAN_ID)
                .name(KEYLESS_PLAN_NAME)
                .security(PlanSecurity.builder().type(KEYLESS_PLAN_SECURITY_TYPE).build())
                .build();
        }

        public static Plan anApiKeyV4() {
            return (Plan) BASE_HTTP_V4
                .get()
                .id(API_KEY_PLAN_ID)
                .name(API_KEY_PLAN_NAME)
                .security(PlanSecurity.builder().type(API_KEY_PLAN_SECURITY_TYPE).build())
                .build();
        }

        public static Plan aPushPlan() {
            return (Plan) BASE_HTTP_V4.get().id("push").name("Push Plan").mode(PlanMode.PUSH).build();
        }

        public static Plan anMtlsPlanV4() {
            return (Plan) BASE_HTTP_V4.get().id("mtls").name("mTLS Plan").security(PlanSecurity.builder().type("mtls").build()).build();
        }
    }

    public static class NativeV4Definition {

        private NativeV4Definition() {}

        public static NativePlan aKeylessV4() {
            return (NativePlan) BASE_NATIVE_V4
                .get()
                .id(KEYLESS_PLAN_ID)
                .name(KEYLESS_PLAN_NAME)
                .security(PlanSecurity.builder().type(KEYLESS_PLAN_SECURITY_TYPE).build())
                .build();
        }

        public static NativePlan anApiKeyV4() {
            return (NativePlan) BASE_NATIVE_V4
                .get()
                .id(API_KEY_PLAN_ID)
                .name(API_KEY_PLAN_NAME)
                .security(PlanSecurity.builder().type(API_KEY_PLAN_SECURITY_TYPE).build())
                .build();
        }

        public static NativePlan aPushPlan() {
            return (NativePlan) BASE_NATIVE_V4.get().id("push").name("Push Plan").mode(PlanMode.PUSH).build();
        }

        public static NativePlan anMtlsPlanV4() {
            return (NativePlan) BASE_NATIVE_V4
                .get()
                .id("mtls")
                .name("mTLS Plan")
                .security(PlanSecurity.builder().type("mtls").build())
                .build();
        }
    }

    public static io.gravitee.definition.model.Plan aKeylessV2() {
        return BASE_V2
            .get()
            .id(KEYLESS_PLAN_ID)
            .name(KEYLESS_PLAN_NAME)
            .security(KEYLESS_PLAN_SECURITY_TYPE)
            .securityDefinition("{\"nice\": \"config\"}")
            .build();
    }

    public static io.gravitee.definition.model.Plan anApiKeyV2() {
        return BASE_V2.get().id(API_KEY_PLAN_ID).name(API_KEY_PLAN_NAME).security(API_KEY_PLAN_SECURITY_TYPE).build();
    }

    public static io.gravitee.definition.model.Plan aKeylessV1() {
        return BASE_V2
            .get()
            .id(KEYLESS_PLAN_ID)
            .name(KEYLESS_PLAN_NAME)
            .security(KEYLESS_PLAN_SECURITY_TYPE)
            .paths(Map.of("/", List.of()))
            .build();
    }

    public static FederatedPlan aFederatedPlan() {
        return FederatedPlan
            .builder()
            .id(PLAN_ID)
            .mode(PlanMode.STANDARD)
            .providerId("provider-id")
            .status(PlanStatus.PUBLISHED)
            .security(PlanSecurity.builder().type(API_KEY_PLAN_SECURITY_TYPE).build())
            .build();
    }

    public static SubscriptionParameter subscriptionParameter() {
        return new SubscriptionParameter.ApiKey(aFederatedPlan());
    }
}
