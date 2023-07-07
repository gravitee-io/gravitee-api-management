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
package io.gravitee.rest.api.management.v2.rest.utils;

import static org.junit.Assert.assertEquals;

import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.UriBuilder;
import java.util.Date;
import org.junit.Before;
import org.junit.Test;

public class ManagementApiLinkHelperTest {

    private static final String API_ID = "apidId";
    private static final Date UPDATED_AT = new Date();
    private static final String BASE_URL = "http://example.com";
    private static final String BASE_URL_APIS = BASE_URL + "/environments/DEFAULT" + "/apis";
    private static final String BASE_URL_APIS_APIID = BASE_URL_APIS + "/" + API_ID;
    private static final String BASE_URL_APIS_APIID_PICTURE = BASE_URL_APIS_APIID + "/picture?hash=" + UPDATED_AT.getTime();
    private static final String BASE_URL_APIS_APIID_BACKGROUND = BASE_URL_APIS_APIID + "/background?hash=" + UPDATED_AT.getTime();

    @Before
    public void setUp() {
        GraviteeContext.cleanContext();
    }

    @Test
    public void testApisLink() {
        ApiEntity api = new ApiEntity();
        api.setId(API_ID);
        api.setUpdatedAt(UPDATED_AT);

        String apiPictureURL = ManagementApiLinkHelper.apiPictureURL(UriBuilder.fromPath(BASE_URL), api);
        String apiBackgroundURL = ManagementApiLinkHelper.apiBackgroundURL(UriBuilder.fromPath(BASE_URL), api);
        assertEquals(BASE_URL_APIS_APIID_PICTURE, apiPictureURL);
        assertEquals(BASE_URL_APIS_APIID_BACKGROUND, apiBackgroundURL);
    }
}
