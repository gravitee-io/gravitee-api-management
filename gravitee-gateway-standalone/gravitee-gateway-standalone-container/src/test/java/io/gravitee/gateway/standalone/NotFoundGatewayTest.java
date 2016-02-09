package io.gravitee.gateway.standalone;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.standalone.junit.annotation.ApiDescriptor;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author GraviteeSource Team
 */
@ApiDescriptor("/io/gravitee/gateway/standalone/teams.json")
public class NotFoundGatewayTest extends AbstractGatewayTest {

    @Test
    public void call_no_api() throws IOException {
        Request request = Request.Get("http://localhost:8082/unknow");
        Response response = request.execute();
        HttpResponse returnResponse = response.returnResponse();

        assertEquals(HttpStatusCode.NOT_FOUND_404, returnResponse.getStatusLine().getStatusCode());
    }

    @Test
    public void call_get_not_started_api() throws Exception {
        Request request = Request.Get("http://localhost:8082/not_started_api");
        Response response = request.execute();
        HttpResponse returnResponse = response.returnResponse();

        assertEquals(HttpStatusCode.NOT_FOUND_404, returnResponse.getStatusLine().getStatusCode());
    }
}
