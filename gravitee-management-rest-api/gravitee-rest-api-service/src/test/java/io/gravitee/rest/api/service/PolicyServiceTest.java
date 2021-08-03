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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.fge.jsonschema.main.JsonSchemaFactory;
import io.gravitee.definition.model.Policy;
import io.gravitee.definition.model.flow.Step;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.core.api.PluginManifest;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.policy.api.annotations.OnRequest;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.rest.api.model.PolicyEntity;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import io.gravitee.rest.api.service.impl.PolicyServiceImpl;
import io.gravitee.rest.api.service.spring.ServiceConfiguration;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Azize Elamrani (azize dot elamrani at gmail dot com)
 */
@RunWith(MockitoJUnitRunner.class)
public class PolicyServiceTest {

    private static final String POLICY_ID = "myPolicy";

    @InjectMocks
    private PolicyService policyService = new PolicyServiceImpl();

    @Mock
    private ConfigurablePluginManager<PolicyPlugin> policyManager;

    @Mock
    private PolicyPlugin policyDefinition;

    private static final String JSON_SCHEMA =
        "{\n" +
        "  \"type\": \"object\",\n" +
        "  \"id\": \"urn:jsonschema:io:gravitee:policy:test\",\n" +
        "  \"properties\": {\n" +
        "    \"name\": {\n" +
        "      \"title\": \"Name\",\n" +
        "      \"type\": \"string\"\n" +
        "    },\n" +
        "    \"valid\": {\n" +
        "      \"title\": \"Valid\",\n" +
        "      \"type\": \"boolean\",\n" +
        "      \"default\": false\n" +
        "    }\n" +
        "  },\n" +
        "  \"required\": [\n" +
        "    \"name\"\n" +
        "  ]\n" +
        "}";

    private JsonSchemaFactory jsonSchemaFactory = new ServiceConfiguration().jsonSchemaFactory();

    class PolicyMock {

        @OnRequest
        public void onRequest() {}
    }

    @Before
    public void before() {
        // Manually set the jsonSchemaFactory.
        ReflectionTestUtils.setField(policyService, "jsonSchemaFactory", jsonSchemaFactory);
        when(policyDefinition.path()).thenReturn(mock(Path.class));
        when(policyDefinition.type()).thenReturn("");
        when(policyDefinition.policy()).thenReturn(PolicyMock.class);
    }

    //    @Mock
    //    private Plugin plugin;

    @Mock
    private PluginManifest manifest;

    @Test
    public void shouldFindAll() throws TechnicalException {
        when(policyDefinition.id()).thenReturn(POLICY_ID);
        when(policyManager.findAll()).thenReturn(Collections.singletonList(policyDefinition));
        when(policyDefinition.manifest()).thenReturn(manifest);
        //        when(plugin.manifest()).thenReturn(manifest);

        final Set<PolicyEntity> policies = policyService.findAll();

        assertNotNull(policies);
        assertEquals(POLICY_ID, policies.iterator().next().getId());
    }

    @Test(expected = InvalidDataException.class)
    public void shouldRejectInvalidJson() throws Exception {
        Policy policy = new Policy();
        policy.setName("my-policy");
        policy.setConfiguration("{ Invalid: \"test\", \"valid\": false }");

        policyService.validatePolicyConfiguration(policy);
    }

    @Test(expected = InvalidDataException.class)
    public void shouldRejectInvalidJsonStep() throws Exception {
        Step step = new Step();
        step.setPolicy("my-policy");
        step.setConfiguration("{ Invalid: \"test\", \"valid\": false }");

        policyService.validatePolicyConfiguration(step);
    }

    @Test
    public void shouldAcceptValidJsonConfigurationWithNoSchema() throws Exception {
        Policy policy = new Policy();
        policy.setName("my-policy");
        policy.setConfiguration("{ \"name\": \"test\", \"valid\": true }");

        when(policyManager.getSchema("my-policy")).thenReturn("");

        policyService.validatePolicyConfiguration(policy);
    }

    @Test
    public void shouldAcceptValidJsonConfigurationWithNoSchemaStep() throws Exception {
        Step step = new Step();
        step.setPolicy("my-policy");
        step.setConfiguration("{ \"name\": \"test\", \"valid\": true }");

        when(policyManager.getSchema("my-policy")).thenReturn("");

        policyService.validatePolicyConfiguration(step);
    }

    @Test
    public void shouldAcceptValidJsonConfigurationWithEmptySchema() throws Exception {
        Policy policy = new Policy();
        policy.setName("my-policy");
        policy.setConfiguration("{ \"name\": \"test\", \"valid\": true }");
        when(policyManager.getSchema("my-policy")).thenReturn("");

        policyService.validatePolicyConfiguration(policy);
    }

    @Test
    public void shouldAcceptNoConfiguration() {
        Policy policy = new Policy();
        policy.setName("my-policy");
        policy.setConfiguration(null);
        policyService.validatePolicyConfiguration(policy);
    }

    @Test
    public void shouldAcceptNoConfigurationStep() {
        Step step = new Step();
        step.setPolicy("my-policy");
        step.setConfiguration(null);
        policyService.validatePolicyConfiguration(step);
    }

    @Test
    public void shouldAcceptNullPolicy() {
        policyService.validatePolicyConfiguration((Policy) null);
    }

    @Test
    public void shouldAcceptNullStep() {
        policyService.validatePolicyConfiguration((Step) null);
    }

    @Test
    public void shouldAcceptValidJsonConfigurationWithSchema() throws Exception {
        Policy policy = new Policy();
        policy.setName("my-policy");
        policy.setConfiguration("{ \"name\": \"test\", \"valid\": true }");
        when(policyManager.getSchema("my-policy")).thenReturn(JSON_SCHEMA);

        policyService.validatePolicyConfiguration(policy);
    }

    @Test(expected = InvalidDataException.class)
    public void shouldRejectInvalidJsonConfigurationWithSchema() throws Exception {
        Policy policy = new Policy();
        policy.setName("my-policy");
        policy.setConfiguration("{ \"name\": true, \"valid\": true }"); // Name should be a String
        when(policyManager.getSchema("my-policy")).thenReturn(JSON_SCHEMA);

        policyService.validatePolicyConfiguration(policy);
    }

    @Test(expected = InvalidDataException.class)
    public void shouldRejectValidJsonConfigurationWithInvalidSchema() throws Exception {
        Policy policy = new Policy();
        policy.setName("my-policy");
        policy.setConfiguration("{ \"name\": \"test\", \"valid\": true }");
        when(policyManager.getSchema("my-policy")).thenReturn("InvalidSchema");

        policyService.validatePolicyConfiguration(policy);
    }

    @Test
    public void shouldAcceptValidJsonConfigurationWithCustomJavaRegexFormat() throws Exception {
        String schemaWithJavaRegexFormat = JSON_SCHEMA.replace(
            "\"type\": \"string\"\n",
            "\"type\": \"string\"\n,\"format\": \"java-regex\""
        );

        Policy policy = new Policy();
        policy.setName("my-policy");
        policy.setConfiguration("{ \"name\": \"^.*[A-Za-z]\\\\d*$\", \"valid\": true }");
        when(policyManager.getSchema("my-policy")).thenReturn(schemaWithJavaRegexFormat);

        policyService.validatePolicyConfiguration(policy);
    }

    @Test(expected = InvalidDataException.class)
    public void shouldRejectInvalidJsonConfigurationWithCustomJavaRegexFormat() throws Exception {
        try {
            String schemaWithJavaRegexFormat = JSON_SCHEMA.replace(
                "\"type\": \"string\"\n",
                "\"type\": \"string\"\n,\"format\": \"java-regex\""
            );

            Policy policy = new Policy();
            policy.setName("my-policy");
            policy.setConfiguration("{ \"name\": \"( INVALID regex\", \"valid\": true }");
            when(policyManager.getSchema("my-policy")).thenReturn(schemaWithJavaRegexFormat);

            policyService.validatePolicyConfiguration(policy);
        } catch (InvalidDataException e) {
            assertEquals("Invalid policy configuration : Invalid java regular expression [( INVALID regex]", e.getMessage());
            throw e;
        }
    }

    @Test
    public void shouldAcceptValidJssonConfiguration_defaultValue() throws Exception {
        final String JSON_SCHEMA =
            "{\n" +
            "  \"type\": \"object\",\n" +
            "  \"id\": \"urn:jsonschema:io:gravitee:policy:test\",\n" +
            "  \"properties\": {\n" +
            "    \"name\": {\n" +
            "      \"title\": \"Name\",\n" +
            "      \"type\": \"string\",\n" +
            "      \"default\": \"test\"\n" +
            "    },\n" +
            "    \"valid\": {\n" +
            "      \"title\": \"Valid\",\n" +
            "      \"type\": \"boolean\",\n" +
            "      \"default\": false\n" +
            "    }\n" +
            "  },\n" +
            "  \"required\": [\n" +
            "    \"name\"\n" +
            "  ]\n" +
            "}";

        Policy policy = new Policy();
        policy.setName("my-policy");
        policy.setConfiguration("{ \"valid\": true }");
        when(policyManager.getSchema("my-policy")).thenReturn(JSON_SCHEMA);

        policyService.validatePolicyConfiguration(policy);
    }
}
