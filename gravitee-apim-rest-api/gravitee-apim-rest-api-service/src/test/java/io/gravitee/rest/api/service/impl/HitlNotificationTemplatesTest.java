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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.gravitee.repository.management.api.NotificationTemplateRepository;
import io.gravitee.rest.api.model.notification.NotificationTemplateEntity;
import io.gravitee.rest.api.service.builder.EmailNotificationBuilder.EmailTemplate;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.notification.HitlHook;
import io.gravitee.rest.api.service.notification.HookScope;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.util.ReflectionTestUtils;

class HitlNotificationTemplatesTest {

    private static final String ORGANIZATION_ID = "ORG_ID";
    // The default templates ship with the distribution, which is not on this module's classpath.
    // The path is relative to this module's base directory, which is where Maven runs the tests.
    private static final Path TEMPLATES_DIRECTORY = Path.of(
        "../../gravitee-apim-distribution/gravitee-apim-distribution-standalone/gravitee-apim-distribution-standalone-rest-api/src/main/resources/templates"
    );
    private static final String[] CONTRACT_KEYS = {
        "tool",
        "proxy",
        "rule",
        "agent",
        "approvers",
        "expiresAt",
        "timeLeft",
        "decidedAt",
        "grant",
        "approvalUrl",
    };

    private NotificationTemplateServiceImpl service;

    @BeforeAll
    static void requireDefaultTemplatesDirectory() {
        assertThat(TEMPLATES_DIRECTORY)
            .as("default HITL templates directory, expected at %s relative to the module", TEMPLATES_DIRECTORY.toAbsolutePath())
            .isDirectory();
    }

    @BeforeEach
    void setUp() throws Exception {
        GraviteeContext.setCurrentOrganization(ORGANIZATION_ID);
        service = new NotificationTemplateServiceImpl();
        ReflectionTestUtils.setField(service, "templatesPath", TEMPLATES_DIRECTORY.toString());
        ReflectionTestUtils.setField(service, "notificationTemplateRepository", mock(NotificationTemplateRepository.class));
        service.afterPropertiesSet();
    }

    @AfterEach
    void tearDown() {
        GraviteeContext.cleanContext();
    }

    @ParameterizedTest
    @EnumSource(HitlHook.class)
    void should_render_body_and_subject_with_a_full_model(HitlHook hook) {
        Map<String, Object> model = fullModel();

        String body = renderBody(hook, model);
        String subject = renderSubject(hook, model);

        assertThat(body).isNotBlank().doesNotContain("${").doesNotContain("<#");
        assertThat(subject).isNotBlank().doesNotContain("${");
    }

    @ParameterizedTest
    @EnumSource(HitlHook.class)
    void should_render_body_and_subject_when_optional_values_are_absent(HitlHook hook) {
        Map<String, Object> model = emptyOptionalModel();

        String body = renderBody(hook, model);
        String subject = renderSubject(hook, model);

        assertThat(body).isNotBlank().doesNotContain("${").doesNotContain("<#");
        assertThat(subject).isNotBlank().doesNotContain("${");
    }

    @ParameterizedTest
    @EnumSource(HitlHook.class)
    void should_escape_html_in_values_rendered_in_the_body_exactly_once(HitlHook hook) {
        Map<String, Object> model = fullModel();
        hitl(model).put("tool", "<b>refund</b>");

        String body = renderBody(hook, model);

        assertThat(body).contains("&lt;b&gt;refund&lt;/b&gt;").doesNotContain("<b>refund</b>").doesNotContain("&amp;lt;");
    }

    @Test
    void should_escape_ampersand_in_the_approval_url() {
        String body = renderBody(HitlHook.HITL_APPROVAL_REQUIRED, fullModel());

        assertThat(body).contains("approvals/1?a=1&amp;b=2");
    }

    @Test
    void should_not_reference_call_arguments_in_any_default_template() throws IOException {
        List<Path> defaultTemplates;
        try (Stream<Path> files = Files.list(TEMPLATES_DIRECTORY)) {
            defaultTemplates = files.filter(file -> file.getFileName().toString().startsWith("hitl")).toList();
        }

        assertThat(defaultTemplates).hasSize(HitlHook.values().length);
        for (Path template : defaultTemplates) {
            assertThat(Files.readString(template)).as(template.getFileName().toString()).doesNotContain("hitl.arguments");
        }
    }

    @ParameterizedTest
    @EnumSource(HitlHook.class)
    void should_end_every_default_template_with_the_closing_html_tag(HitlHook hook) throws IOException {
        String source = Files.readString(TEMPLATES_DIRECTORY.resolve(EmailTemplate.fromHook(hook).getHtmlTemplate()));

        assertThat(source.stripTrailing()).endsWith("</html>");
    }

    @ParameterizedTest
    @EnumSource(HitlHook.class)
    void should_document_every_variable_its_default_template_references(HitlHook hook) throws IOException {
        String source = Files.readString(TEMPLATES_DIRECTORY.resolve(EmailTemplate.fromHook(hook).getHtmlTemplate()));
        Set<String> referencedVariables = new HashSet<>();
        Matcher reference = Pattern.compile("hitl\\.([A-Za-z.]+)").matcher(source);
        while (reference.find()) {
            referencedVariables.add(reference.group(1));
        }

        String description = hook.getDescription();
        String variables = description.substring(description.indexOf("Variables:"));

        assertThat(referencedVariables).isNotEmpty();
        for (String variable : referencedVariables) {
            assertThat(variables)
                .as("%s description must document %s in its Variables list", hook.name(), variable)
                .containsPattern("\\b" + Pattern.quote(variable) + "\\b");
        }
    }

    @Test
    void should_document_every_contract_variable_in_some_hook_description() {
        String variables = Stream.of(HitlHook.values())
            .map(hook -> hook.getDescription().substring(hook.getDescription().indexOf("Variables:")))
            .collect(Collectors.joining(" "));

        for (String key : CONTRACT_KEYS) {
            assertThat(variables).as("contract variable %s must be documented", key).containsPattern("\\b" + Pattern.quote(key) + "\\b");
        }
    }

    @Test
    void should_name_the_tool_in_the_approval_required_subject() {
        String subject = renderSubject(HitlHook.HITL_APPROVAL_REQUIRED, fullModel());

        assertThat(subject).isEqualTo("Approval needed: payments.refund");
    }

    @Test
    void should_load_the_default_template_of_every_hitl_hook() {
        for (HitlHook hook : HitlHook.values()) {
            Set<NotificationTemplateEntity> templates = service.findByHookAndScope(ORGANIZATION_ID, hook.name(), HookScope.HITL.name());

            assertThat(templates)
                .as(hook.name())
                .singleElement()
                .satisfies(template -> {
                    assertThat(template.getTemplateName()).isEqualTo(hook.getTemplate() + ".EMAIL");
                    assertThat(template.getTitle()).isNotBlank();
                    assertThat(template.getContent()).isNotBlank();
                });
        }
    }

    private String renderBody(HitlHook hook, Map<String, Object> model) {
        return service.resolveTemplateWithParam(ORGANIZATION_ID, hook.getTemplate() + ".EMAIL", model);
    }

    private String renderSubject(HitlHook hook, Map<String, Object> model) {
        return service.resolveTemplateWithParam(ORGANIZATION_ID, hook.getTemplate() + ".EMAIL.TITLE", model);
    }

    private static Map<String, Object> fullModel() {
        Map<String, Object> grant = new HashMap<>();
        grant.put("kind", "timed");
        grant.put("minutes", "30");
        grant.put("coversUntil", "2026-11-20 10:35 UTC");

        Map<String, Object> hitl = new HashMap<>();
        hitl.put("tool", "payments.refund");
        hitl.put("proxy", "Billing proxy");
        hitl.put("rule", "Refunds over 100");
        hitl.put("agent", "support-agent");
        hitl.put("approvers", "finance-team");
        hitl.put("expiresAt", "2026-11-20 10:15 UTC");
        hitl.put("timeLeft", "12 minutes");
        hitl.put("decidedAt", "2026-11-20 10:05 UTC");
        hitl.put("grant", grant);
        hitl.put("approvalUrl", "https://console.example.com/approvals/1?a=1&b=2");
        return Map.of("hitl", hitl);
    }

    private static Map<String, Object> emptyOptionalModel() {
        Map<String, Object> hitl = new HashMap<>();
        for (String key : CONTRACT_KEYS) {
            hitl.put(key, null);
        }
        hitl.put("tool", "payments.refund");
        return Map.of("hitl", hitl);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> hitl(Map<String, Object> model) {
        return (Map<String, Object>) model.get("hitl");
    }
}
