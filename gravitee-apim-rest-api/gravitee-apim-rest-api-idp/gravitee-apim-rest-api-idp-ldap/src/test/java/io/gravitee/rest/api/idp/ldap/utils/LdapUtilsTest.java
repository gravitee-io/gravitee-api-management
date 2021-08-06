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
package io.gravitee.rest.api.idp.ldap.utils;

import io.gravitee.rest.api.idp.ldap.utils.LdapUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class LdapUtilsTest {

    @Test
    public void shouldNotExtractAttribute_nullParameter() {
        String attribute = LdapUtils.extractAttribute(null);
        Assert.assertNull(attribute);
    }

    @Test
    public void shouldNotExtractAttribute_emptyParameter() {
        String attribute = LdapUtils.extractAttribute("");
        Assert.assertNull(attribute);
    }

    @Test
    public void shouldNotExtractAttribute_invalidFilter() {
        String attribute = LdapUtils.extractAttribute("(&(objectClass=Person)(uid=");
        Assert.assertNull(attribute);
    }

    @Test
    public void shouldExtractAttribute() {
        String attribute = LdapUtils.extractAttribute("(&(objectClass=Person)(uid={0})(&(ou=People)(ou=Payroll)))");
        Assert.assertEquals("uid", attribute);
    }

    @Test
    public void shouldExtractAttribute2() {
        String attribute = LdapUtils.extractAttribute("uid={0},ou=people");
        Assert.assertEquals("uid", attribute);
    }

    @Test
    public void shouldExtractAttribute3() {
        String attribute = LdapUtils.extractAttribute("ou=people,uid={0}");
        Assert.assertEquals("uid", attribute);
    }

    @Test
    public void shouldExtractAttribute4() {
        String attribute = LdapUtils.extractAttribute("(ou=people,uid={0})");
        Assert.assertEquals("uid", attribute);
    }
}
