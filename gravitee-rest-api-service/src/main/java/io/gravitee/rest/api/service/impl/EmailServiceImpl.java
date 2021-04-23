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
package io.gravitee.rest.api.service.impl;

import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.EmailNotification;
import io.gravitee.rest.api.service.EmailService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.notification.NotificationTemplateService;
import io.gravitee.rest.api.service.spring.GraviteeJavaMailManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.activation.MimetypesFileTypeMap;
import javax.mail.internet.InternetAddress;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.gravitee.rest.api.model.parameters.Key.*;
import static java.lang.String.format;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class EmailServiceImpl extends TransactionalService implements EmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Autowired
    private GraviteeJavaMailManager mailManager;
    @Autowired
    private NotificationTemplateService notificationTemplateService;
    @Autowired
    private ParameterService parameterService;
    @Value("${templates.path:${gravitee.home}/templates}")
    private String templatesPath;

    @Override
    public void sendEmailNotification(final EmailNotification emailNotification) {
        final GraviteeContext.ReferenceContext context = GraviteeContext.getCurrentContext();
        this.sendEmailNotification(emailNotification, context.getReferenceId(), ParameterReferenceType.valueOf(context.getReferenceType().name()));
    }

    private void sendEmailNotification(final EmailNotification emailNotification, String referenceId, ParameterReferenceType referenceType) {
        Map<Key, String> mailParameters = getMailSenderConfiguration(referenceId, referenceType);

        if (Boolean.parseBoolean(mailParameters.get(EMAIL_ENABLED))
                && emailNotification.getTo() != null
                && emailNotification.getTo().length > 0) {
            try {
                JavaMailSender mailSender = mailManager.getOrCreateMailSender(referenceId, referenceType);
                final MimeMessageHelper mailMessage = new MimeMessageHelper(mailSender.createMimeMessage(), true, StandardCharsets.UTF_8.name());

                String emailSubject = notificationTemplateService.resolveTemplateWithParam(emailNotification.getTemplate() + ".EMAIL.TITLE", emailNotification.getParams());
                String content = notificationTemplateService.resolveTemplateWithParam(emailNotification.getTemplate() + ".EMAIL", emailNotification.getParams());
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
                if (! isEmpty(emailNotification.getReplyTo())) {
                    mailMessage.setReplyTo(emailNotification.getReplyTo());
                    sender = emailNotification.getReplyTo();
                }

                if (Arrays.equals(DEFAULT_MAIL_TO,emailNotification.getTo())) {
                    mailMessage.setTo(mailParameters.get(Key.EMAIL_FROM));
                } else {
                    mailMessage.setTo(emailNotification.getTo());
                }

                if (emailNotification.isCopyToSender() && sender != null) {
                    mailMessage.setBcc(sender);
                }

                if (emailNotification.getBcc() != null && emailNotification.getBcc().length > 0) {
                    mailMessage.setBcc(emailNotification.getBcc());
                }

                mailMessage.setSubject(format(mailParameters.get(EMAIL_SUBJECT), emailSubject));

                final String html = addResourcesInMessage(mailMessage, content);

                LOGGER.debug("Sending an email to: {}\nSubject: {}\nMessage: {}",
                        emailNotification.getTo(), emailSubject, html);

                mailSender.send(mailMessage.getMimeMessage());
            } catch (final Exception ex) {
                LOGGER.error("Error while sending email notification", ex);
                throw new TechnicalManagementException("Error while sending email notification", ex);
            }
        }
    }

    @Override
    @Async
    public void sendAsyncEmailNotification(final EmailNotification emailNotification, GraviteeContext.ReferenceContext context) {
        sendEmailNotification(emailNotification, context.getReferenceId(), ParameterReferenceType.valueOf(context.getReferenceType().name()));
    }

    private String addResourcesInMessage(final MimeMessageHelper mailMessage, final String htmlText) throws Exception {
        final Document document = Jsoup.parse(htmlText);

        final List<String> resources = new ArrayList<>();

        final Elements imageElements = document.getElementsByTag("img");
        resources.addAll(imageElements.stream()
                .filter(imageElement -> imageElement.hasAttr("src"))
                .filter(imageElement -> !imageElement.attr("src").startsWith("http"))
                .map(imageElement -> {
                    final String src = imageElement.attr("src");
                    imageElement.attr("src", "cid:" + src);
                    return src;
                })
                .collect(Collectors.toList()));

        final String html = document.html();
        mailMessage.setText(html, true);

        for (final String res : resources) {
            if (res.startsWith("data:image/")) {
                final String value = res.replaceFirst("^data:image/[^;]*;base64,?", "");
                byte[] bytes = Base64.getDecoder().decode(value.getBytes("UTF-8"));
                mailMessage.addInline(res, new ByteArrayResource(bytes), extractMimeType(res));
            } else {
                final FileSystemResource templateResource = new FileSystemResource(new File(templatesPath, res));
                mailMessage.addInline(res, templateResource, getContentTypeByFileName(res));
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
    private static String extractMimeType(final String encoded) {
        final Pattern mime = Pattern.compile("^data:([a-zA-Z0-9]+/[a-zA-Z0-9]+).*,.*");
        final Matcher matcher = mime.matcher(encoded);
        if (!matcher.find())
            return "";
        return matcher.group(1).toLowerCase();
    }

    private Map<Key, String> getMailSenderConfiguration(String referenceId, ParameterReferenceType referenceType) {
        return parameterService.findAll(Arrays.asList(
                EMAIL_ENABLED,
                EMAIL_SUBJECT,
                EMAIL_FROM
        ), referenceId, referenceType)
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        e -> Key.findByKey(e.getKey()),
                        e -> e.getValue().isEmpty() ? "" : e.getValue().get(0))
                );
    }
}
