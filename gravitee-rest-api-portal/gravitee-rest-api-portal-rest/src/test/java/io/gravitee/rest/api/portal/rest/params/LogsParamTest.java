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
package io.gravitee.rest.api.portal.rest.params;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.junit.Test;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.portal.rest.resource.param.LogsParam;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class LogsParamTest {

    @Test
    public void testValidateOk() {
        //init
        LogsParam params = new LogsParam();
        params.setFrom(1);
        params.setTo(10);
        params.setOrder("ASC");
        //test
        params.validate();
        assertTrue(true);
        
        //another test with 'DESC'
        params.setOrder("DESC");
        //test
        params.validate();
        assertTrue(true);
    }
    
    @Test
    public void testValidateKoFrom() {
        LogsParam params = new LogsParam();
        params.setFrom(-1);
        
        try {
            params.validate();
            assertFalse(true);
        } catch(WebApplicationException e) {
            final Response response = e.getResponse();
            assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
            assertEquals("Query parameter 'from' is not valid", response.getEntity());
        }
    }
    
    @Test
    public void testValidateKoTo() {
        LogsParam params = new LogsParam();
        params.setFrom(1);
        params.setTo(-1);

        try {
            params.validate();
            assertFalse(true);
        } catch(WebApplicationException e) {
            final Response response = e.getResponse();
            assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
            assertEquals("Query parameter 'to' is not valid", response.getEntity());
        }
    }
    
    @Test
    public void testValidateKoFromAndTo() {
        LogsParam params = new LogsParam();
        params.setFrom(10);
        params.setTo(1);
        try {
            params.validate();
            assertFalse(true);
        } catch(WebApplicationException e) {
            final Response response = e.getResponse();
            assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
            assertEquals("'from' query parameter value must be greater than 'to'", response.getEntity());
        }
    }
    
    @Test
    public void testValidateKoOrder() {
        LogsParam params = new LogsParam();
        params.setFrom(1);
        params.setTo(10);
        params.setOrder("");
        try {
            params.validate();
            assertFalse(true);
        } catch(WebApplicationException e) {
            final Response response = e.getResponse();
            assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
            assertEquals("'order' query parameter value must be 'ASC' or 'DESC'", response.getEntity());
        }
    }
}
