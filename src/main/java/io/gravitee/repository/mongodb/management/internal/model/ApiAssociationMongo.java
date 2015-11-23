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
package io.gravitee.repository.mongodb.management.internal.model;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Mongo object model for association beetween key and api.
 * 
 * @author Loic DASSONVILLE (loic.dassonville at gmail.com)
 */
@Document(collection="keys")
public class ApiAssociationMongo {
  
	@Id 
	private ObjectId id;
	
	@DBRef
    private ApplicationMongo application;
    
    @DBRef
    private ApiMongo api;
    
    private ApiKeyMongo key;

	public ObjectId getId() {
		return id;
	}

	public void setId(ObjectId id) {
		this.id = id;
	}

	public ApplicationMongo getApplication() {
		return application;
	}

	public void setApplication(ApplicationMongo application) {
		this.application = application;
	}

	public ApiMongo getApi() {
		return api;
	}

	public void setApi(ApiMongo api) {
		this.api = api;
	}

	public ApiKeyMongo getKey() {
		return key;
	}

	public void setKey(ApiKeyMongo key) {
		this.key = key;
	}
}
