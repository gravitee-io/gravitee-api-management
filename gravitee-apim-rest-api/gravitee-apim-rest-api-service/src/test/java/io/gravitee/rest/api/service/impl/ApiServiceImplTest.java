/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.data.domain.Page;
import io.gravitee.common.util.DataEncryptor;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Event;
import io.gravitee.rest.api.model.EventEntity;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.PropertyEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.EventService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.PrimaryOwnerNotFoundException;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.AfterProperty;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.security.GeneralSecurityException;
import java.util.*;

import static io.gravitee.rest.api.model.EventType.PUBLISH_API;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiServiceImplTest {

    @InjectMocks
    private ApiServiceImpl apiService;

    @Mock
    private DataEncryptor dataEncryptor;

    @Mock
    private MembershipService membershipService;

    @Mock
    private EventService eventService;

    @Mock
    private ApiRepository apiRepository;

    @Test
    public void encryptProperties_should_call_data_encryptor_for_each_encryptable_property_not_yet_encrypted()
            throws GeneralSecurityException {
        List<PropertyEntity> properties = buildProperties();

        apiService.encryptProperties(properties);

        verify(dataEncryptor, times(1)).encrypt("value2");
        verify(dataEncryptor, times(1)).encrypt("value4");
        verifyNoMoreInteractions(dataEncryptor);
    }

    @Test
    public void encryptProperties_should_set_encrypted_boolean_true_for_each_encrypted_property() throws GeneralSecurityException {
        List<PropertyEntity> properties = buildProperties();

        apiService.encryptProperties(properties);

        assertFalse(properties.get(0).isEncrypted());
        assertTrue(properties.get(1).isEncrypted());
        assertTrue(properties.get(2).isEncrypted());
        assertTrue(properties.get(3).isEncrypted());
    }

    @Test
    public void encryptProperties_should_set_value_of_each_encrypted_property() throws GeneralSecurityException {
        List<PropertyEntity> properties = buildProperties();
        when(dataEncryptor.encrypt("value2")).thenReturn("encryptedValue2");
        when(dataEncryptor.encrypt("value4")).thenReturn("encryptedValue4");

        apiService.encryptProperties(properties);

        assertEquals("value1", properties.get(0).getValue());
        assertEquals("encryptedValue2", properties.get(1).getValue());
        assertEquals("value3", properties.get(2).getValue());
        assertEquals("encryptedValue4", properties.get(3).getValue());
    }

    @Test(expected = PrimaryOwnerNotFoundException.class)
    public void getPrimaryOwner_should_throw_PrimaryOwnerNotFoundException_if_no_membership_found() {
        try {
            when(
                    membershipService.getPrimaryOwner(
                            GraviteeContext.getExecutionContext().getOrganizationId(),
                            MembershipReferenceType.API,
                            "my-api"
                    )
            )
                    .thenReturn(null);
            apiService.getPrimaryOwner(GraviteeContext.getExecutionContext(), "my-api");
        } catch (PrimaryOwnerNotFoundException e) {
            assertEquals("Primary owner not found for API [my-api]", e.getMessage());
            assertEquals("primaryOwner.notFound", e.getTechnicalCode());
            assertEquals(500, e.getHttpStatusCode());
            assertEquals(Map.of("api", "my-api"), e.getParameters());
            throw e;
        }
    }

    private AutoCloseable mockitoCloseable;

    @BeforeProperty
        //@BeforeTry
    void initMocks() {
        mockitoCloseable = MockitoAnnotations.openMocks(this);
    }

    @AfterProperty
        //@AfterTry
    void closeMocks() throws Exception {
        mockitoCloseable.close();
    }

    @Group
    @Label("GIVEN an API Entity to be deployed")
    class GivenTheApiToBeDeployed {
        @Group
        @Label("WHEN the synchronization comparison is invoked")
        class WhenCheckingForSynchronizationWithTheDeployedApi{


            @Property
            @Label("THEN the cross IDs of the deployed API and the API to be deployed should be excluded when the comparison is made")
            public boolean thenExcludeCrossIdsFromApiSynchronizationCheck(@ForAll("getPotentialCrossIds") String crossIdOfApiToBeDeployed) throws JsonProcessingException, TechnicalException {
                // Mock required services
                ExecutionContext executionContext = new ExecutionContext("DEFAULT", "DEFAULT");

                Map<String, Object> initialProperties = new HashMap<>();
                initialProperties.put(Event.EventProperties.API_ID.getValue(), "c38d779e-6e7e-472b-8d77-9e6e7e172b78");

                Map<String, String> newProperties = new HashMap<>();
                newProperties.put(Event.EventProperties.API_ID.getValue(), "c38d779e-6e7e-472b-8d77-9e6e7e172b78");
                newProperties.put(Event.EventProperties.DEPLOYMENT_NUMBER.getValue(), "c38d779e-6e7e-472b-8d77-9e6e7e172b78");
                newProperties.put(Event.EventProperties.USER.getValue(), "4c16223b-f7e2-407e-9622-3bf7e2f07e0a");
                newProperties.put(Event.EventProperties.ORIGIN.getValue(), "origin -> 192.168.0.21");

                EventEntity eventEntity = new EventEntity();
                eventEntity.setId("db770e80-2547-4d48-b70e-8025477d4880");
                eventEntity.setType(PUBLISH_API);
                eventEntity.setParentId(null);
                eventEntity.setProperties(newProperties);
                eventEntity.setPayload(
                        "{\n" +
                                "  \"id\" : \"c38d779e-6e7e-472b-8d77-9e6e7e172b78\",\n" +
                                "  \"crossId\" : \""+ crossIdOfApiToBeDeployed +"\",\n" +
                                "  \"environmentId\" : \"DEFAULT\",\n" +
                                "  \"name\" : \"test\",\n" +
                                "  \"description\" : \"test\",\n" +
                                "  \"version\" : \"1.0\",\n" +
                                "  \"definition\" : \"{\\n  \\\"id\\\" : \\\"c38d779e-6e7e-472b-8d77-9e6e7e172b78\\\",\\n  \\\"name\\\" : \\\"keyless\\\",\\n  \\\"version\\\" : \\\"1.0\\\",\\n  \\\"gravitee\\\" : \\\"2.0.0\\\",\\n  \\\"execution_mode\\\" : \\\"v3\\\",\\n  \\\"flow_mode\\\" : \\\"DEFAULT\\\",\\n  \\\"proxy\\\" : {\\n    \\\"virtual_hosts\\\" : [ {\\n      \\\"path\\\" : \\\"/keyless\\\"\\n    } ],\\n    \\\"strip_context_path\\\" : false,\\n    \\\"preserve_host\\\" : false,\\n    \\\"groups\\\" : [ {\\n      \\\"name\\\" : \\\"default-group\\\",\\n      \\\"endpoints\\\" : [ {\\n        \\\"inherit\\\" : true,\\n        \\\"name\\\" : \\\"default\\\",\\n        \\\"target\\\" : \\\"https://api.gravitee.io/echo\\\",\\n        \\\"weight\\\" : 1,\\n        \\\"backup\\\" : false,\\n        \\\"type\\\" : \\\"http\\\"\\n      } ],\\n      \\\"load_balancing\\\" : {\\n        \\\"type\\\" : \\\"ROUND_ROBIN\\\"\\n      },\\n      \\\"http\\\" : {\\n        \\\"connectTimeout\\\" : 5000,\\n        \\\"idleTimeout\\\" : 60000,\\n        \\\"keepAlive\\\" : true,\\n        \\\"readTimeout\\\" : 10000,\\n        \\\"pipelining\\\" : false,\\n        \\\"maxConcurrentConnections\\\" : 100,\\n        \\\"useCompression\\\" : true,\\n        \\\"followRedirects\\\" : false\\n      }\\n    } ]\\n  },\\n  \\\"plans\\\" : [ {\\n    \\\"id\\\" : \\\"0c9e3720-4e37-4314-9e37-204e375314ce\\\",\\n    \\\"name\\\" : \\\"keyless\\\",\\n    \\\"security\\\" : \\\"KEY_LESS\\\",\\n    \\\"paths\\\" : { },\\n    \\\"api\\\" : \\\"c38d779e-6e7e-472b-8d77-9e6e7e172b78\\\",\\n    \\\"flows\\\" : [ {\\n      \\\"id\\\" : \\\"53a4cca2-558b-4d70-a4cc-a2558b2d70d4\\\",\\n      \\\"name\\\" : \\\"\\\",\\n      \\\"path-operator\\\" : {\\n        \\\"path\\\" : \\\"/\\\",\\n        \\\"operator\\\" : \\\"STARTS_WITH\\\"\\n      },\\n      \\\"condition\\\" : \\\"\\\",\\n      \\\"consumers\\\" : [ ],\\n      \\\"methods\\\" : [ ],\\n      \\\"pre\\\" : [ ],\\n      \\\"post\\\" : [ ],\\n      \\\"enabled\\\" : true\\n    } ],\\n    \\\"status\\\" : \\\"PUBLISHED\\\"\\n  } ],\\n  \\\"properties\\\" : [ ]\\n}\",\n" +
                                "  \"deployedAt\" : 1688483780799,\n" +
                                "  \"createdAt\" : 1688483780696,\n" +
                                "  \"updatedAt\" : 1688483780799,\n" +
                                "  \"visibility\" : \"PRIVATE\",\n" +
                                "  \"lifecycleState\" : \"STOPPED\",\n" +
                                "  \"groups\" : [ ],\n" +
                                "  \"disableMembershipNotifications\" : false,\n" +
                                "  \"apiLifecycleState\" : \"CREATED\"\n" +
                                "}"
                );

                List<EventEntity> eventEntityList = new ArrayList<EventEntity>();
                eventEntityList.add(eventEntity);

                io.gravitee.common.data.domain.Page<EventEntity> mockedEvents = new io.gravitee.common.data.domain.Page<EventEntity>(eventEntityList, 0,1,1);

                Api deployedApi = new Api();
                deployedApi.setCrossId(crossIdOfApiToBeDeployed);
                deployedApi.setId("c38d779e-6e7e-472b-8d77-9e6e7e172b78");
                deployedApi.setEnvironmentId("DEFAULT");

                when(apiRepository.findById("c38d779e-6e7e-472b-8d77-9e6e7e172b78")).thenReturn(Optional.of(deployedApi));

                when(eventService.search(
                        executionContext,
                        Arrays.asList(PUBLISH_API, EventType.UNPUBLISH_API),
                        initialProperties,
                        0,
                        0,
                        0,
                        1,
                        singletonList(executionContext.getEnvironmentId())
                )).thenReturn(mockedEvents);

                return apiService.isSynchronized(executionContext, crossIdOfApiToBeDeployed);
            }
        }
    }

    @Provide
    Arbitrary<String> getPotentialCrossIds() {
        return Arbitraries.strings().ofLength(36).alpha().numeric().withChars("-").injectNull(0.05);
    }

    private List<PropertyEntity> buildProperties() {
        return List.of(
                new PropertyEntity("key1", "value1", false, false),
                new PropertyEntity("key2", "value2", true, false),
                new PropertyEntity("key3", "value3", true, true),
                new PropertyEntity("key4", "value4", true, false)
        );
    }

}
