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
package io.gravitee.rest.api.portal.rest.resource.param;

import io.gravitee.rest.api.portal.rest.model.FilterApiQuery;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.QueryParam;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApisParam {

    @ApiParam(value = "filter by context path")
    @QueryParam("context-path")
    private String contextPath;
    @ApiParam(value = "filter by label")
    @QueryParam("label")
    private String label;
    @ApiParam(value = "filter by version")
    @QueryParam("version")
    private String version;
    @ApiParam(value = "filter by full API Name")
    @QueryParam("name")
    private String name;
    @ApiParam(value = "filter by tag")
    @QueryParam("tag")
    private String tag;
    @ApiParam(value = "filter by category id")
    @QueryParam("category")
    private String category;
    @QueryParam("filter")
    private FilterApiQuery filter;
    @QueryParam("-filter")
    private FilterApiQuery excludedFilter;
    
    public String getContextPath() {
        return contextPath;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public FilterApiQuery getFilter() {
        return filter;
    }

    public void setFilter(FilterApiQuery filter) {
        this.filter = filter;
    }

    public FilterApiQuery getExcludedFilter() {
        return excludedFilter;
    }

    public void setExcludedFilter(FilterApiQuery excludedFilter) {
        this.excludedFilter = excludedFilter;
    }
}
