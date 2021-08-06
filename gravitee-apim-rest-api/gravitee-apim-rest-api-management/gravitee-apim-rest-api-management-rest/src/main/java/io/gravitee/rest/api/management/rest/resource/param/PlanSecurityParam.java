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

import io.gravitee.rest.api.model.PlanSecurityType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PlanSecurityParam {

    private static final String SEPARATOR = ",";
    private List<PlanSecurityType> securities;

    public PlanSecurityParam(List<PlanSecurityType> securities) {
        this.securities = Collections.unmodifiableList(securities);
    }

    public PlanSecurityParam(String param) {
        this.securities = new ArrayList<>();

        if (param != null) {
            String[] params = param.replaceAll("\\s", "").split(SEPARATOR);
            for (String _param : params) {
                this.securities.add(PlanSecurityType.valueOf(_param.toUpperCase()));
            }
        }
    }

    public List<PlanSecurityType> getSecurities() {
        return this.securities;
    }
}
