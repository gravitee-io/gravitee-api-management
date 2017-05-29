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
package io.gravitee.management.model.permissions;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com) 
 * @author GraviteeSource Team
 */
public enum ManagementPermission implements Permission {
    INSTANCE(   "INSTANCE",    1000),
    VIEW(       "VIEW",        1100),
    GROUP(      "GROUP",       1200),
    TAG(        "TAG",         1300),
    TENANT(     "TENANT",      1400),
    API(        "API",         1500),
    ROLE(       "ROLE",        1600),
    APPLICATION("APPLICATION", 1700),
    PLATFORM(   "PLATFORM",    1800);

    String name;
    int mask;

    ManagementPermission(String name, int mask) {
        this.name = name;
        this.mask = mask;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getMask() {
        return mask;
    }

}
