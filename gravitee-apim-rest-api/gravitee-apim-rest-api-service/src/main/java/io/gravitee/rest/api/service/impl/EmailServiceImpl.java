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

import static io.gravitee.rest.api.model.parameters.Key.EMAIL_BRANDED_SENDERS;
import static io.gravitee.rest.api.model.parameters.Key.EMAIL_ENABLED;
import static io.gravitee.rest.api.model.parameters.Key.EMAIL_FROM;
import static io.gravitee.rest.api.model.parameters.Key.EMAIL_SUBJECT;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.settings.BrandedSenderConfig;
import io.gravitee.rest.api.model.settings.BrandedSenders;
import io.gravitee.rest.api.service.EmailNotification;
import io.gravitee.rest.api.service.EmailService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.ReferenceContext;
import io.gravitee.rest.api.service.common.TimeBoundedCharSequence;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.notification.NotificationTemplateService;
import io.gravitee.rest.api.service.spring.GraviteeJavaMailManager;
import jakarta.activation.MimetypesFileTypeMap;
import jakarta.mail.internet.InternetAddress;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.CustomLog;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
@Component
public class EmailServiceImpl extends TransactionalService implements EmailService {

    private static final Duration REGEX_TIMEOUT = Duration.ofSeconds(2);
    private static final String SUBJECT_PLACEHOLDER = "%s";
    private static final String POSITIONAL_SUBJECT_PLACEHOLDER = "%1$s";

    private final GraviteeJavaMailManager mailManager;
    private final NotificationTemplateService notificationTemplateService;
    private final ParameterService parameterService;
    private final String templatesPath;

    public EmailServiceImpl(
        GraviteeJavaMailManager mailManager,
        NotificationTemplateService notificationTemplateService,
        ParameterService parameterService,
        @Value("${templates.path:${gravitee.home}/templates}") String templatesPath
    ) {
        this.mailManager = mailManager;
        this.notificationTemplateService = notificationTemplateService;
        this.parameterService = parameterService;
        this.templatesPath = templatesPath;
    }

    @Override
    public void sendEmailNotification(ExecutionContext executionContext, final EmailNotification emailNotification) {
        final ReferenceContext context = executionContext.getReferenceContext();
        this.sendEmailNotification(
            executionContext,
            emailNotification,
            context.getReferenceId(),
            ParameterReferenceType.valueOf(context.getReferenceType().name())
        );
    }

    @Override
    @Async
    public void sendAsyncEmailNotification(ExecutionContext executionContext, final EmailNotification emailNotification) {
        sendEmailNotification(
            executionContext,
            emailNotification,
            executionContext.getReferenceContext().getReferenceId(),
            ParameterReferenceType.valueOf(executionContext.getReferenceContext().getReferenceType().name())
        );
    }

    private void sendEmailNotification(
        ExecutionContext executionContext,
        final EmailNotification emailNotification,
        String referenceId,
        ParameterReferenceType referenceType
    ) {
        Map<Key, String> mailParameters = getMailSenderConfiguration(executionContext, referenceId, referenceType);

        if (Boolean.parseBoolean(mailParameters.get(EMAIL_ENABLED)) && emailNotification.hasRecipients()) {
            try {
                JavaMailSender mailSender = mailManager.getOrCreateMailSender(executionContext, referenceId, referenceType);

                String emailSubject = notificationTemplateService.resolveTemplateWithParam(
                    executionContext.getOrganizationId(),
                    emailNotification.getTemplate() + ".EMAIL.TITLE",
                    emailNotification.getParams()
                );
                String content = notificationTemplateService.resolveTemplateWithParam(
                    executionContext.getOrganizationId(),
                    emailNotification.getTemplate() + ".EMAIL",
                    emailNotification.getParams()
                );
                content = content.replaceAll("&lt;br /&gt;", "<br />");

                // Group every recipient (to + bcc + copyToSender) by the branded sender its domain resolves to,
                // then send one message per (From, Subject) group, so recipients on different branded domains each
                // get their own sender identity. The common case — no configuration, or all recipients resolving to
                // the same sender — yields a single group, i.e. a single message identical to before.
                // Per-group send fault-isolation and idempotency (best-effort vs all-or-nothing, retry, per-group
                // logging) are deferred to APIM-14632 (F2) to keep this PR scoped; this keeps today's fail-fast.
                for (RecipientGroup group : buildRecipientGroups(emailNotification, mailParameters)) {
                    sendGroupedMessage(mailSender, emailNotification, emailSubject, content, group);
                }
            } catch (final Exception ex) {
                throw new TechnicalManagementException("Error while sending email notification", ex);
            }
        }
    }

    private String addResourcesInMessage(final MimeMessageHelper mailMessage, final String htmlText) throws Exception {
        final Document document = Jsoup.parse(htmlText);

        final Elements imageElements = document.getElementsByTag("img");
        final List<String> resources = imageElements
            .stream()
            .filter(imageElement -> imageElement.hasAttr("src"))
            .filter(imageElement -> !imageElement.attr("src").startsWith("http"))
            .map(imageElement -> {
                final String src = imageElement.attr("src");
                imageElement.attr("src", "cid:" + src);
                return src;
            })
            .toList();

        final String html = document.html();
        mailMessage.setText(html, true);

        for (final String res : resources) {
            if (res.startsWith("data:image/")) {
                final String value = res.replaceFirst("^data:image/[^;]*;base64,?", "");
                byte[] bytes = Base64.getDecoder().decode(value.getBytes(StandardCharsets.UTF_8));
                mailMessage.addInline(res, new ByteArrayResource(bytes), extractMimeType(res));
            } else {
                File file = new File(templatesPath, res);
                if (file.getCanonicalPath().startsWith(templatesPath)) {
                    final FileSystemResource templateResource = new FileSystemResource(file);
                    mailMessage.addInline(res, templateResource, getContentTypeByFileName(res));
                } else {
                    log.warn("Resource path invalid : {}", file.getPath());
                }
            }
        }

        return html;
    }

    private String getContentTypeByFileName(final String fileName) {
        if (fileName == null) {
            return "";
        } else if (fileName.endsWith(".png")) {
            return "image/png";
        }
        return MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(fileName);
    }

    /**
     * Extract the MIME type from a base64 string
     * @param encoded Base64 string
     * @return MIME type string
     */
    @SuppressWarnings("java:S5852") // do not warn about catastrophic backtracking as the matcher is bounded by a timeout
    private static String extractMimeType(final String encoded) {
        final Pattern mime = Pattern.compile("^data:([a-zA-Z0-9]+/[a-zA-Z0-9]+).*,.*");
        final Matcher matcher = mime.matcher(new TimeBoundedCharSequence(encoded, REGEX_TIMEOUT));
        if (!matcher.find()) return "";
        return matcher.group(1).toLowerCase();
    }

    private void sendGroupedMessage(
        JavaMailSender mailSender,
        EmailNotification emailNotification,
        String emailSubject,
        String content,
        RecipientGroup group
    ) throws Exception {
        final MimeMessageHelper mailMessage = new MimeMessageHelper(mailSender.createMimeMessage(), true, StandardCharsets.UTF_8.name());

        final InternetAddress configuredFrom = new InternetAddress(group.from());
        if (isEmpty(configuredFrom.getPersonal())) {
            if (isEmpty(emailNotification.getFromName())) {
                mailMessage.setFrom(group.from());
            } else {
                mailMessage.setFrom(group.from(), emailNotification.getFromName());
            }
        } else {
            mailMessage.setFrom(configuredFrom);
        }

        if (!isEmpty(emailNotification.getReplyTo())) {
            mailMessage.setReplyTo(emailNotification.getReplyTo());
        }
        if (!group.to().isEmpty()) {
            mailMessage.setTo(group.to().toArray(new String[0]));
        }
        if (!group.bcc().isEmpty()) {
            mailMessage.setBcc(group.bcc().toArray(new String[0]));
        }

        mailMessage.setSubject(applySubject(group.subjectTemplate(), emailSubject));

        final String html = addResourcesInMessage(mailMessage, content);

        log.debug("Sending an email to {} recipient(s)\nSubject: {}\nMessage: {}", group.recipientCount(), emailSubject, html);

        mailSender.send(mailMessage.getMimeMessage());
    }

    /**
     * Splits the recipients into groups that share the same resolved {@code (From, Subject)}. Each recipient's
     * domain (from {@code to}, {@code bcc}, and — when {@code copyToSender} is set — the reply-to/sender) is
     * matched against the branded-sender configuration; a match yields that config's {@code from} / {@code subject},
     * otherwise the default {@code EMAIL_FROM} / {@code EMAIL_SUBJECT}.
     *
     * <p>An explicit caller-provided {@code From} is a deliberate override and keeps today's single, unbranded
     * message. The publisher broadcast ({@link EmailService#DEFAULT_MAIL_TO}) is <em>not</em> exempt: its real
     * recipients live in {@code bcc} and are branded like any other, while the sentinel {@code to} is replaced
     * with the placeholder {@code EMAIL_FROM} address on the default-identity message (as before) so the
     * no-configuration case stays byte-identical to today.</p>
     */
    private List<RecipientGroup> buildRecipientGroups(EmailNotification emailNotification, Map<Key, String> mailParameters) {
        final String defaultFrom = mailParameters.get(EMAIL_FROM);
        final String defaultSubject = mailParameters.get(EMAIL_SUBJECT);

        // copyToSender copies the reply-to (or, failing that, the explicit from) to the sender.
        String sender = emailNotification.getFrom();
        if (!isEmpty(emailNotification.getReplyTo())) {
            sender = emailNotification.getReplyTo();
        }
        final boolean copyToSender = emailNotification.isCopyToSender() && sender != null;
        final List<String> bcc = new ArrayList<>();
        if (emailNotification.getBcc() != null) {
            bcc.addAll(Arrays.asList(emailNotification.getBcc()));
        }

        final boolean selfSend = Arrays.equals(DEFAULT_MAIL_TO, emailNotification.getTo());

        // An explicit caller-provided From is a deliberate override: keep today's single, unbranded message.
        if (!isEmpty(emailNotification.getFrom())) {
            return List.of(explicitFromGroup(emailNotification, defaultFrom, defaultSubject, sender, copyToSender, selfSend, bcc));
        }

        // Branding path: parse the configuration once, then match each recipient against the parsed list.
        final List<BrandedSenderConfig> configurations = BrandedSenders.parse(mailParameters.get(EMAIL_BRANDED_SENDERS));
        final Map<GroupKey, RecipientGroup> groups = new LinkedHashMap<>();
        // On a self-send the "to" is the DEFAULT_MAIL_TO sentinel, not real addresses, so it is not matched;
        // the placeholder recipient is added to the default-identity group at the end.
        if (!selfSend && emailNotification.getTo() != null) {
            for (String recipient : emailNotification.getTo()) {
                groupRecipient(groups, configurations, recipient, true, defaultFrom, defaultSubject);
            }
        }
        for (String recipient : bcc) {
            groupRecipient(groups, configurations, recipient, false, defaultFrom, defaultSubject);
        }
        // The sender's own copy uses the default identity rather than being branded by the sender's domain,
        // which could otherwise differ from what the recipients received.
        if (copyToSender) {
            defaultGroup(groups, defaultFrom, defaultSubject).bcc().add(sender);
        }
        if (selfSend) {
            addSelfSendPlaceholderRecipient(groups, defaultFrom, defaultSubject);
        }
        return new ArrayList<>(groups.values());
    }

    /**
     * Builds the single, unbranded group used when the caller supplies an explicit {@code From} (a deliberate
     * override). A self-send keeps its {@code EMAIL_FROM} placeholder recipient; {@code copyToSender} adds the
     * sender to the {@code bcc}.
     */
    private RecipientGroup explicitFromGroup(
        EmailNotification emailNotification,
        String defaultFrom,
        String defaultSubject,
        String sender,
        boolean copyToSender,
        boolean selfSend,
        List<String> bcc
    ) {
        final List<String> to = new ArrayList<>();
        if (selfSend) {
            to.add(defaultFrom);
        } else if (emailNotification.getTo() != null) {
            to.addAll(Arrays.asList(emailNotification.getTo()));
        }
        if (copyToSender) {
            bcc.add(sender);
        }
        return new RecipientGroup(emailNotification.getFrom(), defaultSubject, to, bcc);
    }

    /**
     * Restores today's placeholder {@code "To: <default from>"} on a self-send (publisher broadcast). It is
     * attached to the default-identity group only when one already exists (or no branded group does), so that
     * when every recipient resolved to a brand we do not emit a spurious message addressed only to ourselves.
     */
    private void addSelfSendPlaceholderRecipient(Map<GroupKey, RecipientGroup> groups, String defaultFrom, String defaultSubject) {
        if (groups.isEmpty()) {
            defaultGroup(groups, defaultFrom, defaultSubject);
        }
        final RecipientGroup defaultGroup = groups.get(new GroupKey(defaultFrom, defaultSubject));
        if (defaultGroup != null) {
            defaultGroup.to().add(defaultFrom);
        }
    }

    private void groupRecipient(
        Map<GroupKey, RecipientGroup> groups,
        List<BrandedSenderConfig> configurations,
        String recipient,
        boolean isTo,
        String defaultFrom,
        String defaultSubject
    ) {
        final Optional<BrandedSenderConfig> branded = BrandedSenders.match(configurations, recipient);
        // Optional.map already drops a null from/subject, so the filtered value is non-null here.
        final String from = branded
            .map(BrandedSenderConfig::getFrom)
            .filter(value -> !value.isBlank())
            .orElse(defaultFrom);
        final String subject = branded
            .map(BrandedSenderConfig::getSubject)
            .filter(value -> !value.isBlank())
            .orElse(defaultSubject);
        final RecipientGroup group = groups.computeIfAbsent(new GroupKey(from, subject), key ->
            new RecipientGroup(from, subject, new ArrayList<>(), new ArrayList<>())
        );
        if (isTo) {
            group.to().add(recipient);
        } else {
            group.bcc().add(recipient);
        }
    }

    private RecipientGroup defaultGroup(Map<GroupKey, RecipientGroup> groups, String defaultFrom, String defaultSubject) {
        return groups.computeIfAbsent(new GroupKey(defaultFrom, defaultSubject), key ->
            new RecipientGroup(defaultFrom, defaultSubject, new ArrayList<>(), new ArrayList<>())
        );
    }

    /**
     * Applies the subject template to the resolved email title by substituting the {@code "%s"} placeholder — and
     * the positional {@code "%1$s"} form that a pre-existing {@code EMAIL_SUBJECT} may use, which {@link String#format}
     * used to honour before this substitution replaced it.
     * Deliberately not {@link String#format}: the template is admin-controlled (via {@code EMAIL_SUBJECT}) or
     * branded-config text, so a stray {@code '%'}, a width specifier, or {@code %n} must be treated as literal text
     * rather than a format directive that could throw, allocate, or inject a newline into the Subject header.
     */
    private static String applySubject(String template, String title) {
        // The two placeholders are disjoint substrings, so replacing them independently (positional first) is safe.
        return template.replace(POSITIONAL_SUBJECT_PLACEHOLDER, title).replace(SUBJECT_PLACEHOLDER, title);
    }

    private record GroupKey(String from, String subjectTemplate) {}

    private record RecipientGroup(String from, String subjectTemplate, List<String> to, List<String> bcc) {
        int recipientCount() {
            return to.size() + bcc.size();
        }
    }

    private Map<Key, String> getMailSenderConfiguration(
        ExecutionContext executionContext,
        String referenceId,
        ParameterReferenceType referenceType
    ) {
        return parameterService
            .findAll(
                Arrays.asList(EMAIL_ENABLED, EMAIL_SUBJECT, EMAIL_FROM, EMAIL_BRANDED_SENDERS),
                referenceId,
                referenceType,
                executionContext
            )
            .entrySet()
            .stream()
            .collect(Collectors.toMap(e -> Key.findByKey(e.getKey()), e -> e.getValue().isEmpty() ? "" : e.getValue().get(0)));
    }
}
