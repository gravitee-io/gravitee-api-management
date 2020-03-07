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
package io.gravitee.management.service;

import io.gravitee.management.service.impl.SwaggerServiceImpl;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
@Ignore
public class SwaggerService_CreateAPITest {

    private SwaggerService swaggerService = new SwaggerServiceImpl();

    /*
    // Swagger v1
    @Test
    public void shouldPrepareAPIFromSwaggerV1_URL_json() throws IOException {
        validate(prepareUrl("io/gravitee/management/service/swagger-v1.json"));
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV1_Inline_json() throws IOException {
        validate(prepareInline("io/gravitee/management/service/swagger-v1.json"));
    }

    // Swagger v2
    @Test
    public void shouldPrepareAPIFromSwaggerV2_URL_json() throws IOException {
        validate(prepareUrl("io/gravitee/management/service/swagger-v2.json"));
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV2_Inline_json() throws IOException {
        validate(prepareInline("io/gravitee/management/service/swagger-v2.json"));
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV2_URL_yaml() throws IOException {
        validate(prepareUrl("io/gravitee/management/service/swagger-v2.yaml"));
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV2_Inline_yaml() throws IOException {
        validate(prepareInline("io/gravitee/management/service/swagger-v2.yaml"));
    }

    // OpenAPI
    @Test
    public void shouldPrepareAPIFromSwaggerV3_URL_json() throws IOException {
        validate(prepareUrl("io/gravitee/management/service/openapi.json"));
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV3_Inline_json() throws IOException {
        validate(prepareInline("io/gravitee/management/service/openapi.json"));
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV3_URL_yaml() throws IOException {
        validate(prepareUrl("io/gravitee/management/service/openapi.yaml"));
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV3_Inline_yaml() throws IOException {
        validate(prepareInline("io/gravitee/management/service/openapi.yaml"));
    }

    private void validate(SwaggerApiEntity api) {
        assertEquals("1.2.3", api.getVersion());
        assertEquals("Gravitee.io Swagger API", api.getName());
        assertEquals("https://demo.gravitee.io/gateway/echo", api.getEndpoint().get(0));
        assertEquals(2, api.getPaths().size());
        assertTrue(api.getPaths().stream().filter(swaggerPath -> swaggerPath.getVerbs() == null)
                .map(SwaggerPath::getPath).collect(toList()).containsAll(asList("/pets", "/pets/:petId")));
    }

    private SwaggerApiEntity prepareInline(String file) throws IOException {
        return prepareInline(file, false);
    }

    private SwaggerApiEntity prepareInline(String file, boolean withMocks) throws IOException {
        URL url = Resources.getResource(file);
        String descriptor = Resources.toString(url, Charsets.UTF_8);
        ImportSwaggerDescriptorEntity swaggerDescriptor = new ImportSwaggerDescriptorEntity();
        swaggerDescriptor.setType(ImportSwaggerDescriptorEntity.Type.INLINE);
        swaggerDescriptor.setPayload(descriptor);
        swaggerDescriptor.setWithPolicyMocks(withMocks);

        return swaggerService.createAPI(swaggerDescriptor);
    }

    private SwaggerApiEntity prepareUrl(String file) {
        URL url = Resources.getResource(file);
        ImportSwaggerDescriptorEntity swaggerDescriptor = new ImportSwaggerDescriptorEntity();
        swaggerDescriptor.setType(ImportSwaggerDescriptorEntity.Type.URL);
        try {
            swaggerDescriptor.setPayload(url.toURI().getPath());
        } catch (URISyntaxException e) {
            fail(e.getMessage());
        }

        return swaggerService.createAPI(swaggerDescriptor);
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV3WithExamples() throws IOException {
        final SwaggerApiEntity api = prepareInline("io/gravitee/management/service/mock/api-with-examples.yaml", true);
        assertEquals("2.0.0", api.getVersion());
        assertEquals("Simple API overview", api.getName());
        assertEquals("simpleapioverview", api.getContextPath());
        assertEquals("/", api.getEndpoint().get(0));
        assertEquals(2, api.getPaths().size());
        assertTrue(api.getPaths().stream().map(SwaggerPath::getPath).collect(toList()).containsAll(asList("/", "/v2")));
        final List<SwaggerVerb> verbs = api.getPaths().iterator().next().getVerbs();
        assertNotNull(verbs);
        assertEquals(1, verbs.size());
        final SwaggerVerb listVersionsv2 = verbs.iterator().next();
        assertEquals("List API versions", listVersionsv2.getDescription());
        assertEquals("200", listVersionsv2.getResponseStatus());
        assertEquals("application/json", listVersionsv2.getContentType());
        assertEquals("GET", listVersionsv2.getVerb());
        assertNotNull(listVersionsv2.getResponseProperties());
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV3WithSimpleTypedExamples() throws IOException {
        final SwaggerApiEntity api = prepareInline("io/gravitee/management/service/mock/callback-example.yaml", true);
        assertEquals("1.0.0", api.getVersion());
        assertEquals("Callback Example", api.getName());
        assertEquals("callbackexample", api.getContextPath());
        assertEquals("/", api.getEndpoint().get(0));
        assertEquals(1, api.getPaths().size());
        assertTrue(api.getPaths().stream().map(SwaggerPath::getPath).collect(toList()).contains("/streams"));
        final List<SwaggerVerb> verbs = api.getPaths().iterator().next().getVerbs();
        assertNotNull(verbs);
        assertEquals(1, verbs.size());
        final SwaggerVerb postStreams = verbs.iterator().next();
        assertEquals("subscribes a client to receive out-of-band data", postStreams.getDescription());
        assertEquals("201", postStreams.getResponseStatus());
        assertEquals("application/json", postStreams.getContentType());
        assertEquals("POST", postStreams.getVerb());
        assertNotNull(postStreams.getResponseProperties());
        assertEquals("2531329f-fb09-4ef7-887e-84e648214436", ((Map) postStreams.getResponseProperties()).get("subscriptionId"));
        assertEquals("Mocked string", ((Map) postStreams.getResponseProperties()).get("message"));
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV3WithLinks() throws IOException {
        final SwaggerApiEntity api = prepareInline("io/gravitee/management/service/mock/link-example.yaml", true);
        assertEquals("1.0.0", api.getVersion());
        assertEquals("Link Example", api.getName());
        assertEquals("linkexample", api.getContextPath());
        assertEquals("/", api.getEndpoint().get(0));
        assertEquals(6, api.getPaths().size());
        final SwaggerPath usersUsername = api.getPaths().get(0);
        assertEquals("/2.0/users/:username", usersUsername.getPath());
        final SwaggerPath repositoriesUsername = api.getPaths().get(1);
        assertEquals("/2.0/repositories/:username", repositoriesUsername.getPath());
        assertEquals("/2.0/repositories/:username/:slug", api.getPaths().get(2).getPath());
        assertEquals("/2.0/repositories/:username/:slug/pullrequests", api.getPaths().get(3).getPath());
        assertEquals("/2.0/repositories/:username/:slug/pullrequests/:pid", api.getPaths().get(4).getPath());
        assertEquals("/2.0/repositories/:username/:slug/pullrequests/:pid/merge", api.getPaths().get(5).getPath());

        final List<SwaggerVerb> userUsernameVerbs = usersUsername.getVerbs();
        assertNotNull(userUsernameVerbs);
        assertEquals(1, userUsernameVerbs.size());
        final SwaggerVerb getUserByName = userUsernameVerbs.iterator().next();
        assertEquals("getUserByName", getUserByName.getDescription());
        assertEquals("GET", getUserByName.getVerb());
        assertEquals("200", getUserByName.getResponseStatus());
        assertEquals("application/json", getUserByName.getContentType());
        assertNotNull(getUserByName.getResponseProperties());
        assertEquals("Mocked string", ((Map) getUserByName.getResponseProperties()).get("username"));
        assertEquals("Mocked string", ((Map) getUserByName.getResponseProperties()).get("uuid"));

        final List<SwaggerVerb> repositoriesUsernameVerbs = repositoriesUsername.getVerbs();
        assertNotNull(repositoriesUsernameVerbs);
        assertEquals(1, repositoriesUsernameVerbs.size());
        final SwaggerVerb getRepositoriesByOwner = repositoriesUsernameVerbs.iterator().next();
        assertEquals("getRepositoriesByOwner", getRepositoriesByOwner.getDescription());
        assertEquals("GET", getRepositoriesByOwner.getVerb());
        assertEquals("200", getRepositoriesByOwner.getResponseStatus());
        assertEquals("application/json", getRepositoriesByOwner.getContentType());
        assertNotNull(getRepositoriesByOwner.getResponseProperties());
        assertTrue(getRepositoriesByOwner.isArray());
        assertEquals("Mocked string", ((Map) getRepositoriesByOwner.getResponseProperties()).get("slug"));
        assertEquals("Mocked string", ((Map) ((Map) getRepositoriesByOwner.getResponseProperties()).get("owner")).get("username"));
        assertEquals("Mocked string", ((Map) ((Map) getRepositoriesByOwner.getResponseProperties()).get("owner")).get("uuid"));
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV3WithPetstore() throws IOException {
        final SwaggerApiEntity api = prepareInline("io/gravitee/management/service/mock/petstore.yaml", true);
        assertEquals("1.0.0", api.getVersion());
        assertEquals("/v1", api.getContextPath());
        assertEquals("Swagger Petstore", api.getName());
        assertEquals("http://petstore.swagger.io/v1", api.getEndpoint().get(0));
        assertEquals(2, api.getPaths().size());
        final SwaggerPath pets = api.getPaths().get(0);
        assertEquals("/pets", pets.getPath());
        final SwaggerPath petsId = api.getPaths().get(1);
        assertEquals("/pets/:petId", petsId.getPath());

        final List<SwaggerVerb> petsVerbs = pets.getVerbs();
        assertNotNull(petsVerbs);
        assertEquals(2, petsVerbs.size());
        final SwaggerVerb findPets = petsVerbs.iterator().next();
        assertEquals("List all pets", findPets.getDescription());
        assertEquals("GET", findPets.getVerb());
        assertEquals("200", findPets.getResponseStatus());
        assertEquals("application/json", findPets.getContentType());
        assertNotNull(findPets.getResponseProperties());
        assertTrue(findPets.isArray());
        assertEquals(3, ((Map) findPets.getResponseProperties()).size());
        assertEquals("Mocked string", ((Map) findPets.getResponseProperties()).get("name"));
        assertEquals("Mocked string", ((Map) findPets.getResponseProperties()).get("tag"));
        assertTrue(((Map) findPets.getResponseProperties()).get("id") instanceof Integer);
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV3WithPetstoreExpanded() throws IOException {
        final SwaggerApiEntity api = prepareInline("io/gravitee/management/service/mock/petstore-expanded.yaml", true);
        assertEquals("1.0.0", api.getVersion());
        assertEquals("Swagger Petstore", api.getName());
        assertEquals("/api", api.getContextPath());
        assertEquals("http://petstore.swagger.io/api", api.getEndpoint().get(0));
        assertEquals(2, api.getPaths().size());
        final SwaggerPath pets = api.getPaths().get(0);
        assertEquals("/pets", pets.getPath());
        final SwaggerPath petsId = api.getPaths().get(1);
        assertEquals("/pets/:id", petsId.getPath());

        final List<SwaggerVerb> petsVerbs = pets.getVerbs();
        assertNotNull(petsVerbs);
        assertEquals(2, petsVerbs.size());
        final SwaggerVerb findPets = petsVerbs.iterator().next();
        assertEquals("findPets", findPets.getDescription());
        assertEquals("GET", findPets.getVerb());
        assertEquals("200", findPets.getResponseStatus());
        assertEquals("application/json", findPets.getContentType());
        assertNotNull(findPets.getResponseProperties());
        assertTrue(findPets.isArray());
        assertEquals(3, ((Map) findPets.getResponseProperties()).size());
        assertEquals("Mocked string", ((Map) findPets.getResponseProperties()).get("name"));
        assertEquals("Mocked string", ((Map) findPets.getResponseProperties()).get("tag"));
        assertTrue(((Map) findPets.getResponseProperties()).get("id") instanceof Integer);

        final List<SwaggerVerb> petsIdVerbs = petsId.getVerbs();
        assertNotNull(petsIdVerbs);
        assertEquals(2, petsIdVerbs.size());
        final SwaggerVerb findPetsId = petsIdVerbs.iterator().next();
        assertEquals("find pet by id", findPetsId.getDescription());
        assertEquals("GET", findPetsId.getVerb());
        assertEquals("200", findPetsId.getResponseStatus());
        assertEquals("application/json", findPetsId.getContentType());
        assertNotNull(findPetsId.getResponseProperties());
        assertEquals(3, ((Map) findPetsId.getResponseProperties()).size());
        assertEquals("Mocked string", ((Map) findPetsId.getResponseProperties()).get("name"));
        assertEquals("Mocked string", ((Map) findPetsId.getResponseProperties()).get("tag"));
        assertTrue(((Map) findPetsId.getResponseProperties()).get("id") instanceof Integer);
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV3WithExample() throws IOException {
        final SwaggerApiEntity api = prepareInline("io/gravitee/management/service/mock/uspto.yaml", true);
        assertEquals("1.0.0", api.getVersion());
        assertEquals("USPTO Data Set API", api.getName());
        assertEquals("/ds-api", api.getContextPath());
        assertEquals(2, api.getEndpoint().size());
        assertTrue(api.getEndpoint().contains("http://developer.uspto.gov/ds-api"));
        assertTrue(api.getEndpoint().contains("https://developer.uspto.gov/ds-api"));
        assertEquals(3, api.getPaths().size());
        final SwaggerPath metadata = api.getPaths().get(0);
        assertEquals("/", metadata.getPath());
        final SwaggerPath fields = api.getPaths().get(1);
        assertEquals("/:dataset/:version/fields", fields.getPath());
        final SwaggerPath searchRecords = api.getPaths().get(2);
        assertEquals("/:dataset/:version/records", searchRecords.getPath());

        final List<SwaggerVerb> metadataVerbs = metadata.getVerbs();
        assertNotNull(metadataVerbs);
        assertEquals(1, metadataVerbs.size());
        final SwaggerVerb getMetadata = metadataVerbs.iterator().next();
        assertEquals("List available data sets", getMetadata.getDescription());
        assertEquals("GET", getMetadata.getVerb());
        assertEquals("200", getMetadata.getResponseStatus());
        assertEquals("application/json", getMetadata.getContentType());
        assertNotNull(getMetadata.getResponseProperties());
        final Map responseExample = ((Map) getMetadata.getResponseProperties());
        assertEquals(2, responseExample.size());
        assertEquals(2, responseExample.get("total"));
        final List<Map<String, String>> apis = (List) responseExample.get("apis");
        assertEquals(2, apis.size());
        assertEquals("oa_citations", apis.iterator().next().get("apiKey"));

        final List<SwaggerVerb> fieldsVerbs = fields.getVerbs();
        assertNotNull(fieldsVerbs);
        assertEquals(1, fieldsVerbs.size());
        final SwaggerVerb getFields = fieldsVerbs.iterator().next();
        assertEquals("Provides the general information about the API and the list of fields that can be used to query the dataset.", getFields.getDescription());
        assertEquals("GET", getFields.getVerb());
        assertEquals("200", getFields.getResponseStatus());
        assertEquals("application/json", getFields.getContentType());
        assertNotNull(getFields.getResponseProperties());
        assertEquals("Mocked string", ((Map) getFields.getResponseProperties()).get("string"));
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV3WithEnumExample() throws IOException {
        final SwaggerApiEntity api = prepareInline("io/gravitee/management/service/mock/enum-example.yml", true);
        assertEquals("v1", api.getVersion());
        assertEquals("Gravitee Import Mock Example", api.getName());
        assertEquals("graviteeimportmockexample", api.getContextPath());
        assertEquals(1, api.getEndpoint().size());
        assertTrue(api.getEndpoint().contains("/"));
        assertEquals(1, api.getPaths().size());
        final SwaggerPath swaggerPath = api.getPaths().get(0);
        assertEquals("/", swaggerPath.getPath());

        final List<SwaggerVerb> swaggerVerbs = swaggerPath.getVerbs();
        assertNotNull(swaggerVerbs);
        assertEquals(1, swaggerVerbs.size());
        final SwaggerVerb getRoot = swaggerVerbs.iterator().next();
        assertEquals("getRoot", getRoot.getDescription());
        assertEquals("GET", getRoot.getVerb());
        assertEquals("200", getRoot.getResponseStatus());
        assertEquals("application/json", getRoot.getContentType());
        assertNotNull(getRoot.getResponseProperties());
        assertFalse(getRoot.isArray());
        final Map responseExample = ((Map) getRoot.getResponseProperties());
        assertEquals(10, responseExample.size());
        assertEquals("Mocked string", responseExample.get("optionalValue"));
        assertEquals("Mocked string", responseExample.get("stringValue"));
        assertEquals("exampleValue", responseExample.get("stringExampleValue"));
        assertEquals("value1", responseExample.get("enumValue"));
        assertEquals(123, responseExample.get("integerExampleValue"));
        assertEquals("Mocked string", ((Map) responseExample.get("inlinedObjectValue")).get("nestedOptionalValue"));
        assertEquals("Mocked string", ((Map) responseExample.get("inlinedObjectValue")).get("nestedStringValue"));
        assertEquals("nestedExampleValue", ((Map) responseExample.get("inlinedObjectValue")).get("nestedStringExampleValue"));
        assertEquals("Mocked string", ((Map) responseExample.get("objectValue")).get("nestedOptionalValue"));
        assertEquals("Mocked string", ((Map) responseExample.get("objectValue")).get("nestedStringValue"));
        assertEquals("nestedExampleValue", ((Map) responseExample.get("objectValue")).get("nestedStringExampleValue"));
        assertEquals("itemValue", ((List) responseExample.get("inlinedArrayValue")).get(0));
        assertEquals("itemValue", ((List) responseExample.get("arrayValue")).get(0));
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV3WithMonoServer() throws IOException {
        final SwaggerApiEntity api = prepareInline("io/gravitee/management/service/mock/openapi-monoserver.yaml", true);
        assertEquals("/v1", api.getContextPath());
        assertEquals(1, api.getEndpoint().size());
        assertTrue(api.getEndpoint().contains("https://development.gigantic-server.com/v1"));
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV3WithMultiServer() throws IOException {
        final SwaggerApiEntity api = prepareInline("io/gravitee/management/service/mock/openapi-multiserver.yaml", true);
        assertEquals("/v1", api.getContextPath());
        assertEquals(3, api.getEndpoint().size());
        assertTrue(api.getEndpoint().contains("https://development.gigantic-server.com/v1"));
        assertTrue(api.getEndpoint().contains("https://staging.gigantic-server.com/v1"));
        assertTrue(api.getEndpoint().contains("https://api.gigantic-server.com/v1"));
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV3WithNoServer() throws IOException {
        final SwaggerApiEntity api = prepareInline("io/gravitee/management/service/mock/openapi-noserver.yaml", true);
        assertEquals("noserver", api.getContextPath());
        assertEquals(1, api.getEndpoint().size());
        assertTrue(api.getEndpoint().contains("/"));
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV3WithVariablesInServer() throws IOException {
        final SwaggerApiEntity api = prepareInline("io/gravitee/management/service/mock/openapi-variables-in-server.yaml", true);
        assertEquals("/v2", api.getContextPath());
        assertEquals(2, api.getEndpoint().size());
        assertTrue(api.getEndpoint().contains("https://demo.gigantic-server.com:443/v2"));
        assertTrue(api.getEndpoint().contains("https://demo.gigantic-server.com:8443/v2"));
    }

    @Test
    public void shouldPrepareAPIFromSwaggerV3WithComplexReferences() throws IOException {
        final SwaggerApiEntity api = prepareInline("io/gravitee/management/service/mock/json-api.yml", true);

        assertEquals(2, api.getPaths().size());
        final SwaggerPath swaggerPath = api.getPaths().get(0);
        assertEquals("/drives", swaggerPath.getPath());

        final List<SwaggerVerb> swaggerVerbs = swaggerPath.getVerbs();
        assertNotNull(swaggerVerbs);
        assertEquals(2, swaggerVerbs.size());
        final SwaggerVerb getDrives = swaggerVerbs.iterator().next();
        assertEquals("List the Drive sites", getDrives.getDescription());
        assertEquals("GET", getDrives.getVerb());
        assertEquals("200", getDrives.getResponseStatus());
        assertEquals("application/vnd.api+json", getDrives.getContentType());
        assertNotNull(getDrives.getResponseProperties());
        assertFalse(getDrives.isArray());
        final Map responseExample = ((Map) getDrives.getResponseProperties());
        assertEquals(3, responseExample.size());

        assertNotNull(responseExample.get("data"));
        final Map data = (Map) responseExample.get("data");
        assertEquals(5, data.size());
        assertEquals("823", data.get("id"));
        assertEquals("drives", data.get("type"));
        final Map dataRelationships = ((Map) data.get("relationships"));
        assertEquals("https://host/drives/823/time-slots", ((Map) ((Map) dataRelationships.get("timeSlots")).get("links")).get("related"));
        final Map dataLinks = ((Map) data.get("links"));
        assertEquals("https://host/drives/823", dataLinks.get("self"));
        final Map dataAttributes = ((Map) data.get("attributes"));
        assertEquals("Lille", dataAttributes.get("name"));

        final Map dataAttributesAdress = ((Map) dataAttributes.get("address"));
        assertEquals("59260", dataAttributesAdress.get("zipCode"));
        assertEquals("RUE DES XXX", dataAttributesAdress.get("streetName"));
        assertEquals("New York", dataAttributesAdress.get("city"));
        assertEquals("7 bis", dataAttributesAdress.get("streetNumber"));

        final Map dataAttributesLocation = ((Map) dataAttributes.get("location"));
        assertEquals("3.121271", dataAttributesLocation.get("latitude").toString());
        assertEquals("50.615205", dataAttributesLocation.get("longitude").toString());

        assertNotNull(responseExample.get("meta"));
        final Map meta = (Map) responseExample.get("meta");
        assertEquals(5, meta.size());
        assertEquals(50, meta.get("per-page"));
        assertEquals(489, meta.get("total"));
        assertEquals(3, meta.get("current-page"));
        assertEquals(101, meta.get("from"));
        assertEquals(150, meta.get("to"));

        assertNotNull(responseExample.get("links"));
        final Map links = (Map) responseExample.get("links");
        assertEquals(5, links.size());
        assertEquals("https://host/drives?page[number]=4&page[size]=50", links.get("next"));
        assertEquals("https://host/drives?page[number]=10&page[size]=50", links.get("last"));
        assertEquals("https://host/drives?page[number]=2&page[size]=50", links.get("prev"));
        assertEquals("https://host/drives?page[number]=3&page[size]=50", links.get("self"));
        assertEquals("https://host/drives?page[number]=1&page[size]=50", links.get("first"));
    }
     */
}
