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
package io.gravitee.repository.mongodb.management.internal.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com) 
 * @author GraviteeSource Team
 */
public class RolePkMongo implements Serializable {
    private int scope;
    private String name;

    public RolePkMongo() {
    }

    public RolePkMongo(int scope, String name) {
        this.scope = scope;
        this.name = name;
    }


    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public int getScope() {
        return scope;
    }
    public void setScope(int scope) {
        this.scope = scope;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RolePkMongo)) return false;
        RolePkMongo roleMongo = (RolePkMongo) o;
        return Objects.equals(name, roleMongo.name) && 
                Objects.equals(scope, roleMongo.scope);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scope, name);
    }

    @Override
    public String toString() {
        return "RolePkMongo{" +
                "name='" + name + '\'' +
                ", scope='" + scope + '\'' +
                '}';
    }
}
