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
    private static final String PAGE = "pageId";
    private static final String USER = "userId";
    private static final String CATEGORY = "categoryId";

    private static final String BASE_URL = "http://foo.bar";

    private static final String BASE_URL_APIS = BASE_URL + "/environments/DEFAULT" + "/apis";
    private static final String BASE_URL_APIS_APIID = BASE_URL_APIS + "/" + API;
    private static final String BASE_URL_APIS_APIID_PAGES = BASE_URL_APIS_APIID + "/pages";
    private static final String BASE_URL_APIS_APIID_PAGES_PAGEID = BASE_URL_APIS_APIID_PAGES + "/" + PAGE;

    private static final String BASE_URL_APPLICATIONS = BASE_URL + "/environments/DEFAULT" + "/applications";
    private static final String BASE_URL_APPLICATIONS_APPLICATIONID = BASE_URL_APPLICATIONS + "/" + APPLICATION;

    private static final String BASE_URL_PAGES = BASE_URL + "/environments/DEFAULT" + "/pages";
    private static final String BASE_URL_PAGES_PAGEID = BASE_URL_PAGES + "/" + PAGE;

    private static final String BASE_URL_USER = BASE_URL + "/environments/DEFAULT" + "/user";

    private static final String BASE_URL_USERS = BASE_URL + "/environments/DEFAULT" + "/users";
    private static final String BASE_URL_USERS_USERID = BASE_URL_USERS + "/" + USER;

    private static final String BASE_URL_CATEGORIES = BASE_URL + "/environments/DEFAULT" + "/categories";
    private static final String BASE_URL_CATEGORIES_CATEGORYID = BASE_URL_CATEGORIES + "/" + CATEGORY;

    @Test
    public void testApisLink() {
        String apis = PortalApiLinkHelper.apisURL(UriBuilder.fromPath(BASE_URL));
        String api = PortalApiLinkHelper.apisURL(UriBuilder.fromPath(BASE_URL), API);
        String apiPages = PortalApiLinkHelper.apiPagesURL(UriBuilder.fromPath(BASE_URL), API);
        String apiPage = PortalApiLinkHelper.apiPagesURL(UriBuilder.fromPath(BASE_URL), API, PAGE);
        assertEquals(BASE_URL_APIS, apis);
        assertEquals(BASE_URL_APIS_APIID, api);
        assertEquals(BASE_URL_APIS_APIID_PAGES, apiPages);
        assertEquals(BASE_URL_APIS_APIID_PAGES_PAGEID, apiPage);
    }

    @Test
    public void testApplicationsLink() {
        String applications = PortalApiLinkHelper.applicationsURL(UriBuilder.fromPath(BASE_URL));
        String application = PortalApiLinkHelper.applicationsURL(UriBuilder.fromPath(BASE_URL), APPLICATION);
        assertEquals(BASE_URL_APPLICATIONS, applications);
        assertEquals(BASE_URL_APPLICATIONS_APPLICATIONID, application);
    }

    @Test
    public void testPagesLink() {
        String pages = PortalApiLinkHelper.pagesURL(UriBuilder.fromPath(BASE_URL));
        String page = PortalApiLinkHelper.pagesURL(UriBuilder.fromPath(BASE_URL), PAGE);
        assertEquals(BASE_URL_PAGES, pages);
        assertEquals(BASE_URL_PAGES_PAGEID, page);
    }

    @Test
    public void testUserLink() {
        String user = PortalApiLinkHelper.userURL(UriBuilder.fromPath(BASE_URL));
        assertEquals(BASE_URL_USER, user);
    }

    @Test
    public void testUsersLink() {
        String users = PortalApiLinkHelper.usersURL(UriBuilder.fromPath(BASE_URL));
        String user = PortalApiLinkHelper.usersURL(UriBuilder.fromPath(BASE_URL), USER);
        assertEquals(BASE_URL_USERS, users);
        assertEquals(BASE_URL_USERS_USERID, user);
    }

    @Test
    public void testCategoriesLink() {
        String categories = PortalApiLinkHelper.categoriesURL(UriBuilder.fromPath(BASE_URL));
        String category = PortalApiLinkHelper.categoriesURL(UriBuilder.fromPath(BASE_URL), CATEGORY);
        assertEquals(BASE_URL_CATEGORIES, categories);
        assertEquals(BASE_URL_CATEGORIES_CATEGORYID, category);
    }
}
