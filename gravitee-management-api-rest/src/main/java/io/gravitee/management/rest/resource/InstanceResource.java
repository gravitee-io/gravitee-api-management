package io.gravitee.management.rest.resource;

import io.gravitee.common.http.MediaType;
import io.gravitee.management.model.InstanceEntity;
import io.gravitee.management.service.InstanceService;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
public class InstanceResource extends AbstractResource {

    @Inject
    private InstanceService instanceService;

    @PathParam("instance")
    private String instance;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public InstanceEntity get() {
        return instanceService.findById(this.instance);
    }
}
