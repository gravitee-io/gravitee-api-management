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
package io.gravitee.management.model;

import java.util.Date;
import java.util.Objects;

/**
 * @author Titouan COMPIEGNE
 */
public class PageEntity {

	private String id;

	private String name;

	private String type;

	private String content;

	private int order;

	private String lastContributor;

	private boolean published;

	private Date lastModificationDate;

	private String contentType;

	private PageSourceEntity source;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
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

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	public String getLastContributor() {
		return lastContributor;
	}

	public void setLastContributor(String lastContributor) {
		this.lastContributor = lastContributor;
	}

	public boolean isPublished() {
		return published;
	}

	public void setPublished(boolean published) {
		this.published = published;
	}

	public Date getLastModificationDate() {
		return lastModificationDate;
	}

	public void setLastModificationDate(Date lastModificationDate) {
		this.lastModificationDate = lastModificationDate;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public PageSourceEntity getSource() {
		return source;
	}

	public void setSource(PageSourceEntity source) {
		this.source = source;
	}

	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof PageEntity)) {
			return false;
		}
		PageEntity that = (PageEntity) o;
		return Objects.equals(id, that.id);
	}

	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("Page{");
		sb.append("id='").append(name).append('\'');
		sb.append(", name='").append(name).append('\'');
		sb.append(", type='").append(type).append('\'');
		sb.append(", content='").append(content).append('\'');
		sb.append(", order='").append(order).append('\'');
		sb.append(", lastContributor='").append(lastContributor).append('\'');
		sb.append(", published='").append(published).append('\'');
		sb.append(", lastModificationDate='").append(lastModificationDate).append('\'');
		sb.append('}');
		return sb.toString();
	}
}
