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
package io.gravitee.apim.infra.json.jackson.module;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.endpointgroup.loadbalancer.LoadBalancerType;
import io.gravitee.definition.model.v4.flow.execution.FlowMode;
import io.gravitee.definition.model.v4.flow.selector.SelectorType;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.definition.model.v4.listener.entrypoint.Qos;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import java.io.IOException;

/**
 * Jackson module to serialize Gravitee Definition.
 *
 * <p>
 *     The result is matching what the Rest API returns when exporting an API.
 * </p>
 */
public class GraviteeDefinitionJacksonModule extends SimpleModule {

    public GraviteeDefinitionJacksonModule() {
        super();
        addSerializer(ApiType.class, new ApiTypeSerializer(ApiType.class));
        addSerializer(DefinitionVersion.class, new DefinitionVersionSerializer(DefinitionVersion.class));
        addSerializer(FlowMode.class, new FlowModeSerializer(FlowMode.class));
        addSerializer(ListenerType.class, new ListenerTypeSerializer(ListenerType.class));
        addSerializer(LoadBalancerType.class, new LoadBalancerTypeSerializer(LoadBalancerType.class));
        addSerializer(PlanMode.class, new PlanModeSerializer(PlanMode.class));
        addSerializer(PlanStatus.class, new PlanStatusSerializer(PlanStatus.class));
        addSerializer(Qos.class, new QosSerializer(Qos.class));
        addSerializer(SelectorType.class, new SelectorTypeSerializer(SelectorType.class));
    }

    /**
     * Custom serializer for {@link ListenerType} enum to serialize it as a string using the enum Name.
     */
    public static class ListenerTypeSerializer extends StdScalarSerializer<ListenerType> {

        public ListenerTypeSerializer(Class<ListenerType> t) {
            super(t);
        }

        @Override
        public void serialize(ListenerType value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString(value.name());
        }
    }

    /**
     * Custom serializer for {@link ApiType} enum to serialize it as a string using the enum Name.
     */
    public static class ApiTypeSerializer extends StdScalarSerializer<ApiType> {

        public ApiTypeSerializer(Class<ApiType> t) {
            super(t);
        }

        @Override
        public void serialize(ApiType value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString(value.name());
        }
    }

    /**
     * Custom serializer for {@link io.gravitee.definition.model.v4.listener.entrypoint.Qos} enum to serialize it as a string using the enum Name.
     */
    public static class QosSerializer extends StdScalarSerializer<Qos> {

        public QosSerializer(Class<Qos> t) {
            super(t);
        }

        @Override
        public void serialize(Qos value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString(value.name());
        }
    }

    /**
     * Custom serializer for {@link SelectorType} enum to serialize it as a string using the enum Name.
     */
    public static class SelectorTypeSerializer extends StdScalarSerializer<SelectorType> {

        public SelectorTypeSerializer(Class<SelectorType> t) {
            super(t);
        }

        @Override
        public void serialize(SelectorType value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString(value.name());
        }
    }

    /**
     * Custom serializer for {@link FlowMode} enum to serialize it as a string using the enum Name.
     */
    public static class FlowModeSerializer extends StdScalarSerializer<FlowMode> {

        public FlowModeSerializer(Class<FlowMode> t) {
            super(t);
        }

        @Override
        public void serialize(FlowMode value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString(value.name());
        }
    }

    /**
     * Custom serializer for {@link LoadBalancerType} enum to serialize it as a string using the enum Name.
     */
    public static class LoadBalancerTypeSerializer extends StdScalarSerializer<LoadBalancerType> {

        public LoadBalancerTypeSerializer(Class<LoadBalancerType> t) {
            super(t);
        }

        @Override
        public void serialize(LoadBalancerType value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString(value.name());
        }
    }

    /**
     * Custom serializer for {@link DefinitionVersion} enum to serialize it as a string using the enum Name.
     */
    public static class DefinitionVersionSerializer extends StdScalarSerializer<DefinitionVersion> {

        public DefinitionVersionSerializer(Class<DefinitionVersion> t) {
            super(t);
        }

        @Override
        public void serialize(DefinitionVersion value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString(value.name());
        }
    }

    /**
     * Custom serializer for {@link PlanMode} enum to serialize it as a string using the enum Name.
     */
    public static class PlanModeSerializer extends StdScalarSerializer<PlanMode> {

        public PlanModeSerializer(Class<PlanMode> t) {
            super(t);
        }

        @Override
        public void serialize(PlanMode value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString(value.name());
        }
    }

    /**
     * Custom serializer for {@link PlanStatus} enum to serialize it as a string using the enum Name.
     */
    public static class PlanStatusSerializer extends StdScalarSerializer<PlanStatus> {

        public PlanStatusSerializer(Class<PlanStatus> t) {
            super(t);
        }

        @Override
        public void serialize(PlanStatus value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString(value.name());
        }
    }
}
