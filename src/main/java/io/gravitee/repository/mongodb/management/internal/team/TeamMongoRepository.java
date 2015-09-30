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
package io.gravitee.repository.mongodb.management.internal.team;

import java.util.List;

import io.gravitee.repository.mongodb.management.internal.model.TeamMongo;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamMongoRepository extends MongoRepository<TeamMongo, String>, TeamMongoRepositoryCustom {

	@Query("{ 'name' : ?0}")
	TeamMongo findByName(String teamName);

	@Query("{ 'privateTeam' : ?0}")
	List<TeamMongo> findByVisibility(boolean privateVisibity);
}


