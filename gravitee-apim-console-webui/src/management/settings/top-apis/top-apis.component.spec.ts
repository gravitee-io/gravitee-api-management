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
import { HttpTestingController } from '@angular/common/http/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { GioConfirmDialogHarness } from '@gravitee/ui-particles-angular';
import { MatButtonHarness } from '@angular/material/button/testing';
import { BrowserAnimationsModule, NoopAnimationsModule } from '@angular/platform-browser/animations';

import { TopApisComponent } from './top-apis.component';
import { TopApisModule } from './top-apis.module';
import { TopApisHarness } from './top-apis.harness';
import { TopApi } from './top-apis.model';
import { AddTopApisDialogHarness } from './add-top-apis-dialog/add-top-apis-dialog.harness';

import { GioTestingPermissionProvider } from '../../../shared/components/gio-permission/gio-permission.service';
import { Constants } from '../../../entities/Constants';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { fakeTopApi } from '../../../entities/top-apis/top-apis.fixture';

describe('TopApisComponent', () => {
  let fixture: ComponentFixture<TopApisComponent>;
  let componentHarness: TopApisHarness;
  let httpTestingController: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [TopApisComponent],
      imports: [GioTestingModule, TopApisModule, BrowserAnimationsModule, NoopAnimationsModule],
      providers: [
        {
          provide: GioTestingPermissionProvider,
          useValue: ['environment-top_apis-c', 'environment-top_apis-u', 'environment-top_apis-d'],
        },
        {
          provide: Constants,
          useValue: CONSTANTS_TESTING,
        },
      ],
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
    fixture = TestBed.createComponent(TopApisComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, TopApisHarness);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('table', () => {
    it('should display correct number of rows', async () => {
      const fakeTopApis: TopApi[] = [fakeTopApi({ api: 'd283' }), fakeTopApi({ api: 'dqer3' }), fakeTopApi({ api: 'dassfae83' })];
      expectTopApisGetRequest(fakeTopApis);
      const rows = await componentHarness.rowsNumber();
      expect(rows).toEqual(fakeTopApis.length);
    });

    it('should delete item from TopAPIs list', async () => {
      const fakeTopApis: TopApi[] = [fakeTopApi({ api: 'd283' }), fakeTopApi({ api: 'dqer3' }), fakeTopApi({ api: 'dassfae83' })];
      expectTopApisGetRequest(fakeTopApis);

      await componentHarness.deleteTopApi(1);
      const dialogHarness = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(GioConfirmDialogHarness);
      await dialogHarness.confirm();

      expectTopApisDeleteRequest(fakeTopApis[1]);
      expectTopApisGetRequest(fakeTopApis);
    });

    it('should move topApis up', async () => {
      const fakeTopApis: TopApi[] = [fakeTopApi({ api: 'd283' }), fakeTopApi({ api: 'dqer3' }), fakeTopApi({ api: 'dassfae83' })];
      expectTopApisGetRequest(fakeTopApis);

      await componentHarness.moveTopApiUp(2);

      expectTopApisPutRequest();
    });

    it('should move topApis down', async () => {
      const fakeTopApis: TopApi[] = [fakeTopApi({ api: 'd283' }), fakeTopApi({ api: 'dqer3' }), fakeTopApi({ api: 'dassfae83' })];
      expectTopApisGetRequest(fakeTopApis);

      await componentHarness.moveTopApiDown(0);

      expectTopApisPutRequest();
    });
  });

  describe('header with Add API button', () => {
    it('should be able to add new API with dialog', async () => {
      const fakeTopApis: TopApi[] = [fakeTopApi({ api: 'd283' }), fakeTopApi({ api: 'dqer3' }), fakeTopApi({ api: 'dassfae83' })];
      expectTopApisGetRequest(fakeTopApis);

      const addNewApisButton: MatButtonHarness = await componentHarness.getAddButton();
      await addNewApisButton.click();

      const addTopApisDialog = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(AddTopApisDialogHarness);
      expect(addTopApisDialog).toBeTruthy();

      await addTopApisDialog.fillFormAndSubmit('search term', () => {
        expectApisSearchPostRequest([
          {
            id: 'la;kndvlka',
            name: 'test routing',
            version: '1.1',
            description: '21aefvtinggg',
            visibility: 'PRIVATE',
            state: 'STARTED',
            numberOfRatings: 0,
            tags: [],
            created_at: 1688646887369,
            updated_at: 1709555039399,
            owner: {
              id: '4015f9f2-c0a4-4c0c-95f9-f2c0a4fc0c4c',
              email: 'test',
              displayName: 'asd master',
              type: 'USasdfER',
            },
            picture_url: '/url',
            virtual_hosts: [
              {
                path: '/',
              },
            ],
            lifecycle_state: 'PUBLISHED',
            context_path: '/dynamicrouting',
            healthcheck_enabled: false,
            definition_context: {
              origin: 'management',
              mode: 'fully_managed',
              syncFrom: 'management',
              syncFromKubernetes: false,
              syncFromManagement: true,
              originManagement: true,
              originKubernetes: false,
            },
            gravitee: '2.0.0',
          },
        ]);
      });

      expectTopApisPostRequest();
    });
  });

  function expectTopApisGetRequest(fakeTopApis: TopApi[]): void {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/environments/DEFAULT/configuration/top-apis/`);
    req.flush(fakeTopApis);
    expect(req.request.method).toEqual('GET');
  }

  function expectTopApisDeleteRequest(topApi: TopApi): void {
    const req = httpTestingController.expectOne(
      `${CONSTANTS_TESTING.org.baseURL}/environments/DEFAULT/configuration/top-apis/${topApi.api}`,
    );
    req.flush([]);
    expect(req.request.method).toEqual('DELETE');
  }

  function expectTopApisPutRequest(): void {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/environments/DEFAULT/configuration/top-apis/`);
    req.flush([]);
    expect(req.request.method).toEqual('PUT');
  }

  function expectApisSearchPostRequest(apisList): void {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/_search?page=1&perPage=10`);
    req.flush({
      data: apisList,
    });
    expect(req.request.method).toEqual('POST');
  }

  function expectTopApisPostRequest(): void {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/environments/DEFAULT/configuration/top-apis/`);
    req.flush([]);
    expect(req.request.method).toEqual('POST');
  }
});
