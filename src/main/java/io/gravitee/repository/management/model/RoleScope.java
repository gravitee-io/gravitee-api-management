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
package io.gravitee.repository.management.model;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com) 
 * @author GraviteeSource Team
 */
public enum RoleScope {
    MANAGEMENT(1),
    PORTAL(2),
    API(3),
    APPLICATION(4),
    GROUP(5);

    private int id;

    RoleScope(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static RoleScope valueOf(int id) {
        Optional<RoleScope> scope = Stream.of(RoleScope.values()).filter(r -> r.getId() == id).findFirst();
        if (scope.isPresent()) {
            return scope.get();
        } else {
            throw new IllegalArgumentException(id + " not a RoleScope id");
        }
    }
}
