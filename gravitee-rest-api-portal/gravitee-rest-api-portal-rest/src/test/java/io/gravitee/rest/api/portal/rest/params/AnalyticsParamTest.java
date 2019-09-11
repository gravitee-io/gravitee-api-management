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
import io.gravitee.rest.api.portal.rest.resource.param.AnalyticsParam;
import io.gravitee.rest.api.portal.rest.resource.param.AnalyticsTypeParam;
import io.gravitee.rest.api.portal.rest.resource.param.LogsParam;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AnalyticsParamTest {

    @Test
    public void testValidateOk() {
        //init
        AnalyticsParam params = new AnalyticsParam();
        params.setFrom(1);
        params.setTo(10);
        params.setInterval(10_000);
        params.setTypeParam(new AnalyticsTypeParam("GROUP_BY"));
        params.setField("name");
        
        //test
        params.validate();
        assertTrue(true);
        
    }
    
    @Test
    public void testValidateKoType() {
        //init - type == null
        AnalyticsParam params = new AnalyticsParam();
        params.setFrom(1);
        params.setTo(10);
        params.setInterval(10_000);
        params.setTypeParam(null);
        
        try {
            params.validate();
            assertFalse(true);
        } catch(WebApplicationException e) {
            final Response response = e.getResponse();
            assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
            assertEquals("Query parameter 'type' must be present and one of : GROUP_BY, DATE_HISTO, COUNT", response.getEntity());
        }
        
        //init - type == ""
        params = new AnalyticsParam();
        params.setFrom(1);
        params.setTo(10);
        params.setInterval(10_000);
        params.setTypeParam(new AnalyticsTypeParam(null));
        
        try {
            params.validate();
            assertFalse(true);
        } catch(WebApplicationException e) {
            final Response response = e.getResponse();
            assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
            assertEquals("Query parameter 'type' is not valid", response.getEntity());
        }
    }
    
    @Test
    public void testValidateKoFrom() {
        AnalyticsParam params = new AnalyticsParam();
        params.setFrom(-1);
        params.setTo(10);
        params.setInterval(10_000);
        params.setTypeParam(new AnalyticsTypeParam("COUNT"));

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
        AnalyticsParam params = new AnalyticsParam();
        params.setFrom(1);
        params.setTo(-1);
        params.setInterval(10_000);
        params.setTypeParam(new AnalyticsTypeParam("COUNT"));

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
        AnalyticsParam params = new AnalyticsParam();
        params.setFrom(10);
        params.setTo(1);
        params.setInterval(10_000);
        params.setTypeParam(new AnalyticsTypeParam("COUNT"));
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
    public void testValidateKoInterval() {
        //interval = -1
        AnalyticsParam params = new AnalyticsParam();
        params.setFrom(1);
        params.setTo(10);
        params.setInterval(-1);
        params.setTypeParam(new AnalyticsTypeParam("COUNT"));
        
        try {
            params.validate();
            assertFalse(true);
        } catch(WebApplicationException e) {
            final Response response = e.getResponse();
            assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
            assertEquals("Query parameter 'interval' is not valid", response.getEntity());
        }
        
        //interval = 10
        params = new AnalyticsParam();
        params.setFrom(1);
        params.setTo(10);
        params.setInterval(10);
        params.setTypeParam(new AnalyticsTypeParam("COUNT"));
        
        try {
            params.validate();
            assertFalse(true);
        } catch(WebApplicationException e) {
            final Response response = e.getResponse();
            assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
            assertEquals("Query parameter 'interval' is not valid. 'interval' must be >= 1000 and <= 1000000000", response.getEntity());
        }
        
        //interval = 1_000_000_001
        params = new AnalyticsParam();
        params.setFrom(1);
        params.setTo(10);
        params.setInterval(1_000_000_001);
        params.setTypeParam(new AnalyticsTypeParam("COUNT"));
        
        try {
            params.validate();
            assertFalse(true);
        } catch(WebApplicationException e) {
            final Response response = e.getResponse();
            assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
            assertEquals("Query parameter 'interval' is not valid. 'interval' must be >= 1000 and <= 1000000000", response.getEntity());
        }
    }
    
    @Test
    public void testValidateKoFieldIfTypeGroupBy() {
        //field = null
        AnalyticsParam params = new AnalyticsParam();
        params.setFrom(1);
        params.setTo(10);
        params.setInterval(10_000);
        params.setTypeParam(new AnalyticsTypeParam("GROUP_BY"));
        params.setField(null);
        try {
            params.validate();
            assertFalse(true);
        } catch(WebApplicationException e) {
            final Response response = e.getResponse();
            assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
            assertEquals("'field' query parameter is required for 'group_by' request", response.getEntity());
        }
        
        //field = ''
        params = new AnalyticsParam();
        params.setFrom(1);
        params.setTo(10);
        params.setInterval(10_000);
        params.setTypeParam(new AnalyticsTypeParam("GROUP_BY"));
        params.setField("");
        try {
            params.validate();
            assertFalse(true);
        } catch(WebApplicationException e) {
            final Response response = e.getResponse();
            assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
            assertEquals("'field' query parameter is required for 'group_by' request", response.getEntity());
        }
    }
}
