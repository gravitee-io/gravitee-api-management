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
package io.gravitee.rest.api.management.v2.rest.provider;

import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.ParamConverters;
import org.glassfish.jersey.internal.inject.Providers;

/**
 * This {@link ParamConverterProvider} brings support for multi-value query parameters with comma-separated style.
 * It currently supports query parameter type {@link Set} and {@link List}. When annotated with {@link Nonnull} and no parameter is supplied, empty collection is returned instead od <code>null</code>.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@Provider
@SuppressWarnings({ "rawtypes", "unchecked" })
public class CommaSeparatedQueryParamConverterProvider implements ParamConverterProvider {

    @Inject
    private InjectionManager injectionManager;

    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
        if (List.class.isAssignableFrom(rawType) || Set.class.isAssignableFrom(rawType)) {
            final boolean nonNull = Stream.of(annotations).anyMatch(annotation -> Nonnull.class == annotation.annotationType());

            final Type actualTypeArgument = ((ParameterizedType) genericType).getActualTypeArguments()[0];
            var providers = Providers.getProviders(injectionManager, ParamConverterProvider.class);

            for (ParamConverterProvider provider : providers) {
                var converter = provider.getConverter((Class<?>) actualTypeArgument, genericType, annotations);
                if (converter != null) {
                    return (ParamConverter<T>) new CommaSeparatedQueryParamConverter(collectorFromRawType(rawType), converter, nonNull);
                }
            }
        }

        return null;
    }

    private java.util.stream.Collector<?, ?, ?> collectorFromRawType(Class<?> rawType) {
        if (List.class.isAssignableFrom(rawType)) {
            return Collectors.toList();
        }

        return Collectors.toSet();
    }

    @AllArgsConstructor
    private static class CommaSeparatedQueryParamConverter<U extends Type, T extends Collection<U>> implements ParamConverter<T> {

        private static final Pattern SEPARATOR_PATTERN = Pattern.compile(",\\s*");

        private final java.util.stream.Collector<U, ?, T> collector;
        private final ParamConverter<U> converter;
        private final boolean nonNull;

        @Override
        public T fromString(final String value) {
            if (value == null || value.isBlank()) {
                if (nonNull) {
                    return Stream.<U>empty().collect(collector);
                }
                return null;
            }
            return Stream.of(SEPARATOR_PATTERN.split(value)).map(converter::fromString).collect(collector);
        }

        @Override
        public String toString(final T value) {
            return value.stream().map(Object::toString).collect(Collectors.joining(","));
        }
    }
}
