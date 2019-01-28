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
package io.gravitee.management.idp.ldap.utils;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public final class LdapUtils {

    private final static String FILTER_VARIABLE = "={0}";
    private final static String PROTECTED_FILTER_VARIABLE = "=\\{0\\}";

    /**
     *  Filter can be uid={0} or mail={0} or may be even more complex like
     *  (&(objectClass=Person)(uid={0})(&(ou=People)(ou=Payroll)))
     *
     * @param filter
     * @return
     */
    public static String extractAttribute(String filter) {
        if (filter == null) {
            return null;
        }

        String [] parts = filter.split(PROTECTED_FILTER_VARIABLE);
        if (parts.length > 1 || (parts.length == 1 && filter.endsWith(FILTER_VARIABLE))) {
            String attribute = parts[0];
            int idxSep = Math.max(attribute.lastIndexOf(','),
                    attribute.lastIndexOf('('));

            int idx = (idxSep == -1) ? 0 : idxSep + 1;
            return attribute.substring(idx);
        }

        return null;
    }
}
