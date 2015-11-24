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

import io.gravitee.management.model.ApiEntity;
import io.gravitee.management.model.ApplicationEntity;
import io.gravitee.management.service.ApiService;
import io.gravitee.management.service.ApplicationService;

import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * @author Titouan COMPIEGNE
 */
public class ApplicationApisResource {

	@Inject
	private ApplicationService applicationService;

	@Inject
	private ApiService apiService;

	@PathParam("application")
	private String application;

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Set<ApiEntity> apis() {
		// Check application exists
		ApplicationEntity applicationEntity = applicationService.findById(application);

		return apiService.findByApplication(applicationEntity.getId());
	}

}
