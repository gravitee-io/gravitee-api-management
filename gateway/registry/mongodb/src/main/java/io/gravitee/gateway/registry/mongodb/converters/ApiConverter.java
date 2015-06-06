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
package io.gravitee.gateway.registry.mongodb.converters;

import com.google.common.base.Strings;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import io.gravitee.model.Api;

import java.net.URI;

/**
 * @author Azize Elamrani (azize dot elamrani at gmail dot com)
 */
public class ApiConverter extends AbstractConverter<Api, DBObject> {

    @Override
    public DBObject convertTo(final Api api) {
        if (api == null) {
            return null;
        }
        final DBObject dbObject = new BasicDBObject();
        if (!Strings.isNullOrEmpty(api.getName())) {
            dbObject.put("name", api.getName());
        }
        if (!Strings.isNullOrEmpty(api.getVersion())) {
            dbObject.put("version", api.getVersion());
        }
        if (!Strings.isNullOrEmpty(api.getContextPath())) {
            dbObject.put("contextPath", api.getContextPath());
        }
        if (api.isEnabled() != null) {
            dbObject.put("enabled", api.isEnabled());
        }
        if (api.getTarget() != null) {
            dbObject.put("target", api.getTarget().getPath());
        }
        return dbObject;
    }

    @Override
    public Api convertFrom(final DBObject dbobject) {
        final Api api = new Api();
        final Object name = dbobject.get("name");
        if (name != null) {
            api.setName((String) name);
        }
        final Object version = dbobject.get("version");
        if (version != null) {
            api.setVersion((String) version);
        }
        final Object contextPath = dbobject.get("contextPath");
        if (contextPath != null) {
            api.setContextPath((String) contextPath);
        }
        final Object enabled = dbobject.get("enabled");
        if (enabled != null) {
            api.setEnabled((Boolean) enabled);
        }
        final Object target = dbobject.get("target");
        if (target != null) {
            api.setTarget(URI.create((String) target));
        }
        return api;
    }
}
