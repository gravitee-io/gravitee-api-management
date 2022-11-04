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
package io.gravitee.rest.api.management.rest.resource.param.kubernetes.v1alpha1;

import io.swagger.v3.oas.annotations.Parameter;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import lombok.Getter;
import lombok.Setter;

/**
 * @author GraviteeSource Team
 */
@Getter
@Setter
public class ApiExportParam {

    @Parameter(description = "whether to remove all ids from the exported crd (default is false)")
    @QueryParam("removeIds")
    private boolean removeIds;

    @Parameter(description = "the context path to assign to the exported crd (default is the existing context path of the exported API")
    @QueryParam("contextPath")
    private String contextPath;

    @Parameter(description = "the API version to assign to the exported crd (default is the existing version of the exported API")
    @QueryParam("version")
    private String version;

    @Parameter(description = "the management context name to assign to the exported crd (if not defined, no context is added)")
    @QueryParam("managementContextName")
    private String managementContextName;

    @Parameter(description = "the management context namespace to assign to the exported crd (default is 'default')")
    @QueryParam("managementContextNamespace")
    @DefaultValue("default")
    private String managementContextNamespace;

    @Parameter(description = "fields to exclude from the exported crd (default is none)")
    @QueryParam("exclude")
    @DefaultValue("")
    private String exclude;
}
