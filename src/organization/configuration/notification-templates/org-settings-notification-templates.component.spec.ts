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

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpTestingController } from '@angular/common/http/testing';

import { OrgSettingsNotificationTemplatesComponent } from './org-settings-notification-templates.component';

import { OrganizationSettingsModule } from '../organization-settings.module';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../shared/testing';
import { fakeNotificationTemplate } from '../../../entities/notification/notificationTemplate.fixture';
import { NotificationTemplate } from '../../../entities/notification/notificationTemplate';
import { UIRouterState } from '../../../ajs-upgraded-providers';

describe('OrgSettingsNotificationTemplatesComponent', () => {
  let fixture: ComponentFixture<OrgSettingsNotificationTemplatesComponent>;
  let component: OrgSettingsNotificationTemplatesComponent;
  let httpTestingController: HttpTestingController;
  const fakeConstants = CONSTANTS_TESTING;
  const mockUiRouterState = { go: jest.fn() };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioHttpTestingModule, OrganizationSettingsModule],
      providers: [
        {
          provide: 'Constants',
          useValue: fakeConstants,
        },
        { provide: UIRouterState, useValue: mockUiRouterState },
      ],
    });
  });

  describe('with alert service enabled', () => {
    beforeEach(() => {
      fakeConstants.org.settings.alert.enabled = true;
      fixture = TestBed.createComponent(OrgSettingsNotificationTemplatesComponent);
      component = fixture.componentInstance;
      httpTestingController = TestBed.inject(HttpTestingController);
      fixture.detectChanges();
      httpTestingController
        .expectOne(`${CONSTANTS_TESTING.org.baseURL}/configuration/notification-templates`)
        .flush(getNotificationTemplates());
    });

    it('should setup notificationTemplatesByScope ', async () => {
      expect(component.groupNotificationTemplatesVMByScope(getNotificationTemplates())).toStrictEqual({
        API: [
          {
            scope: 'API',
            humanReadableScope: 'API',
            name: 'API key Revoked',
            hook: 'APIKEY_REVOKED',
            description: 'Triggered when an API key is revoked.',
            overridden: false,
            icon: 'dashboard',
          },
          {
            scope: 'API',
            humanReadableScope: 'API',
            name: 'API Stopped',
            hook: 'API_STOPPED',
            description: 'Triggered when an API is stopped',
            overridden: true,
            icon: 'dashboard',
          },
          {
            scope: 'API',
            humanReadableScope: 'API',
            name: 'Subscription Accepted',
            hook: 'SUBSCRIPTION_ACCEPTED',
            description: 'Triggered when a Subscription is accepted.',
            overridden: false,
            icon: 'dashboard',
          },
        ],
        Application: [
          {
            scope: 'APPLICATION',
            humanReadableScope: 'Application',
            name: 'Subscription Accepted',
            hook: 'SUBSCRIPTION_ACCEPTED',
            description: 'Triggered when a Subscription is accepted.',
            overridden: false,
            icon: 'list',
          },
        ],
        'Templates for action': [
          {
            scope: 'TEMPLATES_FOR_ACTION',
            humanReadableScope: 'Templates for action',
            name: 'User registration',
            hook: 'USER_REGISTRATION',
            description: 'Email sent to a user who has self-registered on portal or admin console. Contains a registration link.',
            overridden: false,
            icon: 'assignment',
          },
        ],
        'Templates for alert': [
          {
            scope: 'TEMPLATES_FOR_ALERT',
            humanReadableScope: 'Templates for alert',
            name: 'HTTP status code',
            hook: 'CONSUMER_HTTP_STATUS',
            description: 'Email sent to all members of an application when an "HTTP Status" consumer alert has been triggered.',
            overridden: false,
            icon: 'notifications',
          },
        ],
      });
    });
  });

  describe('with alert service disabled', () => {
    beforeEach(() => {
      fakeConstants.org.settings.alert.enabled = false;
      fixture = TestBed.createComponent(OrgSettingsNotificationTemplatesComponent);
      component = fixture.componentInstance;
      httpTestingController = TestBed.inject(HttpTestingController);
      fixture.detectChanges();
      httpTestingController
        .expectOne(`${CONSTANTS_TESTING.org.baseURL}/configuration/notification-templates`)
        .flush(getNotificationTemplates());
    });

    it('should setup notificationTemplatesByScope ', async () => {
      expect(component.groupNotificationTemplatesVMByScope(getNotificationTemplates())).toStrictEqual({
        API: [
          {
            scope: 'API',
            humanReadableScope: 'API',
            name: 'API key Revoked',
            hook: 'APIKEY_REVOKED',
            description: 'Triggered when an API key is revoked.',
            overridden: false,
            icon: 'dashboard',
          },
          {
            scope: 'API',
            humanReadableScope: 'API',
            name: 'API Stopped',
            hook: 'API_STOPPED',
            description: 'Triggered when an API is stopped',
            overridden: true,
            icon: 'dashboard',
          },
          {
            scope: 'API',
            humanReadableScope: 'API',
            name: 'Subscription Accepted',
            hook: 'SUBSCRIPTION_ACCEPTED',
            description: 'Triggered when a Subscription is accepted.',
            overridden: false,
            icon: 'dashboard',
          },
        ],
        Application: [
          {
            scope: 'APPLICATION',
            humanReadableScope: 'Application',
            name: 'Subscription Accepted',
            hook: 'SUBSCRIPTION_ACCEPTED',
            description: 'Triggered when a Subscription is accepted.',
            overridden: false,
            icon: 'list',
          },
        ],
        'Templates for action': [
          {
            scope: 'TEMPLATES_FOR_ACTION',
            humanReadableScope: 'Templates for action',
            name: 'User registration',
            hook: 'USER_REGISTRATION',
            description: 'Email sent to a user who has self-registered on portal or admin console. Contains a registration link.',
            overridden: false,
            icon: 'assignment',
          },
        ],
      });
    });
  });

  function getNotificationTemplates(): NotificationTemplate[] {
    return [
      fakeNotificationTemplate({
        description: 'Triggered when an API is stopped',
        hook: 'API_STOPPED',
        name: 'API Stopped',
        scope: 'API',
        title: 'API stopped',
        type: 'PORTAL',
        enabled: false,
      }),
      fakeNotificationTemplate({
        description: 'Triggered when an API is stopped',
        hook: 'API_STOPPED',
        name: 'API Stopped',
        scope: 'API',
        title: 'API stopped',
        type: 'EMAIL',
        enabled: true,
      }),
      fakeNotificationTemplate({
        description: 'Triggered when a Subscription is accepted.',
        hook: 'SUBSCRIPTION_ACCEPTED',
        name: 'Subscription Accepted',
        scope: 'API',
        title: 'Subscription approved',
        type: 'EMAIL',
        enabled: false,
      }),
      fakeNotificationTemplate({
        description: 'Triggered when an API key is revoked.',
        hook: 'APIKEY_REVOKED',
        name: 'API key Revoked',
        scope: 'API',
        title: '[${api.name}] Apikey Revoked\n',
        type: 'PORTAL',
        enabled: false,
      }),
      fakeNotificationTemplate({
        description: 'Triggered when a Subscription is accepted.',
        hook: 'SUBSCRIPTION_ACCEPTED',
        name: 'Subscription Accepted',
        scope: 'APPLICATION',
        title: 'Your subscription to ${api.name} with plan ${plan.name} has been approved',
        type: 'EMAIL',
        enabled: false,
      }),
      fakeNotificationTemplate({
        description: 'Email sent to a user who has self-registered on portal or admin console. Contains a registration link.',
        hook: 'USER_REGISTRATION',
        name: 'User registration',
        scope: 'TEMPLATES_FOR_ACTION',
        title: 'User ${registrationAction} - ${user.displayName}',
        type: 'EMAIL',
        enabled: false,
      }),
      fakeNotificationTemplate({
        description: 'Email sent to all members of an application when an "HTTP Status" consumer alert has been triggered.',
        hook: 'CONSUMER_HTTP_STATUS',
        name: 'HTTP status code',
        scope: 'TEMPLATES_FOR_ALERT',
        title: 'Alert reached for the application ${application.name}',
        type: 'EMAIL',
        enabled: false,
      }),
    ];
  }
});
