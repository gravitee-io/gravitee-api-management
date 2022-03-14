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
import { LocalizedDatePipe } from '../../../pipes/localized-date.pipe';
import { TranslateTestingModule } from '../../../test/translate-testing-module';

import { ApplicationSubscriptionsComponent } from './application-subscriptions.component';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { fakeAsync, async, ComponentFixture, TestBed, tick } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { Application, Key, Plan, Subscription } from '../../../../../projects/portal-webclient-sdk/src/lib';
import ApiKeyModeEnum = Application.ApiKeyModeEnum;
import SecurityEnum = Plan.SecurityEnum;

describe('ApplicationSubscriptionsComponent', () => {
  let component: ApplicationSubscriptionsComponent;
  let fixture: ComponentFixture<ApplicationSubscriptionsComponent>;
  const defaultConf = {
    declarations: [ApplicationSubscriptionsComponent, LocalizedDatePipe],
    imports: [TranslateTestingModule, HttpClientTestingModule, RouterTestingModule, FormsModule, ReactiveFormsModule],
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
  };

  function initFixture(): void {
    fixture = TestBed.createComponent(ApplicationSubscriptionsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  describe('empty app', () => {
    beforeEach(async(() => {
      TestBed.configureTestingModule({
        ...defaultConf,
        providers: [
          {
            provide: ActivatedRoute,
            useValue: {
              snapshot: {
                data: {},
              },
            },
          },
        ],
      }).compileComponents();
      initFixture();
    }));

    it('should create', () => {
      expect(component).toBeTruthy();
    });
  });

  describe('shared key app', () => {
    let httpTestingController: HttpTestingController;

    afterEach(() => {
      httpTestingController.verify();
    });

    beforeEach(async(() => {
      TestBed.configureTestingModule({
        ...defaultConf,
        providers: [
          {
            provide: ActivatedRoute,
            useValue: {
              snapshot: {
                data: {
                  application: {
                    id: 'application1',
                    name: 'applicationWithSharedKey',
                    api_key_mode: ApiKeyModeEnum.SHARED,
                  },
                },
                params: {
                  applicationId: 'application1',
                },
                queryParams: {},
              },
            },
          },
        ],
      }).compileComponents();
      httpTestingController = TestBed.inject(HttpTestingController);
      initFixture();
    }));

    it('should init sharedApiKey', fakeAsync(() => {
      const subscription: Subscription = {
        id: 'subscription1',
        api: 'api1',
        application: 'application1',
        plan: 'plan1',
        status: 'ACCEPTED',
      };

      const sharedApiKey: Key = { key: 'my-api-key', created_at: new Date('2022-02-22T22:22:22Z') };

      httpTestingController.expectOne('http://localhost:8083/portal/environments/DEFAULT/applications/application1/subscribers?size=-1');

      httpTestingController
        .expectOne(
          'http://localhost:8083/portal/environments/DEFAULT/subscriptions?applicationId=application1&statuses=ACCEPTED&statuses=PAUSED&statuses=PENDING',
        )
        .flush({
          data: [subscription],
          metadata: {
            plan1: {
              securityType: SecurityEnum.APIKEY as unknown as object,
            },
          },
        });
      tick();

      httpTestingController
        .expectOne('http://localhost:8083/portal/environments/DEFAULT/subscriptions/subscription1?include=keys')
        .flush({ ...subscription, keys: [sharedApiKey] });
      tick();

      expect(component).toBeTruthy();
      expect(component.sharedAPIKey).toEqual(sharedApiKey);
      expect(component.subscriptions[0].keys).toContain(sharedApiKey);
    }));

    it('should renew sharedApiKey', fakeAsync(() => {
      const subscription: Subscription = {
        id: 'subscription1',
        api: 'api1',
        application: 'application1',
        plan: 'plan1',
        status: 'ACCEPTED',
      };

      const sharedApiKey: Key = { key: 'my-api-key', created_at: new Date('2022-02-22T22:22:22Z') };

      httpTestingController.expectOne('http://localhost:8083/portal/environments/DEFAULT/applications/application1/subscribers?size=-1');

      httpTestingController
        .expectOne(
          'http://localhost:8083/portal/environments/DEFAULT/subscriptions?applicationId=application1&statuses=ACCEPTED&statuses=PAUSED&statuses=PENDING',
        )
        .flush({
          data: [subscription],
          metadata: {
            plan1: {
              securityType: SecurityEnum.APIKEY as unknown as object,
            },
          },
        });
      tick();

      httpTestingController
        .expectOne('http://localhost:8083/portal/environments/DEFAULT/subscriptions/subscription1?include=keys')
        .flush({ ...subscription, keys: [sharedApiKey] });
      tick();

      const newSharedApiKey: Key = { key: 'my-new-api-key', created_at: new Date('2022-02-23T22:22:22Z') };

      component.renewSharedApiKey();
      httpTestingController
        .expectOne('http://localhost:8083/portal/environments/DEFAULT/applications/application1/keys/_renew')
        .flush(newSharedApiKey);
      tick();
      expect(component.sharedAPIKey).toEqual(newSharedApiKey);

      httpTestingController.expectOne(
        'http://localhost:8083/portal/environments/DEFAULT/subscriptions?applicationId=application1&statuses=ACCEPTED&statuses=PAUSED&statuses=PENDING',
      );
    }));
  });

  describe('Non shared key app', () => {
    let httpTestingController: HttpTestingController;

    afterEach(() => {
      httpTestingController.verify();
    });

    beforeEach(async(() => {
      TestBed.configureTestingModule({
        ...defaultConf,
        providers: [
          {
            provide: ActivatedRoute,
            useValue: {
              snapshot: {
                data: {
                  application: {
                    id: 'application1',
                    name: 'applicationWithSharedKey',
                    api_key_mode: ApiKeyModeEnum.EXCLUSIVE,
                  },
                },
                params: {
                  applicationId: 'application1',
                },
                queryParams: {},
              },
            },
          },
        ],
      }).compileComponents();
      httpTestingController = TestBed.inject(HttpTestingController);

      initFixture();
    }));

    it('should not init sharedApiKey', fakeAsync(() => {
      const subscription: Subscription = {
        id: 'subscription1',
        api: 'api1',
        application: 'application1',
        plan: 'plan1',
        status: 'ACCEPTED',
      };

      httpTestingController.expectOne('http://localhost:8083/portal/environments/DEFAULT/applications/application1/subscribers?size=-1');

      httpTestingController
        .expectOne(
          'http://localhost:8083/portal/environments/DEFAULT/subscriptions?applicationId=application1&statuses=ACCEPTED&statuses=PAUSED&statuses=PENDING',
        )
        .flush({
          data: [subscription],
          metadata: {
            plan1: {
              securityType: SecurityEnum.APIKEY as unknown as object,
            },
          },
        });
      tick();

      httpTestingController.expectNone('http://localhost:8083/portal/environments/DEFAULT/subscriptions/subscription1?include=keys');

      expect(component).toBeTruthy();
      expect(component.sharedAPIKey).toBeUndefined();
    }));
  });
});
