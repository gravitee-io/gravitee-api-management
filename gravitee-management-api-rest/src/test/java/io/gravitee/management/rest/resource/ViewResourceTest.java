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
package io.gravitee.management.rest.resource;

import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.management.model.UpdateViewEntity;
import io.gravitee.management.model.ViewEntity;
import io.gravitee.management.model.api.ApiEntity;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;

import static io.gravitee.common.http.HttpStatusCode.*;
import static javax.ws.rs.client.Entity.entity;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * @author Eric LELEU (eric dot leleu at graviteesource dot com)
 */
public class ViewResourceTest extends AbstractResourceTest {

    private static final String VIEW = "my-view";
    private static final String UNKNOWN_API = "unknown";

    protected String contextPath() {
        return "configuration/views/";
    }

    private ViewEntity mockView;
    private UpdateViewEntity updateViewEntity;

    @Before
    public void init() {
        mockView = new ViewEntity();
        mockView.setId(VIEW);
        mockView.setName(VIEW);
        mockView.setUpdatedAt(new Date());
        doReturn(mockView).when(viewService).findById(VIEW);

        updateViewEntity = new UpdateViewEntity();
        updateViewEntity.setDescription("toto");
        updateViewEntity.setName(VIEW);

        doReturn(mockView).when(viewService).update(eq(VIEW), any());
    }


    @Test
    public void shouldUpdateApi_ImageWithUpperCaseType_issue4086() throws IOException {
        InputStream inputStream = this.getClass().getResourceAsStream("/images/4086_jpeg.b64");
        String picture = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        updateViewEntity.setPicture(picture);
        final Response response = target(VIEW).request().put(Entity.json(updateViewEntity));

        assertEquals(response.readEntity(String.class), OK_200, response.getStatus());
    }

}
