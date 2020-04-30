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

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.portal.rest.resource.param.AnalyticsParam;
import io.gravitee.rest.api.portal.rest.resource.param.AnalyticsTypeParam;
import org.junit.Test;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;

import static org.junit.Assert.*;

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
        
        testParams(params, "Query parameter 'type' must be present and one of : GROUP_BY, DATE_HISTO, COUNT");
        
        //init - type == ""
        params = new AnalyticsParam();
        params.setFrom(1);
        params.setTo(10);
        params.setInterval(10_000);
        params.setTypeParam(new AnalyticsTypeParam(null));
        
        testParams(params, "Query parameter 'type' is not valid");
    }
    
    @Test
    public void testValidateKoFrom() {
        AnalyticsParam params = new AnalyticsParam();
        params.setFrom(-1);
        params.setTo(10);
        params.setInterval(10_000);
        params.setTypeParam(new AnalyticsTypeParam("COUNT"));

        testParams(params, "Query parameter 'from' is not valid");
    }
    
    @Test
    public void testValidateKoTo() {
        AnalyticsParam params = new AnalyticsParam();
        params.setFrom(1);
        params.setTo(-1);
        params.setInterval(10_000);
        params.setTypeParam(new AnalyticsTypeParam("COUNT"));

        testParams(params, "Query parameter 'to' is not valid");
        
    }
    
    @Test
    public void testValidateKoFromAndTo() {
        AnalyticsParam params = new AnalyticsParam();
        params.setFrom(10);
        params.setTo(1);
        params.setInterval(10_000);
        params.setTypeParam(new AnalyticsTypeParam("COUNT"));
        testParams(params, "'from' query parameter value must not be greater than 'to'");
    }
    
    @Test
    public void testValidateKoInterval() {
        //interval = -1
        AnalyticsParam params = new AnalyticsParam();
        params.setFrom(1);
        params.setTo(10);
        params.setInterval(-1);
        params.setTypeParam(new AnalyticsTypeParam("DATE_HISTO"));
        
        testParams(params, "Query parameter 'interval' is not valid");
        
        //interval = 10
        params = new AnalyticsParam();
        params.setFrom(1);
        params.setTo(10);
        params.setInterval(10);
        params.setTypeParam(new AnalyticsTypeParam("DATE_HISTO"));
        
        testParams(params, "Query parameter 'interval' is not valid. 'interval' must be >= 1000 and <= 1000000000");
        
        //interval = 1_000_000_001
        params = new AnalyticsParam();
        params.setFrom(1);
        params.setTo(10);
        params.setInterval(1_000_000_001);
        params.setTypeParam(new AnalyticsTypeParam("DATE_HISTO"));
        
        testParams(params, "Query parameter 'interval' is not valid. 'interval' must be >= 1000 and <= 1000000000");
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
        testParams(params, "'field' query parameter is required for 'group_by' request");
        
        //field = ''
        params = new AnalyticsParam();
        params.setFrom(1);
        params.setTo(10);
        params.setInterval(10_000);
        params.setTypeParam(new AnalyticsTypeParam("GROUP_BY"));
        params.setField("");
        testParams(params, "'field' query parameter is required for 'group_by' request");
    }
    
    private void testParams(AnalyticsParam params, String expectedErrorMessage) {
        try {
            params.validate();
            assertFalse(true);
        } catch(BadRequestException e) {
            final Response response = e.getResponse();
            assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
            assertEquals(expectedErrorMessage, e.getMessage());
        }
    }
}
