package io.gravitee.management.rest.resource;

import io.gravitee.management.model.MemberEntity;
import io.gravitee.management.service.ApiService;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Set;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ApiMembersResource {

    @Inject
    private ApiService apiService;

    @PathParam("apiName")
    private String apiName;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Set<MemberEntity> members() {
        // Check that the API exists
        apiService.findByName(apiName);

        return apiService.getMembers(apiName, null);
    }
}
