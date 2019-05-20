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

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Document(collection = "dictionaries")
public class DictionaryMongo extends Auditable {

	@Id
	private String id;

	private String environment;
	
	private String name;

	private String description;

	private String type;

	private DictionaryProviderMongo provider;

	private DictionaryTriggerMongo trigger;

	private Map<String, String> properties;

	private Date deployedAt;

	private String state;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public DictionaryProviderMongo getProvider() {
		return provider;
	}

	public void setProvider(DictionaryProviderMongo provider) {
		this.provider = provider;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public DictionaryTriggerMongo getTrigger() {
		return trigger;
	}

	public void setTrigger(DictionaryTriggerMongo trigger) {
		this.trigger = trigger;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}

	public Date getDeployedAt() {
		return deployedAt;
	}

	public void setDeployedAt(Date deployedAt) {
		this.deployedAt = deployedAt;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		DictionaryMongo that = (DictionaryMongo) o;

		return id.equals(that.id);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}
}
