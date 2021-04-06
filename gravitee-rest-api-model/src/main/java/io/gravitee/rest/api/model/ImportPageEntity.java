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
package io.gravitee.rest.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ImportPageEntity {

	@NotNull
	private PageType type;
	private boolean published;
	@NotNull
	private Visibility visibility;
	private String lastContributor;
	private PageSourceEntity source;
	private Map<String, String> configuration;

	@JsonProperty("excluded_groups")
	private List<String> excludedGroups;

	private boolean excludedAccessControls;
	private Set<AccessControlEntity> accessControls;

	public PageType getType() {
		return type;
	}

	public void setType(PageType type) {
		this.type = type;
	}

	public boolean isPublished() {
		return published;
	}

	public void setPublished(boolean published) {
		this.published = published;
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

	public Map<String, String> getConfiguration() {
		return configuration;
	}

	public void setConfiguration(Map<String, String> configuration) {
		this.configuration = configuration;
	}

	public Set<AccessControlEntity> getAccessControls() {
		return accessControls;
	}

	public void setAccessControls(Set<AccessControlEntity> accessControls) {
		this.accessControls = accessControls;
	}

	public boolean isExcludedAccessControls() {
		return excludedAccessControls;
	}

	public void setExcludedAccessControls(boolean excludedAccessControls) {
		this.excludedAccessControls = excludedAccessControls;
	}

	public Visibility getVisibility() {
		return visibility;
	}

	public void setVisibility(Visibility visibility) {
		this.visibility = visibility;
	}

	public List<String> getExcludedGroups() {
		return excludedGroups;
	}

	public void setExcludedGroups(List<String> excludedGroups) {
		this.excludedGroups = excludedGroups;
	}

	@Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Page{");
        sb.append("type='").append(type).append('\'');
        sb.append(", published='").append(published).append('\'');
        sb.append(", visibility='").append(visibility).append('\'');
        sb.append(", lastContributor='").append(lastContributor).append('\'');
        sb.append('}');
        return sb.toString();
    }

}
