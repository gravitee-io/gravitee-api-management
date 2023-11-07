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
package io.gravitee.apim.core;

import io.gravitee.rest.api.service.exceptions.AbstractManagementException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.transaction.annotation.Transactional;

/**
 * Annotation that can be used on UseCase classes to make them transactional.
 * <p>
 *      ⚠️ This annotation rely on Spring annotation which is an exception to the rule of not using Framework in core.
 * </p>
 * <p>
 *      ⚠️ Transactions only supported when using JDBC repositories.
 * </p>
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Transactional(value = "graviteeTransactionManager", noRollbackFor = AbstractManagementException.class)
public @interface TransactionalUseCase {
}
