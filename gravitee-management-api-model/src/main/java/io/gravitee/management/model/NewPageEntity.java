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

import javax.validation.constraints.NotNull;

/**
 * @author Titouan COMPIEGNE
 */
public class NewPageEntity {

	@NotNull
	private String name;
	
	@NotNull
	private String type;
	
	private String content;
	
	private int order;
	
	private String lastContributor;

	private PageSourceEntity source;

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

	public PageSourceEntity getSource() {
		return source;
	}

	public void setSource(PageSourceEntity source) {
		this.source = source;
	}

	@Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Page{");
        sb.append("name='").append(name).append('\'');
        sb.append(", type='").append(type).append('\'');
        sb.append(", content='").append(content).append('\'');
        sb.append(", order='").append(order).append('\'');
        sb.append(", lastContributor='").append(lastContributor).append('\'');
        sb.append('}');
        return sb.toString();
    }
	
}
