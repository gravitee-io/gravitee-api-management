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
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { mockProvider, Spectator, createRoutingFactory } from '@ngneat/spectator/jest';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { CurrentUserService } from '../../services/current-user.service';
import { TranslateTestingModule } from '../../test/translate-testing-module';

import { AlertMode, GvAlertComponent } from './gv-alert.component';

describe('GvAlertComponent', () => {
  const createComponent = createRoutingFactory({
    component: GvAlertComponent,
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
    imports: [RouterTestingModule, TranslateTestingModule, FormsModule, ReactiveFormsModule, HttpClientTestingModule],
    providers: [mockProvider(CurrentUserService)],
    data: { application: { id: 'appId', name: 'Admin Default application' } },
  });

  let spectator: Spectator<GvAlertComponent>;
  let component: GvAlertComponent;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    spectator = createComponent();
    component = spectator.component;
    httpTestingController = spectator.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should create alert', () => {
    spectator.detectChanges();
    expectGetApplicationPermission();
    expectGetNotifiers();
    expectGetSubscriptions();

    component.mode = AlertMode.CREATION;
    component.status = {
      available_plugins: 1,
      enabled: true,
    };
    component.alert = {
      id: 'c8b67a64-ba8a-453e-b67a-64ba8a553e83',
      enabled: true,
      type: 'RESPONSE_TIME',
      description: 'Yo ',
      response_time: 1,
      duration: 1,
      time_unit: 'MINUTES',
    };

    component.alertForm.controls.type.setValue('STATUS');
    component.alertForm.controls.duration.setValue(1);
    component.alertForm.controls.timeUnit.setValue('MINUTES');
    component.alertForm.controls.description.setValue('My description');

    component.addAlert();

    const req = httpTestingController.expectOne({
      method: 'POST',
      url: 'http://localhost:8083/portal/environments/DEFAULT/applications/appId/alerts',
    });
    expect(req.request.body).toEqual({
      api: '',
      description: 'My description',
      duration: 1,
      enabled: true,
      status_code: '4xx',
      status_percent: '1',
      time_unit: 'MINUTES',
      type: 'STATUS',
    });
  });

  it('should create alert with webhook', () => {
    spectator.detectChanges();
    expectGetApplicationPermission();
    expectGetNotifiers();
    expectGetSubscriptions();

    component.mode = AlertMode.CREATION;
    component.status = {
      available_plugins: 1,
      enabled: true,
    };
    component.alert = {
      id: 'c8b67a64-ba8a-453e-b67a-64ba8a553e83',
      enabled: true,
      type: 'RESPONSE_TIME',
      description: '',
      response_time: 1,
      duration: 1,
      time_unit: 'MINUTES',
    };

    component.alertForm.controls.hasWebhook.setValue(true);
    component.alertForm.controls.webhookMethod.setValue('GET');
    component.alertForm.controls.webhookUrl.setValue('http://fox.trot.com');

    component.addAlert();

    const req = httpTestingController.expectOne({
      method: 'POST',
      url: 'http://localhost:8083/portal/environments/DEFAULT/applications/appId/alerts',
    });
    expect(req.request.body).toEqual({
      api: '',
      description: '',
      duration: '1',
      enabled: true,
      status_code: '4xx',
      status_percent: '1',
      time_unit: 'MINUTES',
      type: 'STATUS',
      webhook: {
        httpMethod: 'GET',
        url: 'http://fox.trot.com',
      },
    });
  });

  function expectGetApplicationPermission() {
    httpTestingController
      .expectOne({ method: 'GET', url: 'http://localhost:8083/portal/environments/DEFAULT/permissions?applicationId=appId' })
      .flush({
        SUBSCRIPTION: ['R', 'C', 'D', 'U'],
        ALERT: ['R', 'C', 'D', 'U'],
      });
  }

  function expectGetNotifiers() {
    httpTestingController.expectOne({ method: 'GET', url: 'http://localhost:8083/portal/environments/DEFAULT/notifiers' }).flush([
      {
        id: 'webhook-notifier',
        name: 'Webhook',
        description: 'The Gravitee.IO Parent POM provides common settings for all Gravitee components.',
        version: '1.1.0',
      },
      {
        id: 'default-email',
        name: 'System email',
        description: 'System email notifier',
      },
    ]);
  }

  function expectGetSubscriptions() {
    httpTestingController
      .expectOne({
        method: 'GET',
        url: 'http://localhost:8083/portal/environments/DEFAULT/subscriptions?applicationId=appId&statuses=ACCEPTED&statuses=PAUSED&statuses=PENDING&size=-1',
      })
      .flush({
        data: [
          {
            id: '5d12a45c-e494-4263-92a4-5ce49402632a',
            api: 'bd12c4cb-0c64-480a-92c4-cb0c64180aa1',
            application: 'eac6177f-2fca-4add-8617-7f2fca3add0a',
            plan: '5e239ba2-871e-4283-a39b-a2871ec28313',
            created_at: '2021-12-30T16:48:42.09Z',
            processed_at: '2021-12-30T16:48:42.111Z',
            start_at: '2021-12-30T16:48:42.105Z',
            subscribed_by: '407b2d9c-91d8-4d7a-bb2d-9c91d84d7a93',
            status: 'ACCEPTED',
          },
        ],
        metadata: {
          '5e239ba2-871e-4283-a39b-a2871ec28313': {
            name: '1 Dolar',
          },
          'bd12c4cb-0c64-480a-92c4-cb0c64180aa1': {
            pictureUrl: 'http://localhost:4100/portal/environments/DEFAULT/apis/bd12c4cb-0c64-480a-92c4-cb0c64180aa1/picture?1640882827537',
            name: 'Fox',
            entrypoints: [
              {
                target: 'https://api.company.com/fox',
              },
            ],
            state: 'CREATED',
            version: '1',
          },
        },
      });
  }
});
