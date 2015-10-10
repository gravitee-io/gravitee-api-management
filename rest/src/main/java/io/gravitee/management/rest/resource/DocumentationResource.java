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
package io.gravitee.management.rest.resource;

import io.gravitee.management.model.NewPageEntity;
import io.gravitee.management.model.PageEntity;
import io.gravitee.management.model.UpdatePageEntity;
import io.gravitee.management.service.DocumentationService;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Titouan COMPIEGNE
 */
@Path("/documentation")
public class DocumentationResource extends AbstractResource {
	
	@Inject
	private DocumentationService documentationService;
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/pages/{apiName}")
	public List<PageEntity> listByApiName(@PathParam("apiName") String apiName) {
		return documentationService.findByApiName(apiName);
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public PageEntity createPage(NewPageEntity newPage) {
		int order = documentationService.findMaxPageOrderByApiName(newPage.getApiName()) + 1;
		newPage.setOrder(order);
		newPage.setLastContributor(getAuthenticatedUser());
		return documentationService.createPage(newPage);
	}
	
	@GET
	@Path("/pages/{name}/content")
	@Produces(MediaType.APPLICATION_JSON)
	public String contentPage(@PathParam("name") String name) {
		Optional<PageEntity> optPage = documentationService.findByName(name);
		if (optPage != null && optPage.isPresent()) {
			return optPage.get().getContent();
		}
		return Response.status(400).toString();
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/pages/{name}/edit")
	public PageEntity editPage(@PathParam("name") String name, UpdatePageEntity updatePageEntity) {
		updatePageEntity.setLastContributor(getAuthenticatedUser());
		return documentationService.updatePage(name, updatePageEntity);
	}
	
	@POST
	@Path("/pages/{name}/delete")
	public void deletePage(@PathParam("name") String name) {
		documentationService.deletePage(name);
	}
}
