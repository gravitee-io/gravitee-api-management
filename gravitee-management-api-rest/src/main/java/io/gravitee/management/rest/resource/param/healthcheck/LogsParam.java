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
package io.gravitee.management.rest.resource.param.healthcheck;

import javax.validation.constraints.Min;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public class LogsParam {

    @Min(0)
    @QueryParam("from")
    private long from;

    @Min(0)
    @QueryParam("to")
    private long to;

    @QueryParam("query")
    private String query;

    @QueryParam("size")
    @DefaultValue("10")
    private int size;

    @QueryParam("page")
    @DefaultValue("1")
    private int page;

    @QueryParam("transition")
    private Boolean transition;

    
    public long getFrom() {
        return from;
    }

    public void setFrom(long from) {
        this.from = from;
    }

    public long getTo() {
        return to;
    }

    public void setTo(long to) {
        this.to = to;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public Boolean isTransition() {
        return transition;
    }

    public void setTransition(Boolean transition) {
        this.transition = transition;
    }

    public void validate() throws WebApplicationException {
        if (page < 0) {
            page = 0;
        }

        if (size < 0) {
            size = 10;
        }
    }
}
