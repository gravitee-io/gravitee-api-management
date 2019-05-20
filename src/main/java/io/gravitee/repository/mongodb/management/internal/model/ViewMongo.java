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
import java.util.Objects;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Document(collection = "views")
public class ViewMongo extends Auditable{

	@Id
	private String id;
	private String environment;
	private String name;
	private String description;
	private boolean defaultView;
	private boolean hidden;
	private int order;
	private String highlightApi;
	private String picture;

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

	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isDefaultView() {
		return defaultView;
	}
	public void setDefaultView(boolean defaultView) {
		this.defaultView = defaultView;
	}

	public boolean isHidden() {
		return hidden;
	}
	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}

	public int getOrder() {
		return order;
	}
	public void setOrder(int order) {
		this.order = order;
	}

	public String getHighlightApi() {
		return highlightApi;
	}

	public void setHighlightApi(String highlightApi) {
		this.highlightApi = highlightApi;
	}

	public String getPicture() {
		return picture;
	}

	public void setPicture(String picture) {
		this.picture = picture;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof ViewMongo)) return false;
		ViewMongo viewMongo = (ViewMongo) o;
		return Objects.equals(id, viewMongo.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public String toString() {
		return "ViewMongo{" +
				"id='" + id + '\'' +
                ", environment='" + environment + '\'' +
				", name='" + name + '\'' +
				", description='" + description + '\'' +
				", defaultView='" + defaultView + '\'' +
				", hidden='" + hidden + '\'' +
				", order='" + order + '\'' +
				", highlightApi='" + highlightApi + '\'' +
				'}';
	}
}
