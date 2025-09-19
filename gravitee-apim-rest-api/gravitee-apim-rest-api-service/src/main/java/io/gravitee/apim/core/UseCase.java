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
 * This annotation is used to mark a class as a Use Case.
 *
 * <p>
 * A Use Case is a class that describes a list of actions that a system should perform in order to achieve a particular goal.
 * </p>
 * <p>
 * Rules that applies to Use Cases, in APIM context, are the following:
 *     <ul>
 *         <li>Use Case can't reference another Use Case</li>
 *         <li>Use Case matches to an action that can be done by a user (or System)</li>
 *        <li>Other rules are enforced by ArchUnit {@link architecture.UseCaseRulesTest}</li>
 *      <ul>
 * </p>
 * <p>
 *      ⚠️ This annotation relies on Spring annotation which is an exception to the rule of not using Framework in core.
 * </p>
 * <p>
 *      ⚠️ Transactions only supported when using JDBC repositories.
 * </p>
 *
 * @see <a href="https://gravitee.slab.com/posts/%E2%9A%92%EF%B8%8F-better-software-architecture-of-the-management-api-yxtcvazk">https://gravitee.slab.com/posts/%E2%9A%92%EF%B8%8F-better-software-architecture-of-the-management-api-yxtcvazk</a>
 * @see <a href="https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html">https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html</a>
 * @see <a href="https://nanosoft.co.za/blog/post/clean-architecture-use-cases">https://nanosoft.co.za/blog/post/clean-architecture-use-cases</a>
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Transactional(value = "graviteeTransactionManager", noRollbackFor = AbstractManagementException.class)
public @interface UseCase {}
