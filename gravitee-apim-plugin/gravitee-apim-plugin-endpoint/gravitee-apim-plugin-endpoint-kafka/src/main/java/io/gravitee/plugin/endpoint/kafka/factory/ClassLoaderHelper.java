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
package io.gravitee.plugin.endpoint.kafka.factory;

import java.util.function.Supplier;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.kafka.common.utils.Utils;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ClassLoaderHelper {

    /**
     * In order to properly manage class loading while dealing with kafka, we need to erase the current thread classloader to force Kafka to use correct one instead of node class loader
     * See {@link Utils#getContextOrKafkaClassLoader}
     *
     * @param function use to create the Kafka object
     * @param classLoaderToUse the classloader used as replacement
     * @param <T> reference to the kafka object class
     * @return Kafka object
     */
    public static <T> T switchCCL(final Supplier<T> function, final ClassLoader classLoaderToUse) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        try {
            Thread.currentThread().setContextClassLoader(classLoaderToUse);
            return function.get();
        } finally {
            Thread.currentThread().setContextClassLoader(classLoader);
        }
    }
}
