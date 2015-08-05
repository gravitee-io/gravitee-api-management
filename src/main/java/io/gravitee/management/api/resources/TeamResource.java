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

import io.gravitee.repository.api.TeamRepository;
import io.gravitee.repository.model.Team;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Component
@Produces(MediaType.APPLICATION_JSON)
public class TeamResource {

    private String teamName;

    @Autowired
    private TeamRepository teamRepository;

    @GET
    public Team get() {
        return teamRepository.findByName(teamName);
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }
}
