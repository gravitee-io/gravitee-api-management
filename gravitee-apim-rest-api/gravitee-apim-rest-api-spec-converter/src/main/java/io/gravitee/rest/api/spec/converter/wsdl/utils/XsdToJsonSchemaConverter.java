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
package io.gravitee.rest.api.spec.converter.wsdl.utils;

import io.swagger.v3.oas.models.media.Schema;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.xmlbeans.SchemaLocalElement;
import org.apache.xmlbeans.SchemaParticle;
import org.apache.xmlbeans.SchemaType;

/**
 * Converts XSD schema types to OpenAPI JSON Schema representations.
 * Used to generate request body schemas from WSDL input message parts.
 */
public class XsdToJsonSchemaConverter {

    private static final String TYPE_OBJECT = "object";
    private static final String TYPE_STRING = "string";
    private static final String TYPE_BOOLEAN = "boolean";
    private static final String TYPE_NUMBER = "number";
    private static final String TYPE_INTEGER = "integer";
    private static final String FORMAT_DATE = "date";
    private static final String FORMAT_DATE_TIME = "date-time";

    private static final Set<Integer> BOOLEAN_TYPES = Set.of(SchemaType.BTC_BOOLEAN);

    private static final Set<Integer> NUMBER_TYPES = Set.of(SchemaType.BTC_FLOAT, SchemaType.BTC_DOUBLE, SchemaType.BTC_DECIMAL);

    private static final Set<Integer> INTEGER_TYPES = Set.of(
        SchemaType.BTC_INTEGER,
        SchemaType.BTC_LONG,
        SchemaType.BTC_INT,
        SchemaType.BTC_SHORT,
        SchemaType.BTC_BYTE,
        SchemaType.BTC_NON_NEGATIVE_INTEGER,
        SchemaType.BTC_POSITIVE_INTEGER,
        SchemaType.BTC_NON_POSITIVE_INTEGER,
        SchemaType.BTC_NEGATIVE_INTEGER,
        SchemaType.BTC_UNSIGNED_LONG,
        SchemaType.BTC_UNSIGNED_INT,
        SchemaType.BTC_UNSIGNED_SHORT,
        SchemaType.BTC_UNSIGNED_BYTE
    );

    private static final Set<Integer> DATE_TYPES = Set.of(SchemaType.BTC_DATE);

    private static final Set<Integer> DATE_TIME_TYPES = Set.of(SchemaType.BTC_DATE_TIME);

    private XsdToJsonSchemaConverter() {}

    /**
     * Converts an XSD document type (wrapping a complex type) to an OpenAPI JSON Schema.
     */
    @SuppressWarnings("rawtypes")
    public static Schema<?> convert(SchemaType documentType) {
        if (documentType == null) {
            return new Schema().type(TYPE_OBJECT);
        }

        SchemaType contentType = documentType;
        if (documentType.getContentModel() != null && documentType.getContentModel().getParticleType() == SchemaParticle.ELEMENT) {
            contentType = ((SchemaLocalElement) documentType.getContentModel()).getType();
        }

        return convertType(contentType);
    }

    @SuppressWarnings("rawtypes")
    private static Schema<?> convertType(SchemaType stype) {
        if (stype == null) {
            return new Schema().type(TYPE_OBJECT);
        }

        if (stype.isSimpleType() || stype.isURType()) {
            return mapSimpleType(stype);
        }

        return switch (stype.getContentType()) {
            case SchemaType.SIMPLE_CONTENT -> mapSimpleType(stype);
            case SchemaType.ELEMENT_CONTENT, SchemaType.MIXED_CONTENT -> stype.getContentModel() != null
                ? convertParticle(stype.getContentModel())
                : new Schema().type(TYPE_OBJECT);
            default -> new Schema().type(TYPE_OBJECT);
        };
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static Schema<?> convertParticle(SchemaParticle sp) {
        return switch (sp.getParticleType()) {
            case SchemaParticle.SEQUENCE, SchemaParticle.ALL, SchemaParticle.CHOICE -> buildObjectSchema(sp);
            case SchemaParticle.ELEMENT -> convertType(((SchemaLocalElement) sp).getType());
            default -> new Schema().type(TYPE_OBJECT);
        };
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static Schema<?> buildObjectSchema(SchemaParticle sp) {
        Schema schema = new Schema().type(TYPE_OBJECT);
        schema.setProperties(collectElementProperties(sp));

        List<String> required = collectRequiredProperties(sp);
        if (!required.isEmpty()) {
            schema.setRequired(required);
        }

        return schema;
    }

    @SuppressWarnings("rawtypes")
    private static Map<String, Schema> collectElementProperties(SchemaParticle sp) {
        return Arrays.stream(sp.getParticleChildren())
            .filter(child -> child.getParticleType() == SchemaParticle.ELEMENT)
            .collect(
                Collectors.toMap(
                    child -> ((SchemaLocalElement) child).getName().getLocalPart(),
                    child -> convertType(((SchemaLocalElement) child).getType()),
                    (a, b) -> a,
                    LinkedHashMap::new
                )
            );
    }

    private static List<String> collectRequiredProperties(SchemaParticle sp) {
        return Arrays.stream(sp.getParticleChildren())
            .filter(child -> child.getParticleType() == SchemaParticle.ELEMENT)
            .filter(child -> child.getIntMinOccurs() >= 1)
            .map(child -> ((SchemaLocalElement) child).getName().getLocalPart())
            .collect(Collectors.toCollection(ArrayList::new));
    }

    @SuppressWarnings("rawtypes")
    private static Schema<?> mapSimpleType(SchemaType stype) {
        if (stype == null) {
            return new Schema().type(TYPE_STRING);
        }

        int typeCode = getBuiltinTypeCode(stype);
        if (BOOLEAN_TYPES.contains(typeCode)) {
            return new Schema().type(TYPE_BOOLEAN);
        }
        if (NUMBER_TYPES.contains(typeCode)) {
            return new Schema().type(TYPE_NUMBER);
        }
        if (INTEGER_TYPES.contains(typeCode)) {
            return new Schema().type(TYPE_INTEGER);
        }
        if (DATE_TYPES.contains(typeCode)) {
            return new Schema().type(TYPE_STRING).format(FORMAT_DATE);
        }
        if (DATE_TIME_TYPES.contains(typeCode)) {
            return new Schema().type(TYPE_STRING).format(FORMAT_DATE_TIME);
        }
        return new Schema().type(TYPE_STRING);
    }

    private static int getBuiltinTypeCode(SchemaType stype) {
        SchemaType current = stype;
        while (current != null && !current.isBuiltinType()) {
            current = current.getBaseType();
        }
        return current != null ? current.getBuiltinTypeCode() : SchemaType.BTC_STRING;
    }
}
