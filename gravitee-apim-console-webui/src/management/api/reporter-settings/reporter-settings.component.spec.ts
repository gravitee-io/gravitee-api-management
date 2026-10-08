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
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ActivatedRoute } from '@angular/router';

import { ReporterSettingsComponent } from './reporter-settings.component';
import { ReporterSettingsProxyHarness } from './reporter-settings-proxy/reporter-settings-proxy.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { ApiType, ApiV4, fakeProxyApiV4 } from '../../../entities/management-api-v2';

describe('ReporterSettingsComponent', () => {
  const API_ID = 'api-id';
  let fixture: ComponentFixture<ReporterSettingsComponent>;
  let httpTestingController: HttpTestingController;
  let loader: HarnessLoader;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, ReporterSettingsComponent, MatIconTestingModule],
      providers: [{ provide: ActivatedRoute, useValue: { snapshot: { params: { apiId: API_ID } } } }],
    }).compileComponents();

    fixture = TestBed.createComponent(ReporterSettingsComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should display the proxy reporter settings for an AUTHZ API', async () => {
    expectApiGetRequest(fakeProxyApiV4({ id: API_ID, type: 'AUTHZ' }));

    expect(await loader.getHarnessOrNull(ReporterSettingsProxyHarness)).not.toBeNull();
  });

  it('should display an empty state for an API type without reporter settings', async () => {
    expectApiGetRequest(fakeProxyApiV4({ id: API_ID, type: 'UNSUPPORTED' as ApiType }));

    expect(fixture.nativeElement.querySelector('gio-card-empty-state')?.textContent).toContain('Report settings not available');
  });

  function expectApiGetRequest(api: ApiV4) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`, method: 'GET' }).flush(api);
    fixture.detectChanges();
  }
});
