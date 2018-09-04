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
package io.gravitee.management.model.alert;

import java.util.List;

import static io.gravitee.management.model.alert.AlertReferenceType.API;
import static io.gravitee.management.model.alert.AlertReferenceType.APPLICATION;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public enum AlertType {
    HEALTH_CHECK(singletonList(API)), REQUEST(asList(API, APPLICATION));

    private List<AlertReferenceType> referenceTypes;

    AlertType(List<AlertReferenceType> referenceTypes) {
        this.referenceTypes = referenceTypes;
    }

    public List<AlertReferenceType> getReferenceTypes() {
        return referenceTypes;
    }
}