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

import { MessagesComponent } from './messages.component';
import { MessagesHarness } from './messages.harness';
import { MessagesModule } from './messages.module';

import { CurrentUserService, UIRouterStateParams } from '../../ajs-upgraded-providers';
import { RoleService } from '../../services-ngx/role.service';
import { fakeRole } from '../../entities/role/role.fixture';
import { MessageService } from '../../services-ngx/message.service';
import { User } from '../../entities/user';
import { HttpMessagePayload, TextMessagePayload } from '../../entities/message/messagePayload';
import { GioHttpTestingModule } from '../../shared/testing';

describe('MigratedMessagesComponent', () => {
  let fixture: ComponentFixture<MessagesComponent>;
  let harness: MessagesHarness;
  const fakeRoles = [fakeRole({ name: 'ADMIN' }), fakeRole({ name: 'USER' }), fakeRole({ name: 'REVIEWER' })];
  const currentUser = new User();
  currentUser.userPermissions = ['api-message-c', 'environment-message-c'];

  const init = async (apiId?: string) => {
    await TestBed.configureTestingModule({
      declarations: [MessagesComponent],
      imports: [NoopAnimationsModule, GioHttpTestingModule, MessagesModule, MatIconTestingModule],
      providers: [
        {
          provide: UIRouterStateParams,
          useValue: {
            ...(apiId ? { apiId } : {}),
          },
        },
        { provide: RoleService, useValue: { list: () => of(fakeRoles) } },
        { provide: MessageService, useValue: { sendFromPortal: () => of(1), sendFromApi: () => of(2) } },
        { provide: CurrentUserService, useValue: { currentUser } },
      ],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(MessagesComponent);
    fixture.detectChanges();
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, MessagesHarness);
  };

  describe('init', () => {
    beforeEach(async () => await init());

    it('should list available channels', async () => {
      const options = await harness.getAvailableChannel();
      expect(options.length).toEqual(3);
      expect(options).toEqual(['Portal notifications', 'Email', 'POST HTTP message']);
    });

    it('should init form with proper values', async () => {
      expect(fixture.componentInstance.form.controls['channel'].value).toEqual('PORTAL');
      expect(await harness.getSelectedChannel()).toEqual('Portal notifications');

      expect(fixture.componentInstance.form.controls['url'].disabled).toBeTruthy();
      expect(fixture.componentInstance.form.controls['url'].disabled).toBeTruthy();
      expect(fixture.componentInstance.form.controls['title'].disabled).toBeFalsy();

      expect(await harness.isTitleControlDisplayed()).toBeTruthy();
      expect(await harness.isUrlControlDisplayed()).toBeFalsy();
      expect(await harness.isFormHeadersControlDisplayed()).toBeFalsy();
    });

    it('should disable title and add controls for http notifications', async () => {
      await harness.selectChannel('POST HTTP message');

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
        'Members with the ADMIN role on subscribing applications',
        'Members with the REVIEWER role on subscribing applications',
        'Members with the USER role on subscribing applications',
      ]);
    });

    it('should send MAIL message with selected options', async () => {
      const spy = jest.spyOn(TestBed.inject(MessageService), 'sendFromApi');
      await harness.selectChannel('Email');
      await harness.selectRecipients(['API subscribers', 'Members with the USER role on subscribing applications']);
      await harness.setTitle('Title');
      await harness.setText('Text');
      expect(await harness.isSubmitButtonDisabled()).toBeFalsy();
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
      expect(spy).toHaveBeenCalledWith('apiId', payload);
    });

    it('should send HTTP message with selected options', async () => {
      const spy = jest.spyOn(TestBed.inject(MessageService), 'sendFromApi');
      await harness.selectChannel('POST HTTP message');
      await harness.selectRecipients(['Members with the ADMIN role on subscribing applications']);
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

      expect(spy).toHaveBeenCalledWith('apiId', payload);
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
        'Members with the ADMIN role on ENVIRONMENT scope',
        'Members with the REVIEWER role on ENVIRONMENT scope',
        'Members with the USER role on ENVIRONMENT scope',
      ]);
    });

    it('should send MAIL message with selected options', async () => {
      const spy = jest.spyOn(TestBed.inject(MessageService), 'sendFromPortal');
      await harness.selectChannel('Portal notifications');
      await harness.selectRecipients(['Members with the USER role on ENVIRONMENT scope']);
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
      expect(spy).toHaveBeenCalledWith(payload);
    });

    it('should send HTTP message with selected options', async () => {
      const spy = jest.spyOn(TestBed.inject(MessageService), 'sendFromPortal');
      await harness.selectChannel('POST HTTP message');
      await harness.selectRecipients([
        'Members with the ADMIN role on ENVIRONMENT scope',
        'Members with the REVIEWER role on ENVIRONMENT scope',
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
      expect(spy).toHaveBeenCalledWith(payload);
    });
  });
});
