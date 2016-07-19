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
package io.gravitee.management.rest.resource.param;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AnalyticsAggTypeParam {

    private static final String SEPARATOR = ",";
    private List<String> aggTypes;

    public AnalyticsAggTypeParam(String param) {
        aggTypes = new ArrayList<>();

        if (param != null) {
            String[] params = param.replaceAll("\\s","").split(SEPARATOR);
            for (String _param : params) {
                aggTypes.add(_param);
            }
        }
    }

    public List<String> getAggTypes() {
        return aggTypes;
    }
}
