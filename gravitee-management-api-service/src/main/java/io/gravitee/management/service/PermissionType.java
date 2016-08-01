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
package io.gravitee.management.service;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public enum PermissionType {

    VIEW_API(Scope.API),
    EDIT_API(Scope.API),
    DELETE_API(Scope.API),

    VIEW_APPLICATION(Scope.APPLICATION),
    EDIT_APPLICATION(Scope.APPLICATION),
    DELETE_APPLICATION(Scope.APPLICATION);

    private Scope scope;

    PermissionType(Scope scope) {
        this.scope = scope;
    }

    public Scope scope() {
        return this.scope;
    }

    public enum Scope {
        API,
        APPLICATION
    }
}
