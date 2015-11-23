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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Response;

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.management.model.ApiEntity;
import io.gravitee.management.service.ApiService;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ApisResourceTest extends AbstractResourceTest {

    @Autowired
    private ApiService apiService;

    protected String contextPath() {
        return "apis";
    }

    @Test
    @Ignore
    public void testPostApi_nullApi() {
        final Response response = target().request().post(null);
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }
}
