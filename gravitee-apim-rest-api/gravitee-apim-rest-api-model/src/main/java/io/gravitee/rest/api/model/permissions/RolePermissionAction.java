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
package io.gravitee.rest.api.model.permissions;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public enum RolePermissionAction {
    CREATE('C', 8), // 1000
    READ('R', 4), // 0100
    UPDATE('U', 2), // 0010
    DELETE('D', 1); // 0001

    char id;
    int mask;

    RolePermissionAction(char id, int mask) {
        this.id = id;
        this.mask = mask;
    }

    public char getId() {
        return id;
    }

    public int getMask() {
        return mask;
    }

    public static RolePermissionAction findById(char id) {
        for (RolePermissionAction rolePermissionAction : RolePermissionAction.values()) {
            if (id == rolePermissionAction.id) {
                return rolePermissionAction;
            }
        }
        throw new IllegalArgumentException(id + " not a RolePermissionAction id");
    }
}
