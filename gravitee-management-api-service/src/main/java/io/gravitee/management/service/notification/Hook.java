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
package io.gravitee.management.service.notification;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.gravitee.management.service.jackson.ser.HookSerializer;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com) 
 * @author GraviteeSource Team
 */
@JsonSerialize(using = HookSerializer.class)
public interface Hook {
    String name();
    String getLabel();
    String getDescription();
    String getCategory();
    HookScope getScope();
    default boolean isHidden() {
        return false;
    }
}
