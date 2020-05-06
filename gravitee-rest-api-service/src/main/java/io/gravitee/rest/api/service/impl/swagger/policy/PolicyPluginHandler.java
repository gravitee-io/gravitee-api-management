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

import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.policy.api.swagger.v2.SwaggerOperationVisitor;
import io.gravitee.policy.api.swagger.v3.OAIOperationVisitor;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PolicyPluginHandler extends io.gravitee.plugin.policy.internal.PolicyPluginHandler {

    @Autowired
    private PolicyOperationVisitorManager visitorManager;

    @Override
    protected void register(PolicyPlugin policyPlugin) {
        Class<? extends OAIOperationVisitor> oaiVisitor = new OAIOperationVisitorClassFinder().lookupFirst(policyPlugin.policy());
        Class<? extends SwaggerOperationVisitor> swaggerVisitor = new SwaggerOperationVisitorClassFinder().lookupFirst(policyPlugin.policy());

        try {
            addVisitor(
                    policyPlugin,
                    createInstance(swaggerVisitor),
                    createInstance(oaiVisitor));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addVisitor(PolicyPlugin policyPlugin, SwaggerOperationVisitor swaggerOperationVisitor,
                            OAIOperationVisitor oaiOperationVisitor)  {
        if (swaggerOperationVisitor != null && oaiOperationVisitor != null) {
            logger.info("Add a Swagger visitor for policy: id{}", policyPlugin.id());

            PolicyOperationVisitor visitor = new PolicyOperationVisitor();
            visitor.setId(policyPlugin.id());
            visitor.setName(policyPlugin.manifest().name());
            visitor.setSwaggerOperationVisitor(swaggerOperationVisitor);
            visitor.setOaiOperationVisitor(oaiOperationVisitor);

            visitorManager.add(visitor);
        }
    }

    private <T> T createInstance(Class<T> clazz) throws Exception {
        if (clazz == null) {
            return null;
        }

        try {
            return clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException ex) {
            logger.error("Unable to instantiate class: {}", clazz, ex);
            throw ex;
        }
    }
}
