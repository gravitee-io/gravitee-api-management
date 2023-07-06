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
package io.gravitee.rest.api.service.cockpit.services;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.gravitee.rest.api.service.v4.ApiService;
import io.gravitee.rest.api.service.v4.ApiStateService;
import io.gravitee.rest.api.service.v4.PlanService;
import io.reactivex.rxjava3.observers.TestObserver;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ashraful Hasan (ashraful.hasan at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
public class V4ApiServiceCockpitTest {

    @Mock
    private ApiService apiServiceV4;

    @Mock
    private PlanService planServiceV4;

    @Mock
    private ApiStateService apiStateService;

    private V4ApiServiceCockpitImpl service;

    @BeforeEach
    public void setUp() throws Exception {
        service = new V4ApiServiceCockpitImpl(apiServiceV4, planServiceV4, apiStateService);
    }

    @Test
    public void should_create_publish_api() throws JsonProcessingException, InterruptedException {
        final String userId = "any-user-id";
        final ApiEntity apiEntity = new ApiEntity();
        apiEntity.setId("any-id");
        final ApiEntity startedApi = new ApiEntity();
        startedApi.setId("any-started-id");
        when(apiServiceV4.create(any(), any(), any())).thenReturn(apiEntity);
        when(planServiceV4.create(any(), any())).thenReturn(new PlanEntity());
        when(planServiceV4.publish(any(), any())).thenReturn(new PlanEntity());
        when(apiStateService.start(any(), any(), any())).thenReturn(startedApi);
        when(apiStateService.deploy(any(), any(), any(), any())).thenReturn(startedApi);
        when(apiServiceV4.update(any(), any(), any(), any())).thenReturn(startedApi);

        TestObserver<ApiEntity> observer = service.createPublishApi(userId, validApiDefinition()).test();
        observer.await();

        observer.assertValue(Objects::nonNull);
        verify(apiServiceV4, times(1)).create(any(), any(), any());
        verify(apiServiceV4, times(1)).update(any(), any(), any(), any());
        verify(planServiceV4, times(1)).create(any(), any());
        verify(planServiceV4, times(1)).publish(any(), any());
        verify(apiStateService, times(1)).start(any(), any(), any());
        verify(apiStateService, times(1)).deploy(any(), any(), any(), any());
    }

    @Test
    public void should_throw_exception() {
        final String userId = "any-user-id";
        assertThrows(JsonProcessingException.class, () -> service.createPublishApi(userId, "{invalid-json}"));
    }

    private String validApiDefinition() {
        return (
            "{\n" +
            "   \"newApiEntity\":{\n" +
            "      \"name\":\"Original\",\n" +
            "      \"apiVersion\":\"1.0\",\n" +
            "      \"definitionVersion\":\"4.0.0\",\n" +
            "      \"type\":\"proxy\",\n" +
            "      \"description\":\"Original from Cockpit - HTTP\",\n" +
            "      \"tags\":[\n" +
            "         \n" +
            "      ],\n" +
            "      \"listeners\":[\n" +
            "         {\n" +
            "            \"type\":\"http\",\n" +
            "            \"entrypoints\":[\n" +
            "               {\n" +
            "                  \"type\":\"http-proxy\",\n" +
            "                  \"qos\":\"auto\"\n" +
            "               }\n" +
            "            ],\n" +
            "            \"paths\":[\n" +
            "               {\n" +
            "                  \"path\":\"/original/http-proxy/\"\n" +
            "               }\n" +
            "            ]\n" +
            "         }\n" +
            "      ],\n" +
            "      \"endpointGroups\":[\n" +
            "         {\n" +
            "            \"name\":\"default-group\",\n" +
            "            \"type\":\"http-proxy\",\n" +
            "            \"loadBalancer\":{\n" +
            "               \n" +
            "            },\n" +
            "            \"endpoints\":[\n" +
            "               {\n" +
            "                  \"name\":\"default\",\n" +
            "                  \"type\":\"http-proxy\",\n" +
            "                  \"secondary\":false,\n" +
            "                  \"weight\":1,\n" +
            "                  \"inheritConfiguration\":false,\n" +
            "                  \"configuration\":{\n" +
            "                     \"target\":\"https://api.gravitee.io/echo\"\n" +
            "                  },\n" +
            "                  \"services\":{\n" +
            "                     \n" +
            "                  }\n" +
            "               }\n" +
            "            ],\n" +
            "            \"services\":{\n" +
            "               \n" +
            "            }\n" +
            "         }\n" +
            "      ],\n" +
            "      \"analytics\":{\n" +
            "         \"enabled\":true\n" +
            "      },\n" +
            "      \"flowExecution\":{\n" +
            "         \"mode\":\"default\",\n" +
            "         \"matchRequired\":false\n" +
            "      },\n" +
            "      \"flows\":[\n" +
            "         \n" +
            "      ],\n" +
            "      \"resources\":[\n" +
            "         {\n" +
            "            \"name\":\"my-cache\",\n" +
            "            \"type\":\"cache\",\n" +
            "            \"configuration\":{\n" +
            "               \"maxEntriesLocalHeap\":1000,\n" +
            "               \"timeToIdleSeconds\":2,\n" +
            "               \"timeToLiveSeconds\":4\n" +
            "            },\n" +
            "            \"enabled\":true\n" +
            "         },\n" +
            "         {\n" +
            "            \"name\":\"oauth\",\n" +
            "            \"type\":\"oauth2\",\n" +
            "            \"configuration\":{\n" +
            "               \"authorizationServerUrl\":\"https://authorization_server\",\n" +
            "               \"introspectionEndpoint\":\"/oauth/check_token\",\n" +
            "               \"useSystemProxy\":false,\n" +
            "               \"introspectionEndpointMethod\":\"GET\",\n" +
            "               \"scopeSeparator\":\" \",\n" +
            "               \"userInfoEndpoint\":\"/userinfo\",\n" +
            "               \"userInfoEndpointMethod\":\"GET\",\n" +
            "               \"useClientAuthorizationHeader\":true,\n" +
            "               \"clientAuthorizationHeaderName\":\"Authorization\",\n" +
            "               \"clientAuthorizationHeaderScheme\":\"Basic\",\n" +
            "               \"tokenIsSuppliedByQueryParam\":true,\n" +
            "               \"tokenQueryParamName\":\"token\",\n" +
            "               \"tokenIsSuppliedByHttpHeader\":false,\n" +
            "               \"tokenIsSuppliedByFormUrlEncoded\":false,\n" +
            "               \"tokenFormUrlEncodedName\":\"token\",\n" +
            "               \"userClaim\":\"sub\",\n" +
            "               \"clientId\":\"client-id\",\n" +
            "               \"clientSecret\":\"client-secret\"\n" +
            "            },\n" +
            "            \"enabled\":true\n" +
            "         }\n" +
            "      ],\n" +
            "      \"properties\":[\n" +
            "         {\n" +
            "            \"key\":\"client-id\",\n" +
            "            \"value\":\"abc\",\n" +
            "            \"encrypted\":false,\n" +
            "            \"dynamic\":false,\n" +
            "            \"encryptable\":false\n" +
            "         },\n" +
            "         {\n" +
            "            \"key\":\"client-secret\",\n" +
            "            \"value\":\"abc\",\n" +
            "            \"encrypted\":false,\n" +
            "            \"dynamic\":false,\n" +
            "            \"encryptable\":false\n" +
            "         },\n" +
            "         {\n" +
            "            \"key\":\"property-1\",\n" +
            "            \"value\":\"value-1\",\n" +
            "            \"encrypted\":false,\n" +
            "            \"dynamic\":false,\n" +
            "            \"encryptable\":false\n" +
            "         },\n" +
            "         {\n" +
            "            \"key\":\"property-2\",\n" +
            "            \"value\":\"EnqXzj3i27jDUZU8h6fIqg==\",\n" +
            "            \"encrypted\":true,\n" +
            "            \"dynamic\":false,\n" +
            "            \"encryptable\":false\n" +
            "         }\n" +
            "      ]\n" +
            "   },\n" +
            "   \"planEntities\":[\n" +
            "      {\n" +
            "         \"name\":\"Keyless\",\n" +
            "         \"description\":\"Keyless\",\n" +
            "         \"createdAt\":1689169972399,\n" +
            "         \"updatedAt\":1689169972399,\n" +
            "         \"publishedAt\":1689169972399,\n" +
            "         \"validation\":\"auto\",\n" +
            "         \"type\":\"api\",\n" +
            "         \"mode\":\"standard\",\n" +
            "         \"security\":{\n" +
            "            \"type\":\"KEY_LESS\"\n" +
            "         },\n" +
            "         \"flows\":[\n" +
            "            \n" +
            "         ],\n" +
            "         \"tags\":[\n" +
            "            \n" +
            "         ],\n" +
            "         \"status\":\"published\",\n" +
            "         \"order\":1,\n" +
            "         \"commentRequired\":false\n" +
            "      },\n" +
            "      {\n" +
            "         \"name\":\"Premium API Key Plan\",\n" +
            "         \"description\":\"\",\n" +
            "         \"createdAt\":1689169972379,\n" +
            "         \"updatedAt\":1689170527231,\n" +
            "         \"publishedAt\":1689169972379,\n" +
            "         \"validation\":\"auto\",\n" +
            "         \"type\":\"api\",\n" +
            "         \"mode\":\"standard\",\n" +
            "         \"security\":{\n" +
            "            \"type\":\"API_KEY\",\n" +
            "            \"configuration\":{\n" +
            "               \n" +
            "            }\n" +
            "         },\n" +
            "         \"flows\":[\n" +
            "            {\n" +
            "               \n" +
            "            }\n" +
            "         ],\n" +
            "         \"tags\":[\n" +
            "            \n" +
            "         ],\n" +
            "         \"status\":\"published\",\n" +
            "         \"order\":1,\n" +
            "         \"characteristics\":[\n" +
            "            \n" +
            "         ],\n" +
            "         \"excludedGroups\":[\n" +
            "            \n" +
            "         ],\n" +
            "         \"commentRequired\":false,\n" +
            "         \"commentMessage\":\"\",\n" +
            "         \"generalConditions\":\"\"\n" +
            "      },\n" +
            "      {\n" +
            "         \"name\":\"Keyless\",\n" +
            "         \"description\":\"Keyless\",\n" +
            "         \"createdAt\":1689169964283,\n" +
            "         \"updatedAt\":1689171419581,\n" +
            "         \"publishedAt\":1689169964307,\n" +
            "         \"closedAt\":1689171419581,\n" +
            "         \"validation\":\"auto\",\n" +
            "         \"type\":\"api\",\n" +
            "         \"mode\":\"standard\",\n" +
            "         \"security\":{\n" +
            "            \"type\":\"KEY_LESS\"\n" +
            "         },\n" +
            "         \"flows\":[\n" +
            "            \n" +
            "         ],\n" +
            "         \"tags\":[\n" +
            "            \n" +
            "         ],\n" +
            "         \"status\":\"closed\",\n" +
            "         \"order\":1,\n" +
            "         \"commentRequired\":false\n" +
            "      },\n" +
            "      {\n" +
            "         \"name\":\"Limit Creation of Tasks\",\n" +
            "         \"description\":\"\",\n" +
            "         \"createdAt\":1689169972429,\n" +
            "         \"updatedAt\":1689169972429,\n" +
            "         \"validation\":\"manual\",\n" +
            "         \"type\":\"api\",\n" +
            "         \"mode\":\"standard\",\n" +
            "         \"security\":{\n" +
            "            \"type\":\"API_KEY\",\n" +
            "            \"configuration\":{\n" +
            "               \"propagateApiKey\":false\n" +
            "            }\n" +
            "         },\n" +
            "         \"flows\":[\n" +
            "            {\n" +
            "               \n" +
            "            }\n" +
            "         ],\n" +
            "         \"tags\":[\n" +
            "            \n" +
            "         ],\n" +
            "         \"status\":\"closed\",\n" +
            "         \"order\":3,\n" +
            "         \"characteristics\":[\n" +
            "            \n" +
            "         ],\n" +
            "         \"excludedGroups\":[\n" +
            "            \n" +
            "         ],\n" +
            "         \"commentRequired\":false,\n" +
            "         \"commentMessage\":\"\",\n" +
            "         \"generalConditions\":\"\"\n" +
            "      },\n" +
            "      {\n" +
            "         \"name\":\"Limit Creation of Tasks\",\n" +
            "         \"description\":\"\",\n" +
            "         \"createdAt\":1689169972411,\n" +
            "         \"updatedAt\":1689169972411,\n" +
            "         \"publishedAt\":1689169972411,\n" +
            "         \"validation\":\"manual\",\n" +
            "         \"type\":\"api\",\n" +
            "         \"mode\":\"standard\",\n" +
            "         \"security\":{\n" +
            "            \"type\":\"API_KEY\",\n" +
            "            \"configuration\":{\n" +
            "               \n" +
            "            }\n" +
            "         },\n" +
            "         \"flows\":[\n" +
            "            {\n" +
            "               \n" +
            "            }\n" +
            "         ],\n" +
            "         \"tags\":[\n" +
            "            \n" +
            "         ],\n" +
            "         \"status\":\"published\",\n" +
            "         \"order\":2,\n" +
            "         \"characteristics\":[\n" +
            "            \n" +
            "         ],\n" +
            "         \"excludedGroups\":[\n" +
            "            \n" +
            "         ],\n" +
            "         \"commentRequired\":false,\n" +
            "         \"commentMessage\":\"\",\n" +
            "         \"generalConditions\":\"\"\n" +
            "      }\n" +
            "   ],\n" +
            "   \"metadata\":[\n" +
            "      {\n" +
            "         \"key\":\"test-2\",\n" +
            "         \"name\":\"test-2\",\n" +
            "         \"format\":\"STRING\",\n" +
            "         \"defaultValue\":\"value 2\"\n" +
            "      },\n" +
            "      {\n" +
            "         \"key\":\"email-support\",\n" +
            "         \"name\":\"email-support\",\n" +
            "         \"format\":\"MAIL\",\n" +
            "         \"value\":\"${(api.primaryOwner.email)!''}\",\n" +
            "         \"defaultValue\":\"support@change.me\"\n" +
            "      },\n" +
            "      {\n" +
            "         \"key\":\"test-1\",\n" +
            "         \"name\":\"test 1\",\n" +
            "         \"format\":\"STRING\",\n" +
            "         \"defaultValue\":\"value 1\"\n" +
            "      }\n" +
            "   ]\n" +
            "}"
        );
    }
}
