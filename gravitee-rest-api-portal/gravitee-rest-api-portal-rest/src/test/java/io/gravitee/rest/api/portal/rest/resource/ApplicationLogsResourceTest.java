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
package io.gravitee.rest.api.portal.rest.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.analytics.query.LogQuery;
import io.gravitee.rest.api.model.log.ApplicationRequest;
import io.gravitee.rest.api.model.log.ApplicationRequestItem;
import io.gravitee.rest.api.model.log.SearchLogResponse;
import io.gravitee.rest.api.portal.rest.model.DatasResponse;
import io.gravitee.rest.api.portal.rest.model.Links;
import io.gravitee.rest.api.portal.rest.model.Log;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApplicationLogsResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "applications/";
    }
    
    private static final String APPLICATION = "my-application";
    private static final String LOG = "my-log";

    private Map<String, Map<String, String>> metadata;
    private SearchLogResponse<ApplicationRequestItem> searchResponse;
    
    @Before
    public void init() {
        resetAllMocks();
        
        ApplicationRequestItem appLogItem1 = new ApplicationRequestItem();
        appLogItem1.setId("A");
        ApplicationRequestItem appLogItem2 = new ApplicationRequestItem();
        appLogItem2.setId("B");
        searchResponse = new SearchLogResponse<>(2);
        searchResponse.setLogs(Arrays.asList(appLogItem1, appLogItem2));
        
        metadata = new HashMap<String, Map<String,String>>();
        HashMap<String, String> appMetadata = new HashMap<String, String>();
        appMetadata.put(APPLICATION, APPLICATION);
        metadata.put(APPLICATION, appMetadata);
        HashMap<String, String> listMetadata = new HashMap<String, String>();
        listMetadata.put(AbstractResource.METADATA_LIST_TOTAL_KEY, "2");
        metadata.put(AbstractResource.METADATA_LIST_KEY, listMetadata);
        searchResponse.setMetadata(metadata);
        
        doReturn(searchResponse).when(logsService).findByApplication(eq(APPLICATION), any());
        
        doReturn(new Log()).when(logMapper).convert(any(ApplicationRequestItem.class));
        doReturn(new Log()).when(logMapper).convert(any(ApplicationRequest.class));
    }
    
    @Test
    public void shouldGetLogs() {
        
        final Response response = target(APPLICATION).path("logs")
                .queryParam("page", 1)
                .queryParam("size", 10)
                .queryParam("query", APPLICATION)
                .queryParam("from", 0)
                .queryParam("to", 100)
                .queryParam("field", APPLICATION)
                .queryParam("order", "ASC")
                .request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        
        ArgumentCaptor<LogQuery> logQueryCaptor = ArgumentCaptor.forClass(LogQuery.class);
        Mockito.verify(logsService).findByApplication(eq(APPLICATION), logQueryCaptor.capture());
        final LogQuery logQuery = logQueryCaptor.getValue();
        assertEquals(APPLICATION, logQuery.getField());
        assertEquals(0, logQuery.getFrom());
        assertEquals(1, logQuery.getPage());
        assertEquals(APPLICATION, logQuery.getQuery());
        assertEquals(10, logQuery.getSize());
        assertEquals(100, logQuery.getTo());
        assertTrue(logQuery.isOrder());
        
        DatasResponse logsResponse = response.readEntity(DatasResponse.class);
        assertEquals(2, logsResponse.getData().size());
        assertEquals(metadata, logsResponse.getMetadata());
        Links links = logsResponse.getLinks();
        assertNotNull(links);
    }


    @Test
    public void shouldGetNoLogAndNoLink() {
        SearchLogResponse<ApplicationRequestItem> emptySearchResponse = new SearchLogResponse<>(0);
        emptySearchResponse.setLogs(Collections.EMPTY_LIST);
        doReturn(emptySearchResponse).when(logsService).findByApplication(eq(APPLICATION), any());
        
        final Response response = target(APPLICATION).path("logs")
                .queryParam("page", 1)
                .queryParam("size", 10)
                .queryParam("query", APPLICATION)
                .queryParam("from", 0)
                .queryParam("to", 100)
                .queryParam("field", APPLICATION)
                .queryParam("order", "ASC")
                .request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        
        DatasResponse logsResponse = response.readEntity(DatasResponse.class);
        assertEquals(0, logsResponse.getData().size());
        
        Links links = logsResponse.getLinks();
        assertNull(links);
        
    }
    
    @Test
    public void shouldGetLog() {
        final ApplicationRequest toBeReturned = new ApplicationRequest();
        toBeReturned.setId(LOG);
        doReturn(toBeReturned).when(logsService).findApplicationLog(eq(LOG), any());
        
        final Response response = target(APPLICATION).path("logs").path(LOG).queryParam("timestamp", 1).request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        
        Mockito.verify(logMapper).convert(toBeReturned);
        Mockito.verify(logsService).findApplicationLog(LOG, 1L);
        
        Log Log = response.readEntity(Log.class);
        assertNotNull(Log);
    }
    
    @Test
    public void shouldExportLogs() {
        doReturn("EXPORT").when(logsService).exportAsCsv(any());
        final Response response = target(APPLICATION).path("logs").path("_export")
                .queryParam("page", 1)
                .queryParam("size", 10)
                .queryParam("query", APPLICATION)
                .queryParam("from", 0)
                .queryParam("to", 100)
                .queryParam("field", APPLICATION)
                .queryParam("order", "DESC")
                .request().post(null);
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        
        Mockito.verify(logsService).exportAsCsv(searchResponse);
        
        String exportString = response.readEntity(String.class);
        assertEquals("EXPORT", exportString);
        final MultivaluedMap<String, Object> headers = response.getHeaders();
        assertTrue(((String)headers.getFirst(HttpHeaders.CONTENT_DISPOSITION)).startsWith("attachment;filename=logs-"+APPLICATION));

    }
    
}
