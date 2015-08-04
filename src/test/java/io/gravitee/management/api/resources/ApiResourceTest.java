package io.gravitee.management.api.resources;

import io.gravitee.management.api.builder.ApiBuilder;
import io.gravitee.repository.api.ApiRepository;
import io.gravitee.repository.model.Api;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.core.Response;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ApiResourceTest extends AbstractResourceTest {

    @Autowired
    private ApiRepository apiRepository;

    @Test
    public void testGetApi() {
        Api api = new ApiBuilder()
                .name("my-api")
                .origin("http://localhost/my-api")
                .target("http://remote_api/context")
                .createdAt(new Date())
                .build();

        Mockito.doReturn(api).when(apiRepository).findByName(api.getName());

        final Response response = target("/apis/" + api.getName()).request().get();

        // Check HTTP response
        assertEquals(200, response.getStatus());

        // Check Response content
        Api responseApi = response.readEntity(Api.class);
        assertNotNull(responseApi);
    }
}
