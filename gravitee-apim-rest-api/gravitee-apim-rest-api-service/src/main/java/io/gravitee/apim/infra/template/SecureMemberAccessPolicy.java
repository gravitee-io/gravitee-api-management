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
package io.gravitee.apim.infra.template;

import freemarker.ext.beans.ClassMemberAccessPolicy;
import freemarker.ext.beans.MemberAccessPolicy;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.Set;

public final class SecureMemberAccessPolicy implements MemberAccessPolicy {

    @Override
    public ClassMemberAccessPolicy forClass(Class<?> clazz) {
        return new SecureClassPolicy();
    }

    @Override
    public boolean isToStringAlwaysExposed() {
        return true;
    }

    private static final class SecureClassPolicy implements ClassMemberAccessPolicy {

        private static final Set<String> FORBIDDEN_METHODS = Set.of(
            "getClass",
            "getClassLoader",
            "getContextClassLoader",
            "getProtectionDomain",
            "forName",
            "newInstance"
        );
        private static final Set<Class<?>> FORBIDDEN_TYPES = Set.of(
            Class.class,
            ClassLoader.class,
            ProtectionDomain.class,
            Thread.class,
            Member.class
        );

        @Override
        public boolean isMethodExposed(Method method) {
            if (method.getDeclaringClass() == Object.class) {
                return "equals".equals(method.getName()) || "hashCode".equals(method.getName());
            }

            if (FORBIDDEN_METHODS.contains(method.getName())) {
                return false;
            }

            Class<?> returnType = method.getReturnType();
            return FORBIDDEN_TYPES.stream().noneMatch(type -> type.isAssignableFrom(returnType));
        }

        @Override
        public boolean isConstructorExposed(Constructor<?> constructor) {
            return false;
        }

        @Override
        public boolean isFieldExposed(Field field) {
            return false;
        }
    }
}
