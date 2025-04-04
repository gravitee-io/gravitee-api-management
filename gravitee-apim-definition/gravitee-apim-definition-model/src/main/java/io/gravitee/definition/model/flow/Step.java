/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.definition.model.flow;

import io.gravitee.definition.model.Plugin;
import java.util.List;

/**
 * Flow step interface.
 */
public interface Step {
    String getName();
    String getPolicy();
    String getDescription();
    String getConfiguration();
    boolean isEnabled();
    List<Plugin> getPlugins();
    String getCondition();
    String getMessageCondition();

    void setConfiguration(String configuration);
}
