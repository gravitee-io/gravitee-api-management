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
import javax.validation.constraints.Size;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NewPageEntity {

	@NotNull
	@Size(min = 1)
	private String name;

	@NotNull
	private PageType type;
	
	private String content;
	
	private int order;
	
	private String lastContributor;

	private PageSourceEntity source;

	private PageConfigurationEntity configuration;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public PageType getType() {
		return type;
	}

	public void setType(PageType type) {
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

	public PageConfigurationEntity getConfiguration() {
		return configuration;
	}

	public void setConfiguration(PageConfigurationEntity configuration) {
		this.configuration = configuration;
	}

	@Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Page{");
        sb.append("name='").append(name).append('\'');
        sb.append(", type='").append(type).append('\'');
        sb.append(", order='").append(order).append('\'');
        sb.append(", lastContributor='").append(lastContributor).append('\'');
        sb.append('}');
        return sb.toString();
    }
	
}
