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
package io.gravitee.repository.jdbc.orm;

import static org.springframework.util.StringUtils.capitalize;

import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author njt
 */
public class JdbcColumn {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcColumn.class);

    public final String name;
    public final int jdbcType;
    public final Class javaType;
    public final Method getter;
    public final Method setter;

    JdbcColumn(String name, int jdbcType, Class owningClass, Class fieldType) {
        this.name = getAccessorName(name);
        this.jdbcType = jdbcType;
        this.javaType = fieldType;
        String getterName = "get";
        if (fieldType == boolean.class) {
            getterName = "is";
        }
        getterName += this.name;

        try {
            this.getter = owningClass.getMethod(getterName);
        } catch (NoSuchMethodException ex) {
            LOGGER.error("Method {} not found: ", getterName, ex);
            throw new IllegalStateException("Method " + owningClass.getSimpleName() + "." + getterName + " not found", ex);
        }
        try {
            this.setter = owningClass.getMethod("set" + this.name, fieldType);
        } catch (NoSuchMethodException ex) {
            throw new IllegalStateException(
                "Method " + owningClass.getSimpleName() + ".set" + this.name + "( " + fieldType.getSimpleName() + ") not found",
                ex
            );
        }
    }

    private static String getAccessorName(final String name) {
        final StringBuffer sb = new StringBuffer();
        final Matcher m = Pattern.compile("_(\\w)").matcher(capitalize(name));
        while (m.find()) {
            m.appendReplacement(sb, m.group(1).toUpperCase());
        }
        return m.appendTail(sb).toString();
    }

    public static String getDBName(final String name) {
        final StringBuffer sb = new StringBuffer();
        final Matcher m = Pattern.compile("([A-Z])").matcher(name);
        while (m.find()) {
            m.appendReplacement(sb, (m.start() == 0 ? "" : '_') + m.group(1).toLowerCase());
        }
        return m.appendTail(sb).toString();
    }
}
