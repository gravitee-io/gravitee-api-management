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
import { InteractivityChecker } from '@angular/cdk/a11y';
import { HttpTestingController, TestRequest } from '@angular/common/http/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { BrowserAnimationsModule, NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TestElement } from '@angular/cdk/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { MatPaginatorHarness } from '@angular/material/paginator/testing';

import { IntegrationsComponent } from './integrations.component';
import { IntegrationsHarness } from './integrations.harness';
import { Integration, IntegrationResponse } from './integrations.model';
import { IntegrationsModule } from './integrations.module';

import { CONSTANTS_TESTING, GioTestingModule } from '../../shared/testing';
import { fakeIntegration } from '../../entities/integrations/integration.fixture';

describe('IntegrationsComponent', () => {
  let fixture: ComponentFixture<IntegrationsComponent>;
  let componentHarness: IntegrationsHarness;
  let httpTestingController: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [IntegrationsComponent],
      imports: [IntegrationsModule, GioTestingModule, BrowserAnimationsModule, NoopAnimationsModule, RouterTestingModule],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
          isTabbable: () => true, // This traps focus checks and so avoid warnings when dealing with
        },
      })
      .compileComponents();
  });

  beforeEach(async () => {
    fixture = TestBed.createComponent(IntegrationsComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, IntegrationsHarness);
    fixture.componentInstance.filters = {
      pagination: { index: 1, size: 10 },
      searchTerm: '',
    };
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('table', () => {
    it('should display correct number of rows', async () => {
      const fakeIntegrations: Integration[] = [fakeIntegration(), fakeIntegration(), fakeIntegration()];
      const fakeIntegrationResponse: IntegrationResponse = {
        data: fakeIntegrations,
        pagination: {},
      };

      expectIntegrationGetRequest(fakeIntegrationResponse);

      const rows = await componentHarness.rowsNumber();
      expect(rows).toEqual(fakeIntegrations.length);
    });
  });

  describe('pagination', () => {
    it('should request proper url', async () => {
      const fakeIntegrations: Integration[] = [fakeIntegration(), fakeIntegration(), fakeIntegration(), fakeIntegration()];
      const fakeIntegrationResponse: IntegrationResponse = {
        data: fakeIntegrations,
        pagination: {},
      };
      expectIntegrationGetRequest(fakeIntegrationResponse, 1, 10);
      const pagination: MatPaginatorHarness = await componentHarness.getPagination();

      await pagination.setPageSize(5);
      expectIntegrationGetRequest(fakeIntegrationResponse, 1, 5);
      pagination.getPageSize().then((value) => {
        expect(value).toEqual(5);
      });

      await pagination.setPageSize(25);
      expectIntegrationGetRequest(fakeIntegrationResponse, 1, 25);
      pagination.getPageSize().then((value) => {
        expect(value).toEqual(25);
      });
    });
  });

  describe('banner', () => {
    it('should be visible when no integrations', async () => {
      const fakeIntegrationResponse: IntegrationResponse = {
        data: [],
        pagination: {},
      };
      expectIntegrationGetRequest(fakeIntegrationResponse);
      const banner: TestElement = await componentHarness.getBanner();
      expect(banner).toBeTruthy();
    });

    it('should be hidden when integration are present', async () => {
      const fakeIntegrationResponse: IntegrationResponse = {
        data: [fakeIntegration()],
        pagination: {},
      };
      expectIntegrationGetRequest(fakeIntegrationResponse);
      const banner: TestElement = await componentHarness.getBanner();
      expect(banner).toBeFalsy();
    });
  });

  function expectIntegrationGetRequest(fakeIntegrations: IntegrationResponse, page: number = 1, size: number = 10): void {
    const req: TestRequest = httpTestingController.expectOne(
      `${CONSTANTS_TESTING.env.v2BaseURL}/integrations/?page=${page}&perPage=${size}`,
    );
    req.flush(fakeIntegrations);
    expect(req.request.method).toEqual('GET');
  }
});
