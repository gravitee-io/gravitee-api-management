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

import javax.inject.Inject;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.codec.binary.Base64;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;

/**
 * 
 * @author Titouan COMPIEGNE
 *
 */
@Path("/login")
public class LoginResource extends AbstractResource {

	@Inject
	private AuthenticationManager authenticationManager;

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public Response login(@HeaderParam("Authorization") String authorization) {
		try {
			String base64Credentials = authorization.substring("Basic".length()).trim();
			String authorizationDecode = new String(Base64.decodeBase64(base64Credentials));
			String[] auth = authorizationDecode.split(":");
			String username = auth[0];
			String password = auth[1];

			Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password, AuthorityUtils.commaSeparatedStringToAuthorityList("ROLE_USER")));
			return Response.ok(authentication.getPrincipal(), MediaType.APPLICATION_JSON).build();
		} catch (Exception e) {
			return Response.status(Response.Status.UNAUTHORIZED).entity("Bad credentials").build();
		}
	}
}