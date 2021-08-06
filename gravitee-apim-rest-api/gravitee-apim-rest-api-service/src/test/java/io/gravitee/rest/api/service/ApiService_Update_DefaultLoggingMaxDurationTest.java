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
package io.gravitee.rest.api.service;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.PropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.*;
import io.gravitee.definition.model.endpoint.HttpEndpoint;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.ApiLifecycleState;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.api.UpdateApiEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.impl.ApiServiceImpl;
import io.gravitee.rest.api.service.jackson.filter.ApiPermissionFilter;
import io.gravitee.rest.api.service.search.SearchEngineService;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Function;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(ApiServiceImpl.class)
@PowerMockIgnore("javax.security.*")
public class ApiService_Update_DefaultLoggingMaxDurationTest {

    private static final String API_ID = "id-api";
    private static final String API_NAME = "myAPI";
    private static final String USER_NAME = "myUser";

    @InjectMocks
    private ApiServiceImpl apiService = new ApiServiceImpl();

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private MembershipService membershipService;

    @Mock
    private RoleService roleService;

    @Spy
    private ObjectMapper objectMapper = new GraviteeMapper();

    @Mock
    private Api api;

    @Mock
    private UserService userService;

    @Mock
    private AuditService auditService;

    @Mock
    private SearchEngineService searchEngineService;

    @Mock
    private ParameterService parameterService;

    @Mock
    private CategoryService categoryService;

    private UpdateApiEntity existingApi;

    @Mock
    private VirtualHostService virtualHostService;

    @Mock
    private GroupService groupService;

    @Mock
    private ApiMetadataService apiMetadataService;

    @AfterClass
    public static void cleanSecurityContextHolder() {
        // reset authentication to avoid side effect during test executions.
        SecurityContextHolder.setContext(
            new SecurityContext() {
                @Override
                public Authentication getAuthentication() {
                    return null;
                }

                @Override
                public void setAuthentication(Authentication authentication) {}
            }
        );
    }

    @Before
    public void setUp() throws TechnicalException {
        PropertyFilter apiMembershipTypeFilter = new ApiPermissionFilter();
        objectMapper.setFilterProvider(
            new SimpleFilterProvider(Collections.singletonMap("apiMembershipTypeFilter", apiMembershipTypeFilter))
        );
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        when(apiRepository.update(any())).thenReturn(api);
        when(api.getName()).thenReturn(API_NAME);
        when(api.getApiLifecycleState()).thenReturn(ApiLifecycleState.CREATED);

        existingApi = new UpdateApiEntity();
        existingApi.setName(API_NAME);
        existingApi.setVersion("v1");
        existingApi.setDescription("Ma description");
        existingApi.setLifecycleState(io.gravitee.rest.api.model.api.ApiLifecycleState.CREATED);
        final Proxy proxy = new Proxy();
        EndpointGroup endpointGroup = new EndpointGroup();
        Endpoint endpoint = new HttpEndpoint(null, null);
        endpointGroup.setEndpoints(singleton(endpoint));
        proxy.setGroups(singleton(endpointGroup));
        existingApi.setProxy(proxy);
        proxy.setVirtualHosts(Collections.singletonList(new VirtualHost("/context")));

        RoleEntity poRoleEntity = new RoleEntity();
        poRoleEntity.setName(SystemRole.PRIMARY_OWNER.name());
        poRoleEntity.setScope(RoleScope.API);
        when(roleService.findPrimaryOwnerRoleByOrganization("DEFAULT", RoleScope.API)).thenReturn(poRoleEntity);
        MemberEntity po = new MemberEntity();
        po.setId(USER_NAME);
        po.setReferenceId(API_ID);
        po.setReferenceType(io.gravitee.rest.api.model.MembershipReferenceType.API);
        po.setRoles(Collections.singletonList(poRoleEntity));
        when(membershipService.getMembersByReferencesAndRole(any(), any(), any())).thenReturn(singleton(po));

        mockStatic(System.class);
        when(System.currentTimeMillis()).thenReturn(0L);

        final SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(mock(Authentication.class));
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    public void shouldNotAddDefaultConditionIfNoLogging() throws TechnicalException {
        apiService.update(API_ID, existingApi);

        verify(apiRepository, times(1))
            .update(
                argThat(
                    api -> {
                        ObjectMapper objectMapper = new ObjectMapper();
                        try {
                            JsonNode json = objectMapper.readTree(api.getDefinition());
                            JsonNode proxy = json.get("proxy");
                            JsonNode logging = proxy.get("logging");

                            return logging == null;
                        } catch (IOException e) {
                            e.printStackTrace();
                            return false;
                        }
                    }
                )
            );
    }

    @Test
    public void shouldNotAddDefaultConditionIfNoneLogging() throws TechnicalException {
        Logging logging = new Logging();
        logging.setMode(LoggingMode.NONE);
        logging.setCondition("wrong");
        existingApi.getProxy().setLogging(logging);
        when(parameterService.findAll(eq(Key.LOGGING_DEFAULT_MAX_DURATION), any(Function.class), eq(ParameterReferenceType.ORGANIZATION)))
            .thenReturn(singletonList(1L));

        apiService.update(API_ID, existingApi);

        verify(apiRepository, times(1))
            .update(
                argThat(
                    api -> {
                        ObjectMapper objectMapper = new ObjectMapper();
                        try {
                            JsonNode json = objectMapper.readTree(api.getDefinition());
                            JsonNode proxy = json.get("proxy");
                            JsonNode logging1 = proxy.get("logging");
                            JsonNode mode = logging1.get("mode");
                            JsonNode condition = logging1.get("condition");

                            return "NONE".equals(mode.asText()) && condition == null;
                        } catch (IOException e) {
                            e.printStackTrace();
                            return false;
                        }
                    }
                )
            );
    }

    @Test
    public void shouldNotAddDefaultConditionIfWrongConditionButNoSettings() throws TechnicalException {
        Logging logging = new Logging();
        logging.setMode(LoggingMode.CLIENT_PROXY);
        logging.setCondition("true");
        existingApi.getProxy().setLogging(logging);
        when(parameterService.findAll(eq(Key.LOGGING_DEFAULT_MAX_DURATION), any(Function.class), eq(ParameterReferenceType.ORGANIZATION)))
            .thenReturn(singletonList(0L));

        apiService.update(API_ID, existingApi);

        verify(apiRepository, times(1))
            .update(
                argThat(
                    api -> {
                        ObjectMapper objectMapper = new ObjectMapper();
                        try {
                            JsonNode json = objectMapper.readTree(api.getDefinition());
                            JsonNode proxy = json.get("proxy");
                            JsonNode logging1 = proxy.get("logging");
                            JsonNode mode = logging1.get("mode");
                            JsonNode condition = logging1.get("condition");

                            return "CLIENT_PROXY".equals(mode.asText()) && "true".equals(condition.asText());
                        } catch (IOException e) {
                            e.printStackTrace();
                            return false;
                        }
                    }
                )
            );
    }

    @Test
    public void shouldAddDefaultConditionIfWrongConditionWithSettings() throws TechnicalException {
        Logging logging = new Logging();
        logging.setMode(LoggingMode.CLIENT_PROXY);
        logging.setCondition("true");
        existingApi.getProxy().setLogging(logging);
        when(parameterService.findAll(eq(Key.LOGGING_DEFAULT_MAX_DURATION), any(Function.class), eq(ParameterReferenceType.ORGANIZATION)))
            .thenReturn(singletonList(1L));

        apiService.update(API_ID, existingApi);

        verify(apiRepository, times(1))
            .update(
                argThat(
                    api -> {
                        ObjectMapper objectMapper = new ObjectMapper();
                        try {
                            JsonNode json = objectMapper.readTree(api.getDefinition());
                            JsonNode proxy = json.get("proxy");
                            JsonNode logging1 = proxy.get("logging");
                            JsonNode mode = logging1.get("mode");
                            JsonNode condition = logging1.get("condition");

                            return "CLIENT_PROXY".equals(mode.asText()) && "#request.timestamp <= 1l && true".equals(condition.asText());
                        } catch (IOException e) {
                            e.printStackTrace();
                            return false;
                        }
                    }
                )
            );
    }

    @Test
    public void shouldOverrideTimestampCaseTimestampLessOrEqual() throws TechnicalException {
        Logging logging = new Logging();
        logging.setMode(LoggingMode.CLIENT_PROXY);
        logging.setCondition("#request.timestamp <= 2550166583090l");
        existingApi.getProxy().setLogging(logging);
        when(parameterService.findAll(eq(Key.LOGGING_DEFAULT_MAX_DURATION), any(Function.class), eq(ParameterReferenceType.ORGANIZATION)))
            .thenReturn(singletonList(1L));

        apiService.update(API_ID, existingApi);

        verify(apiRepository, times(1))
            .update(
                argThat(
                    api -> {
                        ObjectMapper objectMapper = new ObjectMapper();
                        try {
                            JsonNode json = objectMapper.readTree(api.getDefinition());
                            JsonNode proxy = json.get("proxy");
                            JsonNode logging1 = proxy.get("logging");
                            JsonNode mode = logging1.get("mode");
                            JsonNode condition = logging1.get("condition");

                            return "CLIENT_PROXY".equals(mode.asText()) && "#request.timestamp <= 1l".equals(condition.asText());
                        } catch (IOException e) {
                            e.printStackTrace();
                            return false;
                        }
                    }
                )
            );
    }

    @Test
    public void shouldOverrideTimestampCaseTimestampLess() throws TechnicalException {
        Logging logging = new Logging();
        logging.setMode(LoggingMode.CLIENT_PROXY);
        logging.setCondition("#request.timestamp < 2550166583090l");
        existingApi.getProxy().setLogging(logging);
        when(parameterService.findAll(eq(Key.LOGGING_DEFAULT_MAX_DURATION), any(Function.class), eq(ParameterReferenceType.ORGANIZATION)))
            .thenReturn(singletonList(1L));

        apiService.update(API_ID, existingApi);

        verify(apiRepository, times(1))
            .update(
                argThat(
                    api -> {
                        ObjectMapper objectMapper = new ObjectMapper();
                        try {
                            JsonNode json = objectMapper.readTree(api.getDefinition());
                            JsonNode proxy = json.get("proxy");
                            JsonNode logging1 = proxy.get("logging");
                            JsonNode mode = logging1.get("mode");
                            JsonNode condition = logging1.get("condition");

                            return "CLIENT_PROXY".equals(mode.asText()) && "#request.timestamp <= 1l".equals(condition.asText());
                        } catch (IOException e) {
                            e.printStackTrace();
                            return false;
                        }
                    }
                )
            );
    }

    @Test
    public void shouldOverrideTimestampCaseTimestampAndAfter() throws TechnicalException {
        Logging logging = new Logging();
        logging.setMode(LoggingMode.CLIENT_PROXY);
        logging.setCondition("#request.timestamp <= 2550166583090l && #context.plan == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2");
        existingApi.getProxy().setLogging(logging);
        when(parameterService.findAll(eq(Key.LOGGING_DEFAULT_MAX_DURATION), any(Function.class), eq(ParameterReferenceType.ORGANIZATION)))
            .thenReturn(singletonList(1L));

        apiService.update(API_ID, existingApi);

        verify(apiRepository, times(1))
            .update(
                argThat(
                    api -> {
                        ObjectMapper objectMapper = new ObjectMapper();
                        try {
                            JsonNode json = objectMapper.readTree(api.getDefinition());
                            JsonNode proxy = json.get("proxy");
                            JsonNode logging1 = proxy.get("logging");
                            JsonNode mode = logging1.get("mode");
                            JsonNode condition = logging1.get("condition");

                            return (
                                "CLIENT_PROXY".equals(mode.asText()) &&
                                "#request.timestamp <= 1l && (#context.plan == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2)".equals(
                                        condition.asText()
                                    )
                            );
                        } catch (IOException e) {
                            e.printStackTrace();
                            return false;
                        }
                    }
                )
            );
    }

    @Test
    public void shouldOverrideTimestampCaseBeforeAndTimestamp() throws TechnicalException {
        Logging logging = new Logging();
        logging.setMode(LoggingMode.CLIENT_PROXY);
        logging.setCondition("#context.application == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2' && #request.timestamp <= 2550166583090l");
        existingApi.getProxy().setLogging(logging);
        when(parameterService.findAll(eq(Key.LOGGING_DEFAULT_MAX_DURATION), any(Function.class), eq(ParameterReferenceType.ORGANIZATION)))
            .thenReturn(singletonList(1L));

        apiService.update(API_ID, existingApi);

        verify(apiRepository, times(1))
            .update(
                argThat(
                    api -> {
                        ObjectMapper objectMapper = new ObjectMapper();
                        try {
                            JsonNode json = objectMapper.readTree(api.getDefinition());
                            JsonNode proxy = json.get("proxy");
                            JsonNode logging1 = proxy.get("logging");
                            JsonNode mode = logging1.get("mode");
                            JsonNode condition = logging1.get("condition");

                            return (
                                "CLIENT_PROXY".equals(mode.asText()) &&
                                "(#context.application == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2') && #request.timestamp <= 1l".equals(
                                        condition.asText()
                                    )
                            );
                        } catch (IOException e) {
                            e.printStackTrace();
                            return false;
                        }
                    }
                )
            );
    }

    @Test
    public void shouldHandleAfter_doubleParenthesis() throws TechnicalException {
        Logging logging = new Logging();
        logging.setMode(LoggingMode.CLIENT_PROXY);
        logging.setCondition(
            "#context.application == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2' && #request.timestamp <= 2550166583090l && (#context.plan == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2')"
        );
        existingApi.getProxy().setLogging(logging);
        when(parameterService.findAll(eq(Key.LOGGING_DEFAULT_MAX_DURATION), any(Function.class), eq(ParameterReferenceType.ORGANIZATION)))
            .thenReturn(singletonList(1L));

        apiService.update(API_ID, existingApi);

        verify(apiRepository, times(1))
            .update(
                argThat(
                    api -> {
                        ObjectMapper objectMapper = new ObjectMapper();
                        try {
                            JsonNode json = objectMapper.readTree(api.getDefinition());
                            JsonNode proxy = json.get("proxy");
                            JsonNode logging1 = proxy.get("logging");
                            JsonNode mode = logging1.get("mode");
                            JsonNode condition = logging1.get("condition");

                            return (
                                "CLIENT_PROXY".equals(mode.asText()) &&
                                "(#context.application == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2') && #request.timestamp <= 1l && (#context.plan == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2')".equals(
                                        condition.asText()
                                    )
                            );
                        } catch (IOException e) {
                            e.printStackTrace();
                            return false;
                        }
                    }
                )
            );
    }

    @Test
    public void shouldHandleBefore_doubleParenthesis() throws TechnicalException {
        Logging logging = new Logging();
        logging.setMode(LoggingMode.CLIENT_PROXY);
        logging.setCondition(
            "(#context.application == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2') && #request.timestamp <= 2550166583090l && (#context.plan == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2')"
        );
        existingApi.getProxy().setLogging(logging);
        when(parameterService.findAll(eq(Key.LOGGING_DEFAULT_MAX_DURATION), any(Function.class), eq(ParameterReferenceType.ORGANIZATION)))
            .thenReturn(singletonList(1L));

        apiService.update(API_ID, existingApi);

        verify(apiRepository, times(1))
            .update(
                argThat(
                    api -> {
                        ObjectMapper objectMapper = new ObjectMapper();
                        try {
                            JsonNode json = objectMapper.readTree(api.getDefinition());
                            JsonNode proxy = json.get("proxy");
                            JsonNode logging1 = proxy.get("logging");
                            JsonNode mode = logging1.get("mode");
                            JsonNode condition = logging1.get("condition");

                            return (
                                "CLIENT_PROXY".equals(mode.asText()) &&
                                "(#context.application == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2') && #request.timestamp <= 1l && (#context.plan == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2')".equals(
                                        condition.asText()
                                    )
                            );
                        } catch (IOException e) {
                            e.printStackTrace();
                            return false;
                        }
                    }
                )
            );
    }

    @Test
    public void shouldHandleBefore_multipleParenthesis() throws TechnicalException {
        Logging logging = new Logging();
        logging.setMode(LoggingMode.CLIENT_PROXY);
        logging.setCondition(
            "((#context.application == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2')) && #request.timestamp <= 2550166583090l && (#context.plan == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2')"
        );
        existingApi.getProxy().setLogging(logging);
        when(parameterService.findAll(eq(Key.LOGGING_DEFAULT_MAX_DURATION), any(Function.class), eq(ParameterReferenceType.ORGANIZATION)))
            .thenReturn(singletonList(1L));

        apiService.update(API_ID, existingApi);

        verify(apiRepository, times(1))
            .update(
                argThat(
                    api -> {
                        ObjectMapper objectMapper = new ObjectMapper();
                        try {
                            JsonNode json = objectMapper.readTree(api.getDefinition());
                            JsonNode proxy = json.get("proxy");
                            JsonNode logging1 = proxy.get("logging");
                            JsonNode mode = logging1.get("mode");
                            JsonNode condition = logging1.get("condition");

                            return (
                                "CLIENT_PROXY".equals(mode.asText()) &&
                                "((#context.application == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2')) && #request.timestamp <= 1l && (#context.plan == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2')".equals(
                                        condition.asText()
                                    )
                            );
                        } catch (IOException e) {
                            e.printStackTrace();
                            return false;
                        }
                    }
                )
            );
    }

    @Test
    public void shouldOverrideTimestampCaseBeforeAndTimestampAndAfter() throws TechnicalException {
        Logging logging = new Logging();
        logging.setMode(LoggingMode.CLIENT_PROXY);
        logging.setCondition(
            "#context.application == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2' && #request.timestamp <= 2550166583090l && #context.plan == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2'"
        );
        existingApi.getProxy().setLogging(logging);
        when(parameterService.findAll(eq(Key.LOGGING_DEFAULT_MAX_DURATION), any(Function.class), eq(ParameterReferenceType.ORGANIZATION)))
            .thenReturn(singletonList(1L));

        apiService.update(API_ID, existingApi);

        verify(apiRepository, times(1))
            .update(
                argThat(
                    api -> {
                        ObjectMapper objectMapper = new ObjectMapper();
                        try {
                            JsonNode json = objectMapper.readTree(api.getDefinition());
                            JsonNode proxy = json.get("proxy");
                            JsonNode logging1 = proxy.get("logging");
                            JsonNode mode = logging1.get("mode");
                            JsonNode condition = logging1.get("condition");

                            return (
                                "CLIENT_PROXY".equals(mode.asText()) &&
                                "(#context.application == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2') && #request.timestamp <= 1l && (#context.plan == '5aada00c-cd25-41f0-ada0-0ccd25b1f0f2')".equals(
                                        condition.asText()
                                    )
                            );
                        } catch (IOException e) {
                            e.printStackTrace();
                            return false;
                        }
                    }
                )
            );
    }

    @Test
    public void shouldNotOverrideTimestampIfBeforeThreshold() throws TechnicalException {
        Logging logging = new Logging();
        logging.setMode(LoggingMode.CLIENT_PROXY);
        logging.setCondition("#request.timestamp <= 2l");
        existingApi.getProxy().setLogging(logging);
        when(parameterService.findAll(eq(Key.LOGGING_DEFAULT_MAX_DURATION), any(Function.class), eq(ParameterReferenceType.ORGANIZATION)))
            .thenReturn(singletonList(3L));

        apiService.update(API_ID, existingApi);

        verify(apiRepository, times(1))
            .update(
                argThat(
                    api -> {
                        ObjectMapper objectMapper = new ObjectMapper();
                        try {
                            JsonNode json = objectMapper.readTree(api.getDefinition());
                            JsonNode proxy = json.get("proxy");
                            JsonNode logging1 = proxy.get("logging");
                            JsonNode mode = logging1.get("mode");
                            JsonNode condition = logging1.get("condition");

                            return "CLIENT_PROXY".equals(mode.asText()) && "#request.timestamp <= 2l".equals(condition.asText());
                        } catch (IOException e) {
                            e.printStackTrace();
                            return false;
                        }
                    }
                )
            );
    }

    @Test
    public void shouldOverrideTimestampCaseGreaterOrEquals() throws TechnicalException {
        Logging logging = new Logging();
        logging.setMode(LoggingMode.CLIENT_PROXY);
        logging.setCondition("#request.timestamp >= 5l");
        existingApi.getProxy().setLogging(logging);
        when(parameterService.findAll(eq(Key.LOGGING_DEFAULT_MAX_DURATION), any(Function.class), eq(ParameterReferenceType.ORGANIZATION)))
            .thenReturn(singletonList(1L));

        apiService.update(API_ID, existingApi);

        verify(apiRepository, times(1))
            .update(
                argThat(
                    api -> {
                        ObjectMapper objectMapper = new ObjectMapper();
                        try {
                            JsonNode json = objectMapper.readTree(api.getDefinition());
                            JsonNode proxy = json.get("proxy");
                            JsonNode logging1 = proxy.get("logging");
                            JsonNode mode = logging1.get("mode");
                            JsonNode condition = logging1.get("condition");

                            return (
                                "CLIENT_PROXY".equals(mode.asText()) &&
                                "#request.timestamp <= 1l && #request.timestamp >= 5l".equals(condition.asText())
                            );
                        } catch (IOException e) {
                            e.printStackTrace();
                            return false;
                        }
                    }
                )
            );
    }

    @Test
    public void shouldOverrideTimestampCaseGreater() throws TechnicalException {
        Logging logging = new Logging();
        logging.setMode(LoggingMode.CLIENT_PROXY);
        logging.setCondition("#request.timestamp > 5l");
        existingApi.getProxy().setLogging(logging);
        when(parameterService.findAll(eq(Key.LOGGING_DEFAULT_MAX_DURATION), any(Function.class), eq(ParameterReferenceType.ORGANIZATION)))
            .thenReturn(singletonList(1L));

        apiService.update(API_ID, existingApi);

        verify(apiRepository, times(1))
            .update(
                argThat(
                    api -> {
                        ObjectMapper objectMapper = new ObjectMapper();
                        try {
                            JsonNode json = objectMapper.readTree(api.getDefinition());
                            JsonNode proxy = json.get("proxy");
                            JsonNode logging1 = proxy.get("logging");
                            JsonNode mode = logging1.get("mode");
                            JsonNode condition = logging1.get("condition");

                            return (
                                "CLIENT_PROXY".equals(mode.asText()) &&
                                "#request.timestamp <= 1l && #request.timestamp > 5l".equals(condition.asText())
                            );
                        } catch (IOException e) {
                            e.printStackTrace();
                            return false;
                        }
                    }
                )
            );
    }

    @Test
    public void shouldOverrideTimestampCaseGreaterOrEqualsInThePast() throws TechnicalException {
        Logging logging = new Logging();
        logging.setMode(LoggingMode.CLIENT_PROXY);
        logging.setCondition("#request.timestamp >= 0l");
        existingApi.getProxy().setLogging(logging);
        when(parameterService.findAll(eq(Key.LOGGING_DEFAULT_MAX_DURATION), any(Function.class), eq(ParameterReferenceType.ORGANIZATION)))
            .thenReturn(singletonList(1L));

        apiService.update(API_ID, existingApi);

        verify(apiRepository, times(1))
            .update(
                argThat(
                    api -> {
                        ObjectMapper objectMapper = new ObjectMapper();
                        try {
                            JsonNode json = objectMapper.readTree(api.getDefinition());
                            JsonNode proxy = json.get("proxy");
                            JsonNode logging1 = proxy.get("logging");
                            JsonNode mode = logging1.get("mode");
                            JsonNode condition = logging1.get("condition");

                            return (
                                "CLIENT_PROXY".equals(mode.asText()) &&
                                "#request.timestamp <= 1l && #request.timestamp >= 0l".equals(condition.asText())
                            );
                        } catch (IOException e) {
                            e.printStackTrace();
                            return false;
                        }
                    }
                )
            );
    }

    @Test
    public void shouldOverrideTimestampCaseGreaterOrEqualsInThePastWithOrCondition() throws TechnicalException {
        final Logging logging = new Logging();
        logging.setMode(LoggingMode.CLIENT_PROXY);
        existingApi.getProxy().setLogging(logging);
        when(parameterService.findAll(eq(Key.LOGGING_DEFAULT_MAX_DURATION), any(Function.class), eq(ParameterReferenceType.ORGANIZATION)))
            .thenReturn(singletonList(1L));

        checkCondition(logging, "true || #request.timestamp <= 2l", "(true) && #request.timestamp <= 1l");
        checkCondition(logging, "#request.timestamp <= 2l || true", "#request.timestamp <= 1l && (true)");
        checkCondition(
            logging,
            "#request.timestamp <= 2l || #request.timestamp >= 1l",
            "#request.timestamp <= 1l && (#request.timestamp >= 1l)"
        );
        checkCondition(
            logging,
            "#request.timestamp <= 1234l  || #request.timestamp > 2l",
            "#request.timestamp <= 1l && (#request.timestamp > 2l)"
        );
        checkCondition(logging, "#request.timestamp <= 1l || true", "#request.timestamp <= 1l && (true)");
        checkCondition(logging, "#request.timestamp <= 0l", "#request.timestamp <= 0l");
    }

    private void checkCondition(final Logging logging, final String condition, final String expectedCondition) throws TechnicalException {
        reset(apiRepository);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        when(apiRepository.update(any())).thenReturn(api);

        logging.setCondition(condition);
        apiService.update(API_ID, existingApi);
        verify(apiRepository, times(1))
            .update(
                argThat(
                    api -> {
                        ObjectMapper objectMapper = new ObjectMapper();
                        try {
                            JsonNode json = objectMapper.readTree(api.getDefinition());
                            JsonNode proxy = json.get("proxy");
                            JsonNode logging1 = proxy.get("logging");
                            JsonNode mode = logging1.get("mode");
                            JsonNode cond = logging1.get("condition");
                            return "CLIENT_PROXY".equals(mode.asText()) && expectedCondition.equals(cond.asText());
                        } catch (IOException e) {
                            e.printStackTrace();
                            return false;
                        }
                    }
                )
            );
    }
}
