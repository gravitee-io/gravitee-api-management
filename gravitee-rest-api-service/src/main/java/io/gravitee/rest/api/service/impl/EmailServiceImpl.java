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

import freemarker.template.Configuration;
import freemarker.template.Template;
import io.gravitee.rest.api.service.EmailNotification;
import io.gravitee.rest.api.service.EmailService;
import io.gravitee.rest.api.service.exceptions.EmailDisabledException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.activation.MimetypesFileTypeMap;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.springframework.ui.freemarker.FreeMarkerTemplateUtils.processTemplateIntoString;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class EmailServiceImpl extends TransactionalService implements EmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Autowired
    private JavaMailSender mailSender;
    @Autowired
    private Configuration freemarkerConfiguration;
    @Value("${templates.path:${gravitee.home}/templates}")
    private String templatesPath;
    @Value("${email.subject:[Gravitee.io] %s}")
    private String subject;
    @Value("${email.enabled:false}")
    private boolean enabled;
    @Value("${email.from}")
    private String defaultFrom;

    public void sendEmailNotification(final EmailNotification emailNotification) {
        if (enabled) {
            try {
                final MimeMessageHelper mailMessage = new MimeMessageHelper(mailSender.createMimeMessage(), true, StandardCharsets.UTF_8.name());

                final Template template = freemarkerConfiguration.getTemplate(emailNotification.getTemplate());
                final String content = processTemplateIntoString(template, emailNotification.getParams());

                final String from = isNull(emailNotification.getFrom()) || emailNotification.getFrom().isEmpty()
                        ? defaultFrom
                        : emailNotification.getFrom();

                if (isEmpty(emailNotification.getFromName())) {
                    mailMessage.setFrom(from);
                } else {
                    mailMessage.setFrom(from, emailNotification.getFromName());
                }

                mailMessage.setTo(emailNotification.getTo());
                if (emailNotification.isCopyToSender() && emailNotification.getFrom() != null) {
                    mailMessage.setBcc(emailNotification.getFrom());
                }
                if (emailNotification.getBcc() != null && emailNotification.getBcc().length > 0) {
                    mailMessage.setBcc(emailNotification.getBcc());
                }
                mailMessage.setSubject(format(subject, emailNotification.getSubject()));

                final String html = addResourcesInMessage(mailMessage, content);

                LOGGER.debug("Sending an email to: {}\nSubject: {}\nMessage: {}",
                        emailNotification.getTo(), emailNotification.getSubject(), html);

                mailSender.send(mailMessage.getMimeMessage());
            } catch (final Exception ex) {
                LOGGER.error("Error while sending email notification", ex);
                throw new TechnicalManagementException("Error while sending email notification", ex);
            }
        } else {
            throw new EmailDisabledException();
        }
    }

    @Async
    public void sendAsyncEmailNotification(final EmailNotification emailNotification) {
        sendEmailNotification(emailNotification);
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
            final FileSystemResource templateResource = new FileSystemResource(new File(templatesPath, res));
            mailMessage.addInline(res, templateResource, getContentTypeByFileName(res));
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
}
