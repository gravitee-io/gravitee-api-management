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
import io.gravitee.rest.api.portal.rest.resource.param.LogsParam;
import org.junit.Test;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;

import static org.junit.Assert.*;

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
        
        testParams(params, "Query parameter 'from' is not valid");
    }
    
    @Test
    public void testValidateKoTo() {
        LogsParam params = new LogsParam();
        params.setFrom(1);
        params.setTo(-1);

        testParams(params, "Query parameter 'to' is not valid");
        
    }
    
    @Test
    public void testValidateKoFromAndTo() {
        LogsParam params = new LogsParam();
        params.setFrom(10);
        params.setTo(1);
        testParams(params, "'from' query parameter value must not be greater than 'to'");
    }
    
    @Test
    public void testValidateKoOrder() {
        LogsParam params = new LogsParam();
        params.setFrom(1);
        params.setTo(10);
        params.setOrder("");
        testParams(params, "'order' query parameter value must be 'ASC' or 'DESC'");
    }
    
    private void testParams(LogsParam params, String expectedErrorMessage) {
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
