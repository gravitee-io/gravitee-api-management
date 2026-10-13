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
package io.gravitee.apim.rest.api.mcp.core.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.rest.api.mcp.core.EnvironmentResolver;
import io.gravitee.apim.rest.api.mcp.core.TriageException;
import io.gravitee.apim.rest.api.mcp.core.model.ApiRef;
import io.gravitee.apim.rest.api.mcp.core.model.EnvRef;
import io.gravitee.apim.rest.api.mcp.core.model.Notification;
import io.gravitee.apim.rest.api.mcp.core.port.ApiDirectory;
import io.gravitee.apim.rest.api.mcp.core.port.EnvironmentDirectory;
import io.gravitee.apim.rest.api.mcp.core.port.Messaging;
import io.gravitee.apim.rest.api.mcp.core.port.TriageAccess;
import io.gravitee.apim.rest.api.mcp.spi.McpCaller;
import java.util.List;

/**
 * Tells the people around an API what the caller decided to tell them. A dry run unless confirmed: the first
 * answer is who would be reached and with what, and nothing goes out.
 */
@UseCase
public class NotifyUseCase {

    private final EnvironmentDirectory environments;
    private final TriageAccess access;
    private final ApiDirectory apis;
    private final Messaging messaging;

    public NotifyUseCase(EnvironmentDirectory environments, TriageAccess access, ApiDirectory apis, Messaging messaging) {
        this.environments = environments;
        this.access = access;
        this.apis = apis;
        this.messaging = messaging;
    }

    public record Input(
        McpCaller caller,
        String environment,
        String api,
        String audience,
        String channel,
        String subject,
        String text,
        String webhookUrl,
        Boolean confirm
    ) {}

    /**
     * @param dryRun true when nothing was sent
     * @param sent how many recipients the message went to; null on a dry run
     */
    public record Output(
        String summary,
        EnvRef environment,
        ApiRef api,
        String audience,
        String channel,
        boolean dryRun,
        List<Messaging.Recipient> recipients,
        String subject,
        String text,
        Integer sent
    ) {}

    public Output execute(Input input) {
        if (input.api() == null || input.api().isBlank()) {
            throw new TriageException("api is required: the API whose consumers to notify.");
        }
        if (input.text() == null || input.text().isBlank()) {
            throw new TriageException("text is required: what to tell them.");
        }
        var audience = Notification.Audience.parse(input.audience());
        var channel = Notification.Channel.parse(input.channel());
        if ((channel == Notification.Channel.WEBHOOK) != (audience == Notification.Audience.WEBHOOK)) {
            throw new TriageException("Channel webhook goes with audience webhook, and only with it.");
        }
        if (channel == Notification.Channel.WEBHOOK && (input.webhookUrl() == null || input.webhookUrl().isBlank())) {
            throw new TriageException("webhookUrl is required for the webhook channel.");
        }
        var scope = EnvironmentResolver.resolve(environments, apis, input.caller(), input.environment(), input.api());
        var auditInfo = scope.auditInfo();
        var api = scope.api();
        if (!access.canNotify(auditInfo, api.id())) {
            throw new TriageException("You are not allowed to message the consumers of " + api.name() + ".");
        }
        var notification = new Notification(audience, channel, input.subject(), input.text(), input.webhookUrl());
        var recipients = messaging.recipients(auditInfo, api.id(), notification);
        boolean confirmed = Boolean.TRUE.equals(input.confirm());

        if (!confirmed) {
            var who = channel == Notification.Channel.WEBHOOK ? "the webhook" : recipients.size() + " person(s)";
            return new Output(
                "Dry run: this would reach " +
                    who +
                    " over " +
                    channel.name().toLowerCase() +
                    ". Nothing was sent. Call again with confirm=true to send.",
                scope.environment(),
                api,
                audience.name(),
                channel.name(),
                true,
                recipients,
                input.subject(),
                input.text(),
                null
            );
        }
        if (channel != Notification.Channel.WEBHOOK && recipients.isEmpty()) {
            throw new TriageException(
                "Nobody to notify: the audience " + audience.name().toLowerCase() + " of " + api.name() + " is empty."
            );
        }
        int sent = messaging.send(auditInfo, api.id(), notification);
        return new Output(
            "Sent to " + sent + " recipient(s) over " + channel.name().toLowerCase() + ", audited under your name.",
            scope.environment(),
            api,
            audience.name(),
            channel.name(),
            false,
            recipients,
            input.subject(),
            input.text(),
            sent
        );
    }
}
