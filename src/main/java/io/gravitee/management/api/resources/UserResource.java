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
package io.gravitee.management.api.resources;

import org.springframework.stereotype.Component;

import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Defines the REST resources to manage {@code User}.
 *
 * @author Azize Elamrani (azize dot elamrani at gmail dot com)
 */
@Component
@Produces(MediaType.APPLICATION_JSON)
@Path("/users")
public class UserResource {

    /*
    @GET
    public Response listAll() {
        final List<String> firstNames = new ArrayList<>(Arrays.asList("Azize", "Nicolas", "David", "Loïc", "Aurélien", "Jeoffrey", "Emmanuel",
            "Yves", "Francis", "Elton"));
        final List<String> lastNames = new ArrayList<>(Arrays.asList("Elamrani", "Géraud", "Brassely", "Dassonville", "Bourdon", "Hayaert", "Péru",
            "Fourré", "Laline", "John"));
        final Random random = new Random(System.currentTimeMillis());
        final List<User> users = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            final User user = new User();
            user.setCode(UUID.randomUUID().toString());
            int randomIndex = random.nextInt(11);
            final int firstNameIndex = randomIndex > firstNames.size() - 1 ? firstNames.size() - 1 : randomIndex;
            user.setFirstName(firstNames.remove(firstNameIndex));
            randomIndex = random.nextInt(11);
            final int lastNameIndex = randomIndex > lastNames.size() - 1 ? lastNames.size() - 1 : randomIndex;
            user.setLastName(lastNames.remove(lastNameIndex));
            user.setEmail(user.getFirstName().toLowerCase() + '.' + user.getLastName().toLowerCase() + "@gravitee.io");
            users.add(user);
        }
        return Response.status(HttpStatusCode.OK_200).entity(users)
            .header("Access-Control-Allow-Origin", "*").build();
    }

    @GET
    @Path("{id}/teams")
    public Response listTeams(@PathParam("id") final String id) {
        return Response.status(HttpStatusCode.OK_200).entity(Arrays.asList("Stores", "Gravitee", "Mobility", "Security"
            , "Intranet", "Web site"))
            .header("Access-Control-Allow-Origin", "*").build();
    }

    @GET
    @Path("/roles")
    public Response listRoles() {
        return Response.status(HttpStatusCode.OK_200).entity(Arrays.asList("ROLE_ADMIN", "ROLE_USER", "ROLE_MANAGEMENT"))
            .header("Access-Control-Allow-Origin", "*").build();
    }
    */
}
