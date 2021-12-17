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
package io.gravitee.rest.api.service;

import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.model.*;
import java.util.Set;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface MessageService {
    /**
     * send a message to api consumers according to recipients filters
     * @param environmentId
     * @param apiId api id
     * @param message message
     * @return the number of recipients
     */
    int create(final String environmentId, String apiId, MessageEntity message);

    /**
     * send a message to all users according to recipients filters
     * @param environmentId
     * @param message message
     * @return the number of recipients
     */
    int create(final String environmentId, MessageEntity message);

    /**
     * get the user ids of recipients
     * @param environment
     * @param api api
     * @param message message
     * @return a user id list
     */
    Set<String> getRecipientsId(final String environment, Api api, MessageEntity message);

    /**
     * get the user ids of recipients
     * @param message message
     * @return a user id list
     */
    Set<String> getRecipientsId(MessageEntity message);
}
