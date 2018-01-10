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
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com) 
 * @author GraviteeSource Team
 */
public enum ApiPermission implements Permission {
    DEFINITION(         "DEFINITION",           1000),
    PLAN(               "PLAN",                 1100),
    SUBSCRIPTION(       "SUBSCRIPTION",         1200),
    MEMBER(             "MEMBER",               1300),
    METADATA(           "METADATA",             1400),
    ANALYTICS(          "ANALYTICS",            1500),
    EVENT(              "EVENT",                1600),
    HEALTH(             "HEALTH",               1700),
    LOG(                "LOG",                  1800),
    DOCUMENTATION(      "DOCUMENTATION",        1900),
    GATEWAY_DEFINITION( "GATEWAY_DEFINITION",   2000),
    RATING(             "RATING",               2100),
    RATING_ANSWER(      "RATING_ANSWER",        2200),
    AUDIT(              "AUDIT",                2300),
    DISCOVERY(          "DISCOVERY",            2400),
    NOTIFICATION(       "NOTIFICATION",         2500);

    String name;
    int mask;

    ApiPermission(String name, int mask) {
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
