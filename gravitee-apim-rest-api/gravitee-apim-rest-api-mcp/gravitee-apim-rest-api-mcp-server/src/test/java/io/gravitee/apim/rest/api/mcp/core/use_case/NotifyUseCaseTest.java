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

import static io.gravitee.apim.rest.api.mcp.fakes.Fakes.MCP_CALLER;
import static io.gravitee.apim.rest.api.mcp.fakes.Fakes.PROD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.apim.rest.api.mcp.core.TriageException;
import io.gravitee.apim.rest.api.mcp.core.model.ApiRef;
import io.gravitee.apim.rest.api.mcp.core.model.Notification;
import io.gravitee.apim.rest.api.mcp.core.port.Messaging;
import io.gravitee.apim.rest.api.mcp.fakes.Fakes;
import java.util.List;
import org.junit.jupiter.api.Test;

class NotifyUseCaseTest {

    private static final ApiRef ORDERS = new ApiRef("orders-id", "Orders");

    private final Fakes.Access access = new Fakes.Access();
    private final Fakes.StubMessaging messaging = new Fakes.StubMessaging();
    private final NotifyUseCase useCase = new NotifyUseCase(Fakes.environments(PROD), access, Fakes.apis(ORDERS), messaging);

    @Test
    void should_only_say_who_would_be_reached_unless_confirmed() {
        var output = execute("consumer_owners", "mail", "The key was revoked", null, null);

        assertThat(output.dryRun()).isTrue();
        assertThat(output.sent()).isNull();
        assertThat(output.recipients()).containsExactly(new Messaging.Recipient("jane-id", "Jane Owner", "jane@acme.example"));
        assertThat(output.summary()).startsWith("Dry run: this would reach 1 person(s) over mail. Nothing was sent.");
        assertThat(output.api()).isEqualTo(ORDERS);
        assertThat(messaging.sent).isNull();
    }

    @Test
    void should_send_when_confirmed_and_report_the_count() {
        var output = execute("consumer_owners", "portal", "The key was revoked", null, true);

        assertThat(output.dryRun()).isFalse();
        assertThat(output.sent()).isEqualTo(1);
        assertThat(messaging.sentApiId).isEqualTo("orders-id");
        assertThat(messaging.sent).isEqualTo(
            new Notification(Notification.Audience.CONSUMER_OWNERS, Notification.Channel.PORTAL, "Orders", "The key was revoked", null)
        );
        assertThat(output.summary()).startsWith("Sent to 1 recipient(s) over portal");
    }

    @Test
    void should_refuse_to_send_to_nobody() {
        messaging.recipients = List.of();

        assertThatThrownBy(() -> execute("subscribers", "mail", "text", null, true))
            .isInstanceOf(TriageException.class)
            .hasMessageContaining("Nobody to notify");
    }

    @Test
    void should_post_to_a_webhook_without_resolving_people() {
        var output = execute("webhook", "webhook", "text", "https://hooks.slack.example/T0/B0/x", true);

        assertThat(output.recipients()).isEmpty();
        assertThat(output.sent()).isEqualTo(1);
        assertThat(messaging.sent.webhookUrl()).isEqualTo("https://hooks.slack.example/T0/B0/x");
    }

    @Test
    void should_require_a_url_for_a_webhook_and_pair_the_webhook_audience_with_its_channel() {
        assertThatThrownBy(() -> execute("webhook", "webhook", "text", null, null))
            .isInstanceOf(TriageException.class)
            .hasMessageContaining("webhookUrl is required");
        assertThatThrownBy(() -> execute("consumer_owners", "webhook", "text", "https://hooks.slack.example/x", null))
            .isInstanceOf(TriageException.class)
            .hasMessageContaining("audience webhook");
    }

    @Test
    void should_refuse_a_caller_who_may_not_message_the_consumers() {
        access.notify = false;

        assertThatThrownBy(() -> execute("consumer_owners", "mail", "text", null, null))
            .isInstanceOf(TriageException.class)
            .hasMessageContaining("not allowed to message the consumers of Orders");
    }

    @Test
    void should_name_the_accepted_audiences_and_channels() {
        assertThatThrownBy(() -> execute("everyone", "mail", "text", null, null))
            .isInstanceOf(TriageException.class)
            .hasMessage("Invalid audience 'everyone'. Use one of: consumer_owners, consumer_members, subscribers, webhook.");
        assertThatThrownBy(() -> execute("consumer_owners", "sms", "text", null, null))
            .isInstanceOf(TriageException.class)
            .hasMessage("Invalid channel 'sms'. Use one of: portal, mail, webhook.");
    }

    private NotifyUseCase.Output execute(String audience, String channel, String text, String url, Boolean confirm) {
        return useCase.execute(new NotifyUseCase.Input(MCP_CALLER, null, "Orders", audience, channel, "Orders", text, url, confirm));
    }
}
