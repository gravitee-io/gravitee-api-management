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
package io.gravitee.rest.api.service.impl;

import static java.util.function.Predicate.not;

import io.gravitee.apim.core.template.TemplateProcessor;
import io.gravitee.apim.core.template.TemplateProcessorException;
import io.gravitee.apim.core.utils.CollectionUtils;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.service.EmailRecipientsService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class EmailRecipientsServiceImpl implements EmailRecipientsService {

    public static final Pattern SPLIT_PATTERN = Pattern.compile("[,;\\s]");
    private final TemplateProcessor templateProcessor;
    private final UserService userService;

    @Override
    public Set<String> processTemplatedRecipients(Collection<String> templatedRecipientsEmail, final Map<String, Object> templateData) {
        return templatedRecipientsEmail
            .stream()
            .flatMap(splittableRecipientsStr ->
                Arrays.stream(SPLIT_PATTERN.split(splittableRecipientsStr))
                    .filter(not(String::isEmpty))
                    .map(recipient -> {
                        if (recipient.contains("$")) {
                            try {
                                return Optional.ofNullable(templateProcessor.processInlineTemplate(recipient, templateData));
                            } catch (TemplateProcessorException e) {
                                log.error("Error while processing template '{}' skipping this email", recipient, e);
                                return Optional.<String>empty();
                            }
                        }
                        return Optional.of(recipient);
                    })
            )
            .flatMap(Optional::stream)
            .filter(not(StringUtils::isEmpty))
            .collect(Collectors.toSet());
    }

    @Override
    public Set<String> filterRegisteredUser(ExecutionContext executionContext, Collection<String> recipientsEmail) {
        return recipientsEmail
            .stream()
            .filter(email -> {
                List<UserEntity> users = userService.findByEmail(executionContext, email);
                return CollectionUtils.isNotEmpty(users) && users.size() == 1 && users.getFirst().optedIn();
            })
            .collect(Collectors.toSet());
    }
}
