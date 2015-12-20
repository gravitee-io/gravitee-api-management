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
package io.gravitee.management.rest.spring;

import freemarker.cache.FileTemplateLoader;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.EnableAsync;

import javax.mail.MessagingException;
import java.io.File;

/**
 * @author Azize Elamrani (azize dot elamrani at gmail dot com)
 */
@Configuration
@EnableAsync
public class EmailConfiguration {

    @Value("${email.host}")
    private String host;

    @Value("${email.port}")
    private String port;

    @Value("${email.from}")
    private String from;

    @Value("${email.username}")
    private String username;

    @Value("${email.password}")
    private String password;

    @Value("${templates.path:${gravitee.home}/templates}")
    private String templatesPath;

    @Bean
    public JavaMailSenderImpl mailSender() {
        final JavaMailSenderImpl javaMailSender = new JavaMailSenderImpl();
        javaMailSender.setHost(host);
        if (StringUtils.isNumeric(port)) {
            javaMailSender.setPort(Integer.valueOf(this.port));
        }
        javaMailSender.setUsername(username);
        javaMailSender.setPassword(password);
        return javaMailSender;
    }

    @Bean
    public MimeMessageHelper mailMessage(JavaMailSenderImpl mailSender) throws MessagingException {
        final MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mailSender.createMimeMessage(), true);
        mimeMessageHelper.setFrom(from);
        return mimeMessageHelper;
    }

    @Bean
    public freemarker.template.Configuration getConfiguration() throws Exception {
        final freemarker.template.Configuration configuration =
                new freemarker.template.Configuration(freemarker.template.Configuration.VERSION_2_3_22);
        configuration.setTemplateLoader(new FileTemplateLoader(new File(templatesPath)));
        return configuration;
    }

}
