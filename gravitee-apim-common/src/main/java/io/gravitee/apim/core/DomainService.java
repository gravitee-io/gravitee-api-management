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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to mark a class as a Domain Service
 *
 * <p>
 *     A Domain Service is a class that will perform business logic that can be both targeted by Use Cases, or other
 *     Domain Services.
 * </p>
 *
 * <p>
 *      Most of the time, we can't have different implementation and therefore defining an Interface can be skipped. <br />
 *      However, an interface can be useful in APIM context when existing code (Legacy) already define the logic and we don't want to spend time to extract in regular Domain Service yet.
 * </p>
 *
 * <p>
 *     An example: revoking an API key can be done in a Use Case (action performed on an API call), or it can be done
 *     when performing another use case (for instance when closing a subscription). We can create a
 *     <code>RevokeApiKeyDomainService</code> that will be called by the <code>RevokeApiKeyUseCase</code>, and <code>CloseSubscriptionUseCase</code>
 * </p>
 *
 * <p>NOTE: In Craft literature, we can also see Application Services. We choose to not introduce the term (yet?). <br />
 * Our main focus for now is to split business code from infrastructure code.</p>
 *
 * @see <a href="https://gravitee.slab.com/posts/%E2%9A%92%EF%B8%8F-better-software-architecture-of-the-management-api-yxtcvazk">https://gravitee.slab.com/posts/%E2%9A%92%EF%B8%8F-better-software-architecture-of-the-management-api-yxtcvazk</a>
 * @see <a href="https://enterprisecraftsmanship.com/posts/domain-vs-application-services/">https://enterprisecraftsmanship.com/posts/domain-vs-application-services/</a>
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface DomainService {}
