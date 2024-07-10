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
package io.gravitee.rest.api.service.cockpit.command.handler;

import static io.gravitee.rest.api.service.cockpit.command.handler.GoodbyeCommandHandler.DELETED_STATUS;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import io.gravitee.cockpit.api.command.Command;
import io.gravitee.cockpit.api.command.CommandStatus;
import io.gravitee.cockpit.api.command.goodbye.GoodbyeCommand;
import io.gravitee.cockpit.api.command.goodbye.GoodbyeReply;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.InstallationEntity;
import io.gravitee.rest.api.model.NewApplicationEntity;
import io.gravitee.rest.api.model.promotion.PromotionEntity;
import io.gravitee.rest.api.model.promotion.PromotionEntityAuthor;
import io.gravitee.rest.api.model.promotion.PromotionEntityStatus;
import io.gravitee.rest.api.service.InstallationService;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.promotion.PromotionService;
import io.reactivex.rxjava3.observers.TestObserver;
import java.util.HashMap;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class GoodbyeCommandHandlerTest {

    private static final String CUSTOM_VALUE = "customValue";
    private static final String CUSTOM_KEY = "customKey";
    private static final String INSTALLATION_ID = "installation#1";

    @Mock
    private InstallationService installationService;

    @Mock
    private PromotionService promotionService;

    private GoodbyeCommandHandler cut;

    @Before
    public void before() {
        cut = new GoodbyeCommandHandler(installationService, promotionService);
    }

    @Test
    public void handleType() {
        assertEquals(Command.Type.GOODBYE_COMMAND, cut.handleType());
    }

    @Test
    public void handle() throws InterruptedException {
        final InstallationEntity installation = new InstallationEntity();
        installation.setId(INSTALLATION_ID);
        installation.getAdditionalInformation().put(CUSTOM_KEY, CUSTOM_VALUE);

        GoodbyeCommand command = new GoodbyeCommand();

        when(installationService.getOrInitialize()).thenReturn(installation);
        when(promotionService.search(any(), any(), any())).thenReturn(new Page<>(emptyList(), 0, 0, 0));

        TestObserver<GoodbyeReply> obs = cut.handle(command).test();

        obs.await();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.SUCCEEDED));

        final HashMap<String, String> expectedAdditionalInfos = new HashMap<>();
        expectedAdditionalInfos.put(CUSTOM_KEY, CUSTOM_VALUE);
        expectedAdditionalInfos.put(InstallationService.COCKPIT_INSTALLATION_STATUS, DELETED_STATUS);
        verify(installationService, times(1)).setAdditionalInformation(expectedAdditionalInfos);
    }

    @Test
    public void handleWithException() throws InterruptedException {
        final InstallationEntity installation = new InstallationEntity();
        installation.setId(INSTALLATION_ID);
        installation.getAdditionalInformation().put(CUSTOM_KEY, CUSTOM_VALUE);

        GoodbyeCommand command = new GoodbyeCommand();

        when(installationService.getOrInitialize()).thenReturn(installation);
        when(installationService.setAdditionalInformation(anyMap())).thenThrow(new TechnicalManagementException());
        when(promotionService.search(any(), any(), any())).thenReturn(new Page<>(emptyList(), 0, 0, 0));

        TestObserver<GoodbyeReply> obs = cut.handle(command).test();

        obs.await();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.ERROR));
    }

    @Test
    public void handleRejectAllPromotionToValidate() throws InterruptedException {
        final InstallationEntity installation = new InstallationEntity();
        installation.setId(INSTALLATION_ID);
        installation.getAdditionalInformation().put(CUSTOM_KEY, CUSTOM_VALUE);

        GoodbyeCommand command = new GoodbyeCommand();

        when(installationService.getOrInitialize()).thenReturn(installation);

        PromotionEntity promotionEntity1 = getAPromotionEntity("promotion#1");
        PromotionEntity promotionEntity2 = getAPromotionEntity("promotion#2");
        when(promotionService.search(any(), any(), any())).thenReturn(new Page<>(List.of(promotionEntity1, promotionEntity2), 0, 0, 0));

        TestObserver<GoodbyeReply> obs = cut.handle(command).test();

        obs.await();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.SUCCEEDED));

        ArgumentCaptor<PromotionEntity> captor = ArgumentCaptor.forClass(PromotionEntity.class);

        verify(promotionService, times(2)).createOrUpdate(captor.capture());

        assertThat(captor.getAllValues())
            .extracting(PromotionEntity::getId, PromotionEntity::getStatus)
            .containsExactlyInAnyOrder(
                tuple(promotionEntity1.getId(), PromotionEntityStatus.REJECTED),
                tuple(promotionEntity2.getId(), PromotionEntityStatus.REJECTED)
            );
    }

    private PromotionEntity getAPromotionEntity(String id) {
        final PromotionEntity promotion = new PromotionEntity();
        promotion.setApiDefinition(
            "{\"id\" : \"api#1\",\"name\" : \"API Name\",\"version\" : \"1\",\"proxy\" : {  \"context_path\" : \"/product\",  \"endpoint\" : \"http://toto.com\",  \"endpoints\" : [ {    \"target\" : \"http://toto.com\",    \"weight\" : 1,    \"name\" : \"endpointName\"  } ],  \"strip_context_path\" : false,  \"http\" : {    \"configuration\" : {      \"connectTimeout\" : 5000,      \"idleTimeout\" : 60000,      \"keepAliveTimeout\" : 30000,      \"keepAlive\" : true,      \"dumpRequest\" : false    }  }},\"paths\" : {  \"/\" : [ {    \"methods\" : [ ],    \"api-key\" : {}  } ]},\"tags\" : [ ]\n}"
        );
        promotion.setTargetEnvCockpitId("env#1-cockpit-id");
        promotion.setTargetEnvName("Target Env");
        promotion.setSourceEnvCockpitId("env#2-cockpit-id");
        promotion.setSourceEnvName("Source Env");
        promotion.setApiId("api id");
        promotion.setTargetApiId("target api id");
        promotion.setStatus(PromotionEntityStatus.TO_BE_VALIDATED);
        promotion.setId(id);

        PromotionEntityAuthor author = new PromotionEntityAuthor();
        author.setDisplayName("Author");
        author.setEmail("author@gv.io");
        author.setPicture("https://picture.png");
        promotion.setAuthor(author);
        return promotion;
    }
}
