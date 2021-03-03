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
package io.gravitee.rest.api.model.settings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.gravitee.rest.api.model.annotations.ParameterKey;
import io.gravitee.rest.api.model.parameters.Key;

import java.util.List;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Api {
    @ParameterKey(Key.API_LABELS_DICTIONARY)
    private List<String> labelsDictionary;
    @ParameterKey(Key.API_PRIMARY_OWNER_MODE)
    private String primaryOwnerMode;

    public List<String> getLabelsDictionary() {
        return labelsDictionary;
    }

    public void setLabelsDictionary(List<String> labelsDictionary) {
        this.labelsDictionary = labelsDictionary;
    }

    public String getPrimaryOwnerMode() {
        return primaryOwnerMode;
    }

    public void setPrimaryOwnerMode(String primaryOwnerMode) {
        this.primaryOwnerMode = primaryOwnerMode;
    }
}
