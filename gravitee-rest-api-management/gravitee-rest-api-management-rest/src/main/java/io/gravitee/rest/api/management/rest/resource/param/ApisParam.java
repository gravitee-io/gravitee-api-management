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
package io.gravitee.rest.api.management.rest.resource.param;

import io.swagger.annotations.ApiParam;
import javax.ws.rs.QueryParam;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApisParam {

    @ApiParam(value = "filter by category id")
    @QueryParam("category")
    private String category;

    @ApiParam(value = "filter by group id")
    @QueryParam("group")
    private String group;

    @ApiParam(value = "true if you only want Top APIs. default: false")
    @QueryParam("top")
    private boolean top;

    @ApiParam(value = "filter by context path")
    @QueryParam("context-path")
    private String contextPath;

    @ApiParam(value = "filter by label")
    @QueryParam("label")
    private String label;

    @ApiParam(value = "filter by state: STARTED or STOPPED")
    @QueryParam("state")
    private String state;

    @ApiParam(value = "filter by visibility: PUBLIC or PRIVATE")
    @QueryParam("visibility")
    private String visibility;

    @ApiParam(value = "filter by version")
    @QueryParam("version")
    private String version;

    @ApiParam(value = "filter by full API Name")
    @QueryParam("name")
    private String name;

    @ApiParam(value = "filter by tag")
    @QueryParam("tag")
    private String tag;

    @QueryParam("portal")
    private boolean portal;

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public boolean isTop() {
        return top;
    }

    public void setTop(boolean top) {
        this.top = top;
    }

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

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
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

    public boolean isPortal() {
        return portal;
    }

    public void setPortal(boolean portal) {
        this.portal = portal;
    }
}
