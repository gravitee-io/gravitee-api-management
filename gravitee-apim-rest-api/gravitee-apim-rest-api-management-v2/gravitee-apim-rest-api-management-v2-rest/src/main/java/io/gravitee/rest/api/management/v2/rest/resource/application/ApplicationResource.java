package io.gravitee.rest.api.management.v2.rest.resource.application;

import io.gravitee.apim.core.application_dictionary.use_case.UpdateApplicationDictionaryUseCase;
import io.gravitee.rest.api.management.v2.rest.mapper.ApplicationDictionaryMapper;
import io.gravitee.rest.api.management.v2.rest.model.ApplicationDictionary;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ApplicationResource extends AbstractResource {

    @Inject
    private UpdateApplicationDictionaryUseCase updateApplicationDictionaryUseCase;

    @GET
    @Path("/dictionary")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getApplicationDictionary(@Valid ApplicationDictionary dictionary) {
        return Response.ok(dictionary).build();
    }

    @POST
    @Path("/dictionary")
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateApplicationDictionary(@PathParam("applicationId") String applicationId, @Valid ApplicationDictionary dictionary) {
        var input = ApplicationDictionaryMapper.INSTANCE.toInput(applicationId, dictionary);
        var output = updateApplicationDictionaryUseCase.execute(input);
        ApplicationDictionary entity = ApplicationDictionaryMapper.INSTANCE.toApplicationDictionary(output);
        return Response.ok(entity).build();
    }
}
