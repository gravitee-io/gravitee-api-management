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
package io.gravitee.rest.api.service;

import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface EmailRecipientsService {
    /**
     * Process a list of templated recipients to extract it as a list of unique email addresses
     *
     * @param templatedRecipientsEmail a list of strings representing templated email. Each string can be itself a literal list of recipients separated by ' ' (whitespace) ',' or ';'. If an element contains '$', then it will be processed against templateData parameter with Freemarker processor
     * @param templateData             is the dateset used to process emails.
     * @return a set of not empty emails.
     */
    Set<String> processTemplatedRecipients(Collection<String> templatedRecipientsEmail, Map<String, Object> templateData);

    /**
     * Checks that each email has an opted-in user attached to it. If it is not the case, then the email is excluded from the result.
     * @param executionContext
     * @param recipientsEmail is the list of recipients to verify if attached user has opted-in. This method assumes emails are valids.
     * @return a set of emails attached to an opted-in user.
     */
    Set<String> filterRegisteredUser(ExecutionContext executionContext, Collection<String> recipientsEmail);
}
