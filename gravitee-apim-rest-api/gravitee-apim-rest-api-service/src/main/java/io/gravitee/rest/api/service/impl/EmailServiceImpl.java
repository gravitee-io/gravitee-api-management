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

import static io.gravitee.rest.api.model.parameters.Key.EMAIL_ENABLED;
import static io.gravitee.rest.api.model.parameters.Key.EMAIL_FROM;
import static io.gravitee.rest.api.model.parameters.Key.EMAIL_SUBJECT;
import static java.lang.String.format;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.EmailNotification;
import io.gravitee.rest.api.service.EmailService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@Component
public class EmailServiceImpl extends TransactionalService implements EmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailServiceImpl.class);

    private static final Duration REGEX_TIMEOUT = Duration.ofSeconds(2);

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
                final MimeMessageHelper mailMessage = new MimeMessageHelper(
                    mailSender.createMimeMessage(),
                    true,
                    StandardCharsets.UTF_8.name()
                );

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

                final String from = isNull(emailNotification.getFrom()) || emailNotification.getFrom().isEmpty()
                    ? mailParameters.get(EMAIL_FROM)
                    : emailNotification.getFrom();

                InternetAddress configuredFrom = new InternetAddress(from);
                if (isEmpty(configuredFrom.getPersonal())) {
                    if (isEmpty(emailNotification.getFromName())) {
                        mailMessage.setFrom(from);
                    } else {
                        mailMessage.setFrom(from, emailNotification.getFromName());
                    }
                } else {
                    mailMessage.setFrom(configuredFrom);
                }

                String sender = emailNotification.getFrom();
                if (!isEmpty(emailNotification.getReplyTo())) {
                    mailMessage.setReplyTo(emailNotification.getReplyTo());
                    sender = emailNotification.getReplyTo();
                }

                if (Arrays.equals(DEFAULT_MAIL_TO, emailNotification.getTo())) {
                    mailMessage.setTo(mailParameters.get(Key.EMAIL_FROM));
                } else if (emailNotification.getTo() != null && emailNotification.getTo().length > 0) {
                    mailMessage.setTo(emailNotification.getTo());
                }

                if (emailNotification.getBcc() != null && emailNotification.getBcc().length > 0) {
                    mailMessage.setBcc(emailNotification.getBcc());
                }

                if (emailNotification.isCopyToSender() && sender != null) {
                    mailMessage.addBcc(sender);
                }

                mailMessage.setSubject(format(mailParameters.get(EMAIL_SUBJECT), emailSubject));

                final String html = addResourcesInMessage(mailMessage, content);

                LOGGER.debug(
                    "Sending an email to {} recipient(s)\nSubject: {}\nMessage: {}",
                    emailNotification.recipientsCount(),
                    emailSubject,
                    html
                );

                mailSender.send(mailMessage.getMimeMessage());
            } catch (final Exception ex) {
                LOGGER.error("Error while sending email notification", ex);
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
                    LOGGER.warn("Resource path invalid : {}", file.getPath());
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

    private Map<Key, String> getMailSenderConfiguration(
        ExecutionContext executionContext,
        String referenceId,
        ParameterReferenceType referenceType
    ) {
        return parameterService
            .findAll(Arrays.asList(EMAIL_ENABLED, EMAIL_SUBJECT, EMAIL_FROM), referenceId, referenceType, executionContext)
            .entrySet()
            .stream()
            .collect(Collectors.toMap(e -> Key.findByKey(e.getKey()), e -> e.getValue().isEmpty() ? "" : e.getValue().get(0)));
    }
}
