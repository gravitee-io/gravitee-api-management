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
package io.gravitee.rest.api.service.impl.swagger.policy;

import io.gravitee.policy.api.swagger.v2.SwaggerOperationVisitor;
import io.gravitee.policy.api.swagger.v3.OAIOperationVisitor;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PolicyOperationVisitor {

    private String id;

    private String name;

    private SwaggerOperationVisitor swaggerOperationVisitor;

    private OAIOperationVisitor oaiOperationVisitor;

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

    public SwaggerOperationVisitor getSwaggerOperationVisitor() {
        return swaggerOperationVisitor;
    }

    public void setSwaggerOperationVisitor(SwaggerOperationVisitor swaggerOperationVisitor) {
        this.swaggerOperationVisitor = swaggerOperationVisitor;
    }

    public OAIOperationVisitor getOaiOperationVisitor() {
        return oaiOperationVisitor;
    }

    public void setOaiOperationVisitor(OAIOperationVisitor oaiOperationVisitor) {
        this.oaiOperationVisitor = oaiOperationVisitor;
    }

    public boolean display() {
        return (
            (oaiOperationVisitor == null && swaggerOperationVisitor == null) ||
            (swaggerOperationVisitor != null && swaggerOperationVisitor.display()) ||
            (oaiOperationVisitor != null && oaiOperationVisitor.display())
        );
    }
}
