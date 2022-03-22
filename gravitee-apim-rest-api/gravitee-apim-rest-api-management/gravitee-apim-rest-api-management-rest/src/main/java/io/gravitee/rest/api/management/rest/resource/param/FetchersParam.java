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

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.ws.rs.QueryParam;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class FetchersParam {

    @QueryParam("expand")
    @Parameter(explode = Explode.FALSE, schema = @Schema(type = "array"))
    ListStringParam expand;

    @QueryParam("import")
    private boolean onlyFilesFetchers;

    public ListStringParam getExpand() {
        return expand;
    }

    public void setExpand(ListStringParam expand) {
        this.expand = expand;
    }

    public boolean isOnlyFilesFetchers() {
        return onlyFilesFetchers;
    }

    public void setOnlyFilesFetchers(boolean onlyFilesFetchers) {
        this.onlyFilesFetchers = onlyFilesFetchers;
    }
}
