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
package io.gravitee.management.service.jackson.filter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import io.gravitee.management.model.ApiEntity;
import io.gravitee.management.model.ApiPermissionsAllowed;

import java.util.Arrays;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiPermissionFilter extends SimpleBeanPropertyFilter {

    @Override
    public void serializeAsField(Object pojo, JsonGenerator jgen, SerializerProvider provider, PropertyWriter writer) throws Exception {
        if (includeField(pojo, writer)) {
            writer.serializeAsField(pojo, jgen, provider);
        }
    }

    private boolean includeField(Object pojo, PropertyWriter writer) {
//        if (writer.getAnnotation(ApiPermissionsAllowed.class)  == null) {
//            return true;
//        }
//
//        return Arrays.asList(writer.getAnnotation(ApiPermissionsAllowed.class).value())
//                .stream()
//                .anyMatch(apiPermission -> apiPermission.equals(((ApiEntity) pojo).getrgetPermission()));
        return true;
    }
}
