package io.gravitee.management.api.resources;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.core.Response;
import java.util.Set;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class PoliciesConfigurationResource {

    private String apiName;

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    @GET
    public Set listAll() {
        return null;
    }

    @POST
    public Response addPolicyConfigurations(Set policiesConfiguration) {
        return null;
    }
}
