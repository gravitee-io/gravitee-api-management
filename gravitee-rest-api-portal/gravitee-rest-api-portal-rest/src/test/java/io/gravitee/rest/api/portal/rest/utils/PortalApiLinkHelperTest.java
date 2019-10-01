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
package io.gravitee.rest.api.portal.rest.utils;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.core.UriBuilder;

import org.junit.Test;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PortalApiLinkHelperTest {

    private static final String API = "apidId";
    private static final String APPLICATION = "applicationId";
    private static final String VIEW = "viewId";
    
    
    private static final String BASE_URL = "http://foo.bar";
    
    private static final String BASE_URL_APIS = BASE_URL + "/DEFAULT" + "/apis";
    private static final String BASE_URL_APIS_APIID = BASE_URL_APIS + "/" + API;
    
    private static final String BASE_URL_APPLICATIONS = BASE_URL + "/DEFAULT" + "/applications";
    private static final String BASE_URL_APPLICATIONS_APPLICATIONID = BASE_URL_APPLICATIONS + "/" + APPLICATION;
    
    private static final String BASE_URL_USER = BASE_URL + "/DEFAULT" + "/user";
    
    private static final String BASE_URL_VIEWS = BASE_URL + "/DEFAULT" + "/views";
    private static final String BASE_URL_VIEWS_VIEWID = BASE_URL_VIEWS + "/" + VIEW;
    
    @Test
    public void testApisLink() {
        String apis = PortalApiLinkHelper.apisURL(UriBuilder.fromPath(BASE_URL));
        String api = PortalApiLinkHelper.apisURL(UriBuilder.fromPath(BASE_URL), API);
        assertEquals(BASE_URL_APIS, apis);
        assertEquals(BASE_URL_APIS_APIID, api);
    }
    
    @Test
    public void testApplicationsLink() {
        String applications = PortalApiLinkHelper.applicationsURL(UriBuilder.fromPath(BASE_URL));
        String application = PortalApiLinkHelper.applicationsURL(UriBuilder.fromPath(BASE_URL), APPLICATION);
        assertEquals(BASE_URL_APPLICATIONS, applications);
        assertEquals(BASE_URL_APPLICATIONS_APPLICATIONID, application);
    }

    @Test
    public void testUserLink() {
        String user = PortalApiLinkHelper.userURL(UriBuilder.fromPath(BASE_URL));
        assertEquals(BASE_URL_USER, user);
    }

    @Test
    public void testViewsLink() {
        String views = PortalApiLinkHelper.viewsURL(UriBuilder.fromPath(BASE_URL));
        String view = PortalApiLinkHelper.viewsURL(UriBuilder.fromPath(BASE_URL), VIEW);
        assertEquals(BASE_URL_VIEWS, views);
        assertEquals(BASE_URL_VIEWS_VIEWID, view);
    }
}
