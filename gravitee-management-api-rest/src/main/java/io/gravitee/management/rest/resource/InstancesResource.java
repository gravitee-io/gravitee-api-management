package io.gravitee.management.rest.resource;

import io.gravitee.common.http.MediaType;
import io.gravitee.management.model.InstanceEntity;
import io.gravitee.management.service.InstanceService;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author GraviteeSource Team
 */
@Path("/instances")
public class InstancesResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private InstanceService instanceService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<InstanceEntity> instances() {
        return instanceService.findInstances()
                .stream()
                .filter(InstanceEntity::isRunning)
                .collect(Collectors.toList());
    }
}
