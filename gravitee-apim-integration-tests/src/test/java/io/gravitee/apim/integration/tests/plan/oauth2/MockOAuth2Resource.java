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

package io.gravitee.apim.integration.tests.plan.oauth2;

import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.resource.api.ResourceConfiguration;
import io.gravitee.resource.oauth2.api.OAuth2Resource;
import io.gravitee.resource.oauth2.api.OAuth2Response;
import io.gravitee.resource.oauth2.api.openid.UserInfoResponse;

import static io.gravitee.apim.integration.tests.plan.oauth2.PlanOAuth2V4EmulationIntegrationTest.CLIENT_ID;
import static io.gravitee.apim.integration.tests.plan.oauth2.PlanOAuth2V4EmulationIntegrationTest.SUCCESS_TOKEN;
import static io.gravitee.apim.integration.tests.plan.oauth2.PlanOAuth2V4EmulationIntegrationTest.UNAUTHORIZED_TOKEN;
import static io.gravitee.apim.integration.tests.plan.oauth2.PlanOAuth2V4EmulationIntegrationTest.UNAUTHORIZED_TOKEN_WITHOUT_CLIENT_ID;
import static io.gravitee.apim.integration.tests.plan.oauth2.PlanOAuth2V4EmulationIntegrationTest.UNAUTHORIZED_WITH_INVALID_PAYLOAD;

/**
 * @author GraviteeSource Team
 */
public class MockOAuth2Resource extends OAuth2Resource<MockOAuth2Resource.MockOAuth2ResourceConfiguration> {

    public static String RESOURCE_ID = "mock-oauth2-resource";

    @Override
    public void introspect(String accessToken, Handler<OAuth2Response> responseHandler) {
        OAuth2Response response = null;

        if (SUCCESS_TOKEN.equals(accessToken)) {
            response = new OAuth2Response(true, "{ \"client_id\": \"" + CLIENT_ID + "\"}");
        } else if (UNAUTHORIZED_TOKEN_WITHOUT_CLIENT_ID.equals(accessToken)) {
            response = new OAuth2Response(true, "{}");
        } else if (UNAUTHORIZED_WITH_INVALID_PAYLOAD.equals(accessToken)) {
            response = new OAuth2Response(true, "{this _is _invalid json");
        } else if (UNAUTHORIZED_TOKEN.equals(accessToken)) {
            response = new OAuth2Response(false, null);
        } else {
            response = new OAuth2Response(false, null);
        }

        responseHandler.handle(response);
    }

    @Override
    public void userInfo(String accessToken, Handler<UserInfoResponse> responseHandler) {
    }

    public class MockOAuth2ResourceConfiguration implements ResourceConfiguration {
    }
}
