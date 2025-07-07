/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author Guillaume GILLON
 * @author GraviteeSource Team
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class NewPageEntity extends FetchablePageEntity {

    /**
     * The page crossId uniquely identifies a page across environments.
     * Pages promoted between environments will share the same crossId.
     */
    private String crossId;
    private String hrid;

    @NotNull
    @Size(min = 1)
    private String name;

    @NotNull
    private PageType type;

    private int order;

    private boolean published;

    private Visibility visibility;

    private String lastContributor;

    private PageSourceEntity source;

    private Map<String, String> configuration;

    private boolean homepage;

    @JsonProperty("excluded_groups")
    private List<String> excludedGroups;

    private boolean excludedAccessControls;

    private Set<AccessControlEntity> accessControls;

    @JsonProperty("attached_media")
    private List<PageMediaEntity> attachedMedia;

    private String parentId;

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Page{");
        sb.append("crossId='").append(crossId).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append(", type='").append(type).append('\'');
        sb.append(", order='").append(order).append('\'');
        sb.append(", homepage='").append(homepage).append('\'');
        sb.append(", visibility='").append(visibility).append('\'');
        sb.append(", lastContributor='").append(lastContributor).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
