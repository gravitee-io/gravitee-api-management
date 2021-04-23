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
package io.gravitee.rest.api.service.jackson.ser.api;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.gravitee.rest.api.model.api.ApiEntity;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiCompositeSerializer extends ApiSerializer implements InitializingBean {

    @Autowired
    private ApplicationContext applicationContext;

    private List<ApiSerializer> serializers = new LinkedList<>();

    public ApiCompositeSerializer() {
        super(ApiEntity.class);
    }

    @Override
    public boolean canHandle(ApiEntity apiEntity) {
        return true;
    }

    @Override
    public void serialize(ApiEntity apiEntity, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        serializers
            .stream()
            .filter(serializer -> serializer.canHandle(apiEntity))
            .findFirst()
            // fall back to default
            .orElse(serializers.get(0))
            .serialize(apiEntity, jsonGenerator, serializerProvider);
    }

    @Override
    public Version version() {
        return null;
    }

    @Override
    public void afterPropertiesSet() {
        addSerializer(new ApiDefaultSerializer());
        addSerializer(new Api1_15VersionSerializer());
        addSerializer(new Api1_20VersionSerializer());
        addSerializer(new Api1_25VersionSerializer());
        addSerializer(new Api3_0VersionSerializer());
    }

    private void addSerializer(ApiSerializer apiSerializer) {
        apiSerializer.setApplicationContext(applicationContext);
        serializers.add(apiSerializer);
    }

    public void setSerializers(List<ApiSerializer> serializers) {
        this.serializers = serializers;
    }
}
