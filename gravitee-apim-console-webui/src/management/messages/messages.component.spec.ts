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
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { of } from 'rxjs';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { ActivatedRoute } from '@angular/router';

import { MessagesComponent } from './messages.component';
import { MessagesHarness } from './messages.harness';
import { MessagesModule } from './messages.module';

import { RoleService } from '../../services-ngx/role.service';
import { fakeRole } from '../../entities/role/role.fixture';
import { HttpMessagePayload, TextMessagePayload } from '../../entities/message/messagePayload';
import { CONSTANTS_TESTING, GioTestingModule } from '../../shared/testing';
import { GioTestingPermissionProvider } from '../../shared/components/gio-permission/gio-permission.service';

describe('MessagesComponent', () => {
  let fixture: ComponentFixture<MessagesComponent>;
  let harness: MessagesHarness;
  let httpTestingController: HttpTestingController;
  const fakeRoles = [fakeRole({ name: 'ADMIN' }), fakeRole({ name: 'USER' }), fakeRole({ name: 'REVIEWER' })];

  const init = async (apiId?: string) => {
    await TestBed.configureTestingModule({
      declarations: [MessagesComponent],
      imports: [NoopAnimationsModule, GioTestingModule, MessagesModule, MatIconTestingModule],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              params: { ...(apiId ? { apiId } : {}) },
            },
          },
        },
        { provide: RoleService, useValue: { list: () => of(fakeRoles) } },
        { provide: GioTestingPermissionProvider, useValue: ['api-message-c', 'environment-message-c'] },
      ],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(MessagesComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, MessagesHarness);
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('init', () => {
    beforeEach(async () => await init());

    it('should list available channels', async () => {
      const options = await harness.getAvailableChannel();
      expect(options.length).toEqual(3);
      expect(options).toEqual(['Portal Notifications', 'Email', 'POST HTTP Message']);
    });

    it('should init form with proper values', async () => {
      expect(fixture.componentInstance.form.controls['channel'].value).toEqual('PORTAL');
      expect(await harness.getSelectedChannel()).toEqual('Portal Notifications');

      expect(fixture.componentInstance.form.controls['url'].disabled).toBeTruthy();
      expect(fixture.componentInstance.form.controls['url'].disabled).toBeTruthy();
      expect(fixture.componentInstance.form.controls['title'].disabled).toBeFalsy();

      expect(await harness.isTitleControlDisplayed()).toBeTruthy();
      expect(await harness.isUrlControlDisplayed()).toBeFalsy();
      expect(await harness.isFormHeadersControlDisplayed()).toBeFalsy();
    });

    it('should disable title and add controls for http notifications', async () => {
      await harness.selectChannel('POST HTTP Message');

      expect(fixture.componentInstance.form.controls['url'].disabled).toBeFalsy();
      expect(fixture.componentInstance.form.controls['url'].disabled).toBeFalsy();
      expect(fixture.componentInstance.form.controls['title'].disabled).toBeTruthy();

      expect(await harness.isTitleControlDisplayed()).toBeFalsy();
      expect(await harness.isUrlControlDisplayed()).toBeTruthy();
      expect(await harness.isFormHeadersControlDisplayed()).toBeTruthy();
    });
  });

  describe('Application scope', () => {
    beforeEach(async () => {
      await init('apiId');
    });

    it('should set ENVIRONMENT scope when no apiId is provided', async () => {
      expect(fixture.componentInstance.scope).toEqual('APPLICATION');
    });

    it('should list available roles', async () => {
      const recipients = await harness.getAvailableRecipients();
      expect(recipients.length).toEqual(4);
      expect(recipients).toEqual([
        'API subscribers',
        'Members with the ADMIN role on applications subscribed to this API',
        'Members with the REVIEWER role on applications subscribed to this API',
        'Members with the USER role on applications subscribed to this API',
      ]);
    });

    it('should send MAIL message with selected options', async () => {
      await harness.selectChannel('Email');
      await harness.selectRecipients(['API subscribers', 'Members with the USER role on applications subscribed to this API']);
      await harness.setTitle('Title');
      await harness.setText('Text');
      await harness.clickOnSubmitButton();

      const payload: TextMessagePayload = {
        channel: 'MAIL',
        recipient: {
          role_scope: 'APPLICATION',
          role_value: ['API_SUBSCRIBERS', 'USER'],
        },
        text: 'Text',
        title: 'Title',
      };
      expectSendFromApiRequest('apiId', payload);
    });

    it('should send HTTP message with selected options', async () => {
      await harness.selectChannel('POST HTTP Message');
      await harness.selectRecipients(['Members with the ADMIN role on applications subscribed to this API']);
      await harness.setUrl('http://alert.io');
      await harness.setText('Text');
      await harness.setHeaders('Accept', 'application/json');
      await harness.toggleUseSystemProxy();
      expect(await harness.isSubmitButtonDisabled()).toBeFalsy();
      await harness.clickOnSubmitButton();

      const payload: HttpMessagePayload = {
        channel: 'HTTP',
        recipient: { url: 'http://alert.io' },
        text: 'Text',
        useSystemProxy: true,
        params: { Accept: 'application/json' },
      };

      expectSendFromApiRequest('apiId', payload);
    });
  });

  describe('Environment scope', () => {
    beforeEach(async () => {
      await init();
    });

    it('should set ENVIRONMENT scope when no apiId is provided', async () => {
      expect(fixture.componentInstance.scope).toEqual('ENVIRONMENT');
    });

    it('should list available roles', async () => {
      const recipients = await harness.getAvailableRecipients();
      expect(recipients.length).toEqual(3);
      expect(recipients).toEqual([
        'Members with the ADMIN role on this environment',
        'Members with the REVIEWER role on this environment',
        'Members with the USER role on this environment',
      ]);
    });

    it('should send MAIL message with selected options', async () => {
      await harness.selectChannel('Portal Notifications');
      await harness.selectRecipients(['Members with the USER role on this environment']);
      await harness.setTitle('Title');
      await harness.setText('Text');
      expect(await harness.isSubmitButtonDisabled()).toBeFalsy();
      await harness.clickOnSubmitButton();

      const payload: TextMessagePayload = {
        channel: 'PORTAL',
        recipient: {
          role_scope: 'ENVIRONMENT',
          role_value: ['USER'],
        },
        text: 'Text',
        title: 'Title',
      };
      expectSendFromPortalRequest(payload);
    });

    it('should send HTTP message with selected options', async () => {
      await harness.selectChannel('POST HTTP Message');
      await harness.selectRecipients([
        'Members with the ADMIN role on this environment',
        'Members with the REVIEWER role on this environment',
      ]);
      await harness.setUrl('http://alert.io');
      await harness.setText('Text');
      expect(await harness.isSubmitButtonDisabled()).toBeFalsy();
      await harness.clickOnSubmitButton();

      const payload: HttpMessagePayload = {
        channel: 'HTTP',
        text: 'Text',
        recipient: {
          url: 'http://alert.io',
        },
        useSystemProxy: false,
        params: {},
      };
      expectSendFromPortalRequest(payload);
    });
  });

  function expectSendFromPortalRequest(payload: any) {
    const req = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.baseURL}/messages`,
      method: 'POST',
    });
    expect(req.request.body).toEqual(payload);
    req.flush(0);
  }

  function expectSendFromApiRequest(apiId: string, payload: any) {
    const req = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}/messages`,
      method: 'POST',
    });
    expect(req.request.body).toEqual(payload);
    req.flush(0);
  }
});
