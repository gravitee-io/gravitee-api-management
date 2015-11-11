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

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * @author Titouan COMPIEGNE
 */
@Document(collection = "pages")
public class PageMongo {

	@Id
	@Field(value = "name")
	protected String name;

	private String type;

	private String title;

	private String content;

	private String lastContributor;

	private int order;

	private String apiName;

	private Date createdAt;

	private Date updatedAt;

	private boolean published;

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

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getLastContributor() {
		return lastContributor;
	}

	public void setLastContributor(String lastContributor) {
		this.lastContributor = lastContributor;
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	public String getApiName() {
		return apiName;
	}

	public void setApiName(String apiName) {
		this.apiName = apiName;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	public Date getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Date updatedAt) {
		this.updatedAt = updatedAt;
	}

	public boolean isPublished() {
		return published;
	}

	public void setPublished(boolean published) {
		this.published = published;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("Page{");
		sb.append("name='").append(name).append('\'');
		sb.append(", type='").append(type).append('\'');
		sb.append(", title='").append(title).append('\'');
		sb.append(", content='").append(content).append('\'');
		sb.append(", order='").append(order).append('\'');
		sb.append(", lastContributor='").append(lastContributor).append('\'');
		sb.append(", apiName='").append(apiName).append('\'');
		sb.append('}');
		return sb.toString();
	}
}
