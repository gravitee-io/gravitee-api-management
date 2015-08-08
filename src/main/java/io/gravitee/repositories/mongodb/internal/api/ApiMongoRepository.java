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
package io.gravitee.repositories.mongodb.internal.api;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import io.gravitee.repositories.mongodb.internal.model.ApiMongo;

@Repository
public interface ApiMongoRepository extends MongoRepository<ApiMongo, String>, ApiMongoRepositoryCustom {


		
	/**
	 * Find Apis by creator username
	 * @param username
	 * @return
	 */
	@Query("{'creator.$id' : ?0}")  
	public List<ApiMongo> findByCreator(String username);

    /**
     * Count api by teamname (owner)
     * @param teamname
     * 
     * @param privateVisibility : true only private / false only public
     * @return
     */	
	@Query(count=true, value="{'owner' : {'$ref' : 'teams', '$id' : ?0}, privateApi : ?1}")
	public long countByTeamAndVisibily(String teamname, boolean privateVisibility);

}


