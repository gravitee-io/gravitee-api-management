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
import { HttpTestingController } from '@angular/common/http/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { MatIconTestingModule } from '@angular/material/icon/testing';

import { ApiV4PolicyStudioDesignComponent } from './api-v4-policy-studio-design.component';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../shared/testing';
import { ApiV4PolicyStudioModule } from '../api-v4-policy-studio.module';
import {
  Api,
  ApiPlansResponse,
  ConnectorPlugin,
  fakeApiV4,
  fakeConnectorPlugin,
  fakePlanV4,
  PlanV4,
} from '../../../../entities/management-api-v2';
import { UIRouterStateParams } from '../../../../ajs-upgraded-providers';

describe('ApiV4PolicyStudioDesignComponent', () => {
  const API_ID = 'api-id';

  let fixture: ComponentFixture<ApiV4PolicyStudioDesignComponent>;
  let component: ApiV4PolicyStudioDesignComponent;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, ApiV4PolicyStudioModule, MatIconTestingModule],
      providers: [{ provide: UIRouterStateParams, useValue: { apiId: API_ID } }],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(ApiV4PolicyStudioDesignComponent);
    component = fixture.componentInstance;

    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('ngOnInit', () => {
    it('should fetch initial data', async () => {
      const api = fakeApiV4({
        id: API_ID,
        name: 'my brand new API',
        type: 'MESSAGE',
        listeners: [
          {
            type: 'SUBSCRIPTION',
            entrypoints: [
              {
                type: 'webhook',
              },
            ],
          },
        ],
        endpointGroups: [
          {
            name: 'default-group',
            type: 'kafka',
            endpoints: [
              {
                name: 'default',
                type: 'kafka',
                weight: 1,
                inheritConfiguration: false,
                configuration: {
                  bootstrapServers: 'localhost:9092',
                },
              },
            ],
          },
        ],
        flows: [
          {
            name: 'my flow',
            enabled: true,
          },
        ],
      });

      expectGetApi(api);

      expectEntrypointsGetRequest([{ id: 'webhook', name: 'Webhook' }]);
      expectEndpointsGetRequest([{ id: 'kafka', name: 'Kafka' }]);

      expectListApiPlans(API_ID, [fakePlanV4()]);

      expect(component.apiType).toEqual('MESSAGE');
      expect(component.commonFlows.length).toEqual(1);
      expect(component.endpointsInfo.length).toEqual(1);
      expect(component.entrypointsInfo.length).toEqual(1);
      expect(component.plans.length).toEqual(1);
    });
  });

  function expectGetApi(api: Api) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`, method: 'GET' }).flush(api);
  }

  function expectEntrypointsGetRequest(connectors: Partial<ConnectorPlugin>[]) {
    const fullConnectors = connectors.map((partial) => fakeConnectorPlugin(partial));
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.v2BaseURL}/plugins/entrypoints` }).flush(fullConnectors);
  }

  function expectEndpointsGetRequest(connectors: Partial<ConnectorPlugin>[]) {
    const fullConnectors = connectors.map((partial) => fakeConnectorPlugin(partial));
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.v2BaseURL}/plugins/endpoints`, method: 'GET' }).flush(fullConnectors);
  }

  function expectListApiPlans(apiId: string, plans: PlanV4[]) {
    const fakeApiPlansResponse: ApiPlansResponse = {
      data: [...plans],
    };
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/plans?page=1&perPage=9999&status=PUBLISHED`, method: 'GET' })
      .flush(fakeApiPlansResponse);
  }
});
