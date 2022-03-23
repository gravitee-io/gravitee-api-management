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
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { fakeAsync, ComponentFixture, TestBed, tick, waitForAsync } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';

import SecurityEnum = Plan.SecurityEnum;

import { TranslateTestingModule } from '../../../test/translate-testing-module';
import { ApiKeyModeEnum, Application, Key, Plan, Subscription } from '../../../../../projects/portal-webclient-sdk/src/lib';
import { LocalizedDatePipe } from '../../../pipes/localized-date.pipe';

import { ApplicationSubscriptionsComponent } from './application-subscriptions.component';

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
    beforeEach(waitForAsync(() => {
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
        teardown: { destroyAfterEach: false },
      }).compileComponents();
      initFixture();
    }));

    it('should create', () => {
      expect(component).toBeTruthy();
    });
  });

  describe('shared key app', () => {
    let httpTestingController: HttpTestingController;

    const application: Application = {
      id: 'application1',
      name: 'applicationWithSharedKey',
      api_key_mode: ApiKeyModeEnum.SHARED,
    };

    // Because of time management during the test, we need to use dates in the past to ensure that tests won't fail
    const initialSharedApiKey: Key = { id: 'my-api-key-id', key: 'my-api-key', created_at: new Date(Date.now() - 86400000) }; // 24H ago
    const newSharedApiKey: Key = { id: 'my-new-api-key-id', key: 'my-new-api-key', created_at: new Date(Date.now() - 43200000) }; // 12H ago

    const subscription: Subscription = {
      id: 'subscription1',
      api: 'api1',
      application: application.id,
      plan: 'plan1',
      status: 'ACCEPTED',
    };

    function mockSearch() {
      httpTestingController
        .expectOne(
          `http://localhost:8083/portal/environments/DEFAULT/subscriptions?applicationId=${application.id}&statuses=ACCEPTED&statuses=PAUSED&statuses=PENDING`,
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
    }

    function mockLoadSubscription(keys: Key[]) {
      httpTestingController
        .expectOne(`http://localhost:8083/portal/environments/DEFAULT/subscriptions/${subscription.id}?include=keys`)
        .flush({ ...subscription, keys });
      tick();
    }

    function initSharedKey(): void {
      httpTestingController.expectOne(
        `http://localhost:8083/portal/environments/DEFAULT/applications/${application.id}/subscribers?size=-1`,
      );

      mockSearch();
      mockLoadSubscription([initialSharedApiKey]);
    }

    afterEach(() => {
      httpTestingController.verify();
    });

    beforeEach(waitForAsync(() => {
      TestBed.configureTestingModule({
        ...defaultConf,
        providers: [
          {
            provide: ActivatedRoute,
            useValue: {
              snapshot: {
                data: {
                  application,
                },
                params: {
                  applicationId: application.id,
                },
                queryParams: {},
              },
            },
          },
        ],
        teardown: { destroyAfterEach: false },
      }).compileComponents();
      httpTestingController = TestBed.inject(HttpTestingController);
      initFixture();
    }));

    it('should init sharedApiKey', fakeAsync(() => {
      initSharedKey();

      expect(component).toBeTruthy();
      expect(component.sharedAPIKey).toEqual(initialSharedApiKey);
      expect(component.subscriptions[0].keys).toContain(initialSharedApiKey);
    }));

    it('should renew initialSharedApiKey', fakeAsync(() => {
      initSharedKey();

      component.renewSharedApiKey();
      httpTestingController
        .expectOne(`http://localhost:8083/portal/environments/DEFAULT/applications/${application.id}/keys/_renew`)
        .flush(newSharedApiKey);
      tick();

      mockSearch();

      const endDate = new Date();
      endDate.setHours(endDate.getHours() + 2);
      const endedInitialSharedApiKey: Key = { ...initialSharedApiKey, revoked_at: endDate, expire_at: endDate };
      mockLoadSubscription([newSharedApiKey, endedInitialSharedApiKey]);

      expect(component.sharedAPIKey).toEqual(newSharedApiKey);
    }));

    it('should revoke initialSharedApiKey', fakeAsync(() => {
      initSharedKey();

      const revokedDate = new Date(initialSharedApiKey.created_at);
      revokedDate.setHours(revokedDate.getHours() + 1);
      const revokedSharedApiKey: Key = { ...initialSharedApiKey, revoked_at: revokedDate, expire_at: revokedDate };

      component.revokeSharedApiKey();
      httpTestingController
        .expectOne(
          `http://localhost:8083/portal/environments/DEFAULT/applications/${application.id}/keys/${initialSharedApiKey.id}/_revoke`,
        )
        .flush(null);
      tick();

      mockSearch();
      mockLoadSubscription([revokedSharedApiKey]);

      expect(component.sharedAPIKey).toBeUndefined();
    }));

    it('should renew initialSharedApiKey, revoke it and display the initial shared key', fakeAsync(() => {
      initSharedKey();

      // First renew
      component.renewSharedApiKey();
      httpTestingController
        .expectOne(`http://localhost:8083/portal/environments/DEFAULT/applications/${application.id}/keys/_renew`)
        .flush(newSharedApiKey);
      tick();

      const endDate = new Date();
      endDate.setHours(endDate.getHours() + 2);
      const endedInitialSharedApiKey: Key = { ...initialSharedApiKey, revoked_at: endDate, expire_at: endDate };

      mockSearch();
      mockLoadSubscription([newSharedApiKey, endedInitialSharedApiKey]);

      expect(component.sharedAPIKey).toEqual(newSharedApiKey);

      // then revoke
      const newSharedApiKeyRevokedDate = new Date(newSharedApiKey.created_at);
      newSharedApiKeyRevokedDate.setHours(newSharedApiKeyRevokedDate.getHours() + 1);
      const revokedNewSharedApiKey: Key = {
        ...newSharedApiKey,
        revoked_at: newSharedApiKeyRevokedDate,
        expire_at: newSharedApiKeyRevokedDate,
      };

      component.revokeSharedApiKey();
      httpTestingController
        .expectOne(`http://localhost:8083/portal/environments/DEFAULT/applications/${application.id}/keys/${newSharedApiKey.id}/_revoke`)
        .flush(null);
      tick();

      mockSearch();
      mockLoadSubscription([revokedNewSharedApiKey, endedInitialSharedApiKey]);

      expect(component.sharedAPIKey).toEqual(endedInitialSharedApiKey);
    }));
  });

  describe('Non shared key app', () => {
    let httpTestingController: HttpTestingController;

    afterEach(() => {
      httpTestingController.verify();
    });

    beforeEach(waitForAsync(() => {
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
        teardown: { destroyAfterEach: false },
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
