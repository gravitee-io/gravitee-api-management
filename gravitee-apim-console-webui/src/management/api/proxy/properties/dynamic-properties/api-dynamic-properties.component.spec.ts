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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HarnessLoader } from '@angular/cdk/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';

import { ApiDynamicPropertiesComponent } from './api-dynamic-properties.component';
import { ApiDynamicPropertiesModule } from './api-dynamic-properties.module';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../../shared/testing';
import { UIRouterStateParams } from '../../../../../ajs-upgraded-providers';
import { GioUiRouterTestingModule } from '../../../../../shared/testing/gio-uirouter-testing-module';
import { Api } from '../../../../../entities/management-api-v2';

describe('ApiDynamicPropertiesComponent', () => {
  const API_ID = 'apiId';
  let fixture: ComponentFixture<ApiDynamicPropertiesComponent>;
  let component: ApiDynamicPropertiesComponent;
  let httpTestingController: HttpTestingController;
  let loader: HarnessLoader;
  let rootLoader: HarnessLoader;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, ApiDynamicPropertiesModule, GioUiRouterTestingModule, MatIconTestingModule],
      providers: [{ provide: UIRouterStateParams, useValue: { apiId: API_ID } }],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(ApiDynamicPropertiesComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    loader = TestbedHarnessEnvironment.loader(fixture);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should display default dynamic properties page', async () => {
    expect(component).toBeTruthy();
  });

  function expectGetApi(api: Api) {
    const req = httpTestingController.expectOne({
      method: 'GET',
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`,
    });
    req.flush(api);
  }
});
