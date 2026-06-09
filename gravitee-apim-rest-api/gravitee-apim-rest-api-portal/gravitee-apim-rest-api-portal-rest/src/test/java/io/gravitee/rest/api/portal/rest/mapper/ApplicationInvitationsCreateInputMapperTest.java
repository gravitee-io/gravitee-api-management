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
package io.gravitee.rest.api.portal.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.rest.api.portal.rest.model.InvitationCreateInput;
import io.gravitee.rest.api.portal.rest.model.InvitationRecipientInput;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;

class ApplicationInvitationsCreateInputMapperTest {

    private static final URI CONFIRMATION_PAGE_URL = URI.create("https://portal.example.com/user/registration/confirm");

    private final ApplicationInvitationsCreateInputMapper cut = ApplicationInvitationsCreateInputMapper.INSTANCE;

    @Test
    void should_map_input_with_default_notify_true() {
        var input = new InvitationCreateInput()
            .recipients(
                List.of(
                    new InvitationRecipientInput().email(" Alice@Example.com "),
                    new InvitationRecipientInput().email("BOB@example.com")
                )
            )
            .role(" USER ")
            .confirmationPageUrl(CONFIRMATION_PAGE_URL);
        input.setNotify(null);

        var result = cut.toCreateApplicationInvitations(input);

        assertThat(result.recipientEmails()).containsExactly("alice@example.com", "bob@example.com");
        assertThat(result.roleName()).isEqualTo("USER");
        assertThat(result.notifyUsers()).isTrue();
        assertThat(result.confirmationPageUrl()).isEqualTo(CONFIRMATION_PAGE_URL);
    }

    @Test
    void should_map_input_with_explicit_notify_false() {
        var input = new InvitationCreateInput()
            .recipients(List.of(new InvitationRecipientInput().email("alice@example.com")))
            .role("USER")
            .notify(false);

        var result = cut.toCreateApplicationInvitations(input);

        assertThat(result.recipientEmails()).containsExactly("alice@example.com");
        assertThat(result.notifyUsers()).isFalse();
        assertThat(result.confirmationPageUrl()).isNull();
    }

    @Test
    void should_deduplicate_recipients_after_email_normalization() {
        var input = new InvitationCreateInput()
            .recipients(
                List.of(
                    new InvitationRecipientInput().email("Alice@Example.com"),
                    new InvitationRecipientInput().email(" alice@example.com ")
                )
            )
            .role("USER")
            .notify(false);

        var result = cut.toCreateApplicationInvitations(input);

        assertThat(result.recipientEmails()).containsExactly("alice@example.com");
    }
}
