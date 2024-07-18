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
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { GioMenuSearchService, LICENSE_CONFIGURATION_TESTING } from '@gravitee/ui-particles-angular';
import { ActivatedRoute } from '@angular/router';

import { ApiNavigationModule } from './api-navigation.module';
import { ApiNavigationComponent } from './api-navigation.component';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { Api, fakeApiV1, fakeApiV2, fakeApiV4 } from '../../../entities/management-api-v2';
import { GioPermissionService, GioTestingPermissionProvider } from '../../../shared/components/gio-permission/gio-permission.service';
import { Constants } from '../../../entities/Constants';
import { IntegrationsService } from '../../../services-ngx/integrations.service';

describe('ApiNavigationComponent', () => {
  const API_ID = 'apiId';
  const ENVIRONMENT_ID = 'envid';

  let fixture: ComponentFixture<ApiNavigationComponent>;
  let apiNgNavigationComponent: ApiNavigationComponent;
  let httpTestingController: HttpTestingController;

  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('without quality score', () => {
    beforeEach(async () => {
      await TestBed.configureTestingModule({
        imports: [ApiNavigationModule, MatIconTestingModule, NoopAnimationsModule, GioTestingModule],
        providers: [
          { provide: IntegrationsService },
          {
            provide: ActivatedRoute,
            useValue: {
              snapshot: { params: { apiId: API_ID, envId: ENVIRONMENT_ID } },
              pathFromRoot: [{ snapshot: { url: { path: `${ENVIRONMENT_ID}/apis/${API_ID}` } } }],
            },
          },
          {
            provide: GioTestingPermissionProvider,
            useValue: ['environment-api-c'],
          },
          { provide: Constants, useValue: CONSTANTS_TESTING },
          { provide: 'LicenseConfiguration', useValue: LICENSE_CONFIGURATION_TESTING },
        ],
      }).compileComponents();

      fixture = TestBed.createComponent(ApiNavigationComponent);
      apiNgNavigationComponent = await fixture.componentInstance;
      httpTestingController = TestBed.inject(HttpTestingController);
    });

    describe('Banners', () => {
      it('should display "Out of sync" banner', (done) => {
        apiNgNavigationComponent.banners$.subscribe((banners) => {
          expect(banners).toEqual([
            {
              title: 'This API is out of sync.',
              type: 'warning',
            },
          ]);
          done();
        });

        httpTestingController
          .expectOne({
            url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`,
            method: 'GET',
          })
          .flush(
            fakeApiV4({
              id: API_ID,
              deploymentState: 'NEED_REDEPLOY',
            }),
          );

        httpTestingController
          .expectOne({
            url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/deployments/_verify`,
            method: 'GET',
          })
          .flush({ ok: true });
      });
      it('should display "API version out-of-date" banner', (done) => {
        apiNgNavigationComponent.banners$.subscribe((banners) => {
          expect(banners.length).toEqual(1);
          expect(banners[0]).toMatchObject({
            title: 'API version out-of-date',
            type: 'warning',
          });
          done();
        });

        httpTestingController
          .expectOne({
            url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`,
            method: 'GET',
          })
          .flush(
            fakeApiV1({
              id: API_ID,
            }),
          );

        httpTestingController
          .expectOne({
            url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/deployments/_verify`,
            method: 'GET',
          })
          .flush({ ok: true });
      });
      it('should display "API cannot be deployed" banner', (done) => {
        apiNgNavigationComponent.banners$.subscribe((banners) => {
          expect(banners).toEqual([
            {
              title: 'This API cannot be deployed.',
              body: 'The current configuration uses features not in your license.',
              type: 'error',
            },
          ]);
          done();
        });

        httpTestingController
          .expectOne({
            url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`,
            method: 'GET',
          })
          .flush(
            fakeApiV4({
              id: API_ID,
              deploymentState: 'NEED_REDEPLOY',
            }),
          );

        httpTestingController
          .expectOne({
            url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/deployments/_verify`,
            method: 'GET',
          })
          .flush({ ok: false });
      });
    });
  });

  describe('side nave search items', () => {
    let addSearchItemByGroupIds: jest.SpyInstance;
    const menuSearchService = new GioMenuSearchService();

    beforeEach(async () => {
      addSearchItemByGroupIds = jest.spyOn(menuSearchService, 'addMenuSearchItems');

      await TestBed.configureTestingModule({
        imports: [ApiNavigationModule, MatIconTestingModule, NoopAnimationsModule, GioTestingModule],
        providers: [
          {
            provide: ActivatedRoute,
            useValue: {
              snapshot: { params: { apiId: API_ID, envId: ENVIRONMENT_ID } },
              pathFromRoot: [{ snapshot: { url: { path: `${ENVIRONMENT_ID}/apis/${API_ID}` } } }],
            },
          },
          {
            provide: GioPermissionService,
            useValue: {
              hasAnyMatching: () => true,
            },
          },
          { provide: Constants, useValue: CONSTANTS_TESTING },
          { provide: 'LicenseConfiguration', useValue: LICENSE_CONFIGURATION_TESTING },
          { provide: GioMenuSearchService, useValue: menuSearchService },
        ],
      }).compileComponents();

      fixture = TestBed.createComponent(ApiNavigationComponent);
      apiNgNavigationComponent = fixture.componentInstance;
      httpTestingController = TestBed.inject(HttpTestingController);
    });

    it('should compute menu search items for V4 API', async () => {
      fixture.detectChanges();
      expectApiGetRequest(fakeApiV4({ id: API_ID }));

      expect(addSearchItemByGroupIds).toHaveBeenCalledTimes(1);
      expect(addSearchItemByGroupIds).toHaveBeenCalledWith(
        expect.arrayContaining(
          [
            'Configuration',
            'General',
            'User Permissions',
            'Properties',
            'Resources',
            'Audit Logs',
            'Entrypoints',
            'Response Templates',
            'CORS',
            'Endpoints',
            'Endpoints',
            'Consumers',
            'Plans',
            'Subscriptions',
            'Documentation',
            'Custom Pages',
            'Default Pages',
            'Deployment',
            'Configuration',
            'Policies',
            'API Traffic',
            'Runtime Logs',
            'Settings',
          ].map((name) =>
            expect.objectContaining({
              name,
              routerLink: expect.not.stringContaining('./') && expect.stringContaining(`${ENVIRONMENT_ID}/apis/${API_ID}/`),
            }),
          ),
        ),
      );
    });

    it('should compute menu search items for V2 API', async () => {
      fixture.detectChanges();
      expectApiGetRequest(fakeApiV2({ id: API_ID }));

      expect(addSearchItemByGroupIds).toHaveBeenCalledTimes(1);
      expect(addSearchItemByGroupIds).toHaveBeenCalledWith(
        expect.arrayContaining(
          [
            'Policy Studio',
            'Messages',
            'Info',
            'Plans',
            'Subscriptions',
            'Documentation',
            'Pages',
            'Metadata',
            'User and group access',
            'Entrypoints',
            'CORS',
            'Deployments',
            'Response Templates',
            'Properties',
            'Resources',
            'Endpoints',
            'Failover',
            'Health-check',
            'Health-check dashboard',
            'Overview',
            'Logs',
            'Path mappings',
            'Audit',
            'History',
            'Events',
            'Notification settings',
          ].map((name) =>
            expect.objectContaining({
              name,
              routerLink: expect.not.stringContaining('./') && expect.stringContaining(`${ENVIRONMENT_ID}/apis/${API_ID}/`),
            }),
          ),
        ),
      );
    });
  });

  function expectApiGetRequest(api: Api) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`, method: 'GET' }).flush(api);
  }
});
