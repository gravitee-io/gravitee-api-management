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
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import freemarker.template.TemplateException;
import io.gravitee.repository.management.model.MetadataReferenceType;
import io.gravitee.rest.api.model.MetadataEntity;
import io.gravitee.rest.api.model.MetadataFormat;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.service.MetadataService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.InvalidTemplateException;
import io.gravitee.rest.api.service.exceptions.TemplateProcessingException;
import io.gravitee.rest.api.service.notification.NotificationTemplateService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PageService_TemplateTest {

    private static final String ORGANIZATION_ID = "ORG_ID";
    private static final String ENVIRONMENT_ID = "ENV_ID";
    private static final String PAGE_ID = "ba01aef0-e3da-4499-81ae-f0e3daa4995a";

    @InjectMocks
    private PageServiceImpl pageService = new PageServiceImpl();

    @Mock
    private MetadataService metadataService;

    @Mock
    private NotificationTemplateService notificationTemplateService;

    @Test
    void shouldSetContentBasedOnTemplate() {
        MetadataEntity metadata = new MetadataEntity();
        metadata.setKey("emailSupport");
        metadata.setName("Email Support");
        metadata.setFormat(MetadataFormat.MAIL);
        metadata.setValue("support@gio.com");
        when(metadataService.findByReferenceTypeAndReferenceId(MetadataReferenceType.ENVIRONMENT, ENVIRONMENT_ID))
            .thenReturn(List.of(metadata));

        PageEntity pageEntity = new PageEntity();
        pageEntity.setId(PAGE_ID);
        pageEntity.setContent("# Hello ${metadata.emailSupport}");

        when(
            notificationTemplateService.resolveInlineTemplateWithParam(
                eq(ORGANIZATION_ID),
                eq(pageEntity.getId()),
                eq(pageEntity.getContent()),
                anyMap(),
                eq(false)
            )
        )
            .thenReturn("# Hello support@gio.com");

        pageService.transformWithTemplate(new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID), pageEntity, null);

        assertThat(pageEntity.getContent()).isEqualTo("# Hello support@gio.com");
    }

    @Test
    void shouldKeepContentAsIsWhenTemplateIsInvalid() {
        MetadataEntity metadata = new MetadataEntity();
        metadata.setKey("emailSupport");
        metadata.setName("Email Support");
        metadata.setFormat(MetadataFormat.MAIL);
        metadata.setValue("support@gio.com");
        when(metadataService.findByReferenceTypeAndReferenceId(MetadataReferenceType.ENVIRONMENT, ENVIRONMENT_ID))
            .thenReturn(List.of(metadata));

        PageEntity pageEntity = new PageEntity();
        pageEntity.setId(PAGE_ID);
        pageEntity.setContent("# Hello ${metadata.['emailSupport']}");

        when(
            notificationTemplateService.resolveInlineTemplateWithParam(
                eq(ORGANIZATION_ID),
                eq(pageEntity.getId()),
                eq(pageEntity.getContent()),
                anyMap(),
                eq(false)
            )
        )
            .thenThrow(new InvalidTemplateException("expecting something else, found ["));

        pageService.transformWithTemplate(new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID), pageEntity, null);

        assertThat(pageEntity.getContent()).isEqualTo("# Hello ${metadata.['emailSupport']}");
        assertThat(pageEntity.getMessages()).isEqualTo(List.of("Invalid template: expecting something else, found ["));
    }

    @Test
    void shouldKeepContentAsIsWhenUsingUnknownProperty() {
        MetadataEntity metadata = new MetadataEntity();
        metadata.setKey("emailSupport");
        metadata.setName("Email Support");
        metadata.setFormat(MetadataFormat.MAIL);
        metadata.setValue("support@gio.com");
        when(metadataService.findByReferenceTypeAndReferenceId(MetadataReferenceType.ENVIRONMENT, ENVIRONMENT_ID))
            .thenReturn(List.of(metadata));

        PageEntity pageEntity = new PageEntity();
        pageEntity.setId(PAGE_ID);
        pageEntity.setContent("# Hello ${metadata.wrong}");

        when(
            notificationTemplateService.resolveInlineTemplateWithParam(
                eq(ORGANIZATION_ID),
                eq(pageEntity.getId()),
                eq(pageEntity.getContent()),
                anyMap(),
                eq(false)
            )
        )
            .thenThrow(new TemplateProcessingException(new TemplateException(null)));

        pageService.transformWithTemplate(new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID), pageEntity, null);

        assertThat(pageEntity.getContent()).isEqualTo("# Hello ${metadata.wrong}");
        assertThat(pageEntity.getMessages()).isEqualTo(List.of("Invalid expression or value is missing for null"));
    }
}
