/*
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

import { NotificationTemplate } from './notificationTemplate';

export function fakeNotificationTemplate(attributes?: Partial<NotificationTemplate>): NotificationTemplate {
  const defaultValue: NotificationTemplate = {
    id: 'notification_template#1',
    content:
      '<html>\n\t<body>\n\t\t<header style="text-align: center;">\n\t\t\t<#include "header.html" />\n\t\t</header>\n\t\t<div style="margin-top: 50px; color: #424e5a;">\n\t\t\t<p><b>Ticket created by ${user.displayName}</b></p>\n\t\t\t<#if api??>\n\t\t\t\t<p><b>API: ${api.name} (${api.version})</b></p>\n\t\t\t</#if>\n\t\t\t<#if application??>\n\t\t\t\t<p><b>Application: ${application.name} (${application.primaryOwner.displayName})</b></p>\n\t\t\t</#if>\n\t\t\t<p>${content}</p>\n\t\t</div>\n\t</body>\n</html>',
    created_at: 1633417938291,
    description: 'Email sent to support team of an API or of the platform, when a support ticket is created.',
    hook: 'SUPPORT_TICKET',
    name: 'Support ticket',
    scope: 'TEMPLATES_FOR_ACTION',
    title: '${ticketSubject}',
    type: 'EMAIL',
  };
  return {
    ...defaultValue,
    ...attributes,
  };
}
