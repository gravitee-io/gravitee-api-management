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
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { InteractivityChecker } from '@angular/cdk/a11y';

import { ApiProxyHealthCheckComponent } from './api-proxy-health-check.component';
import { ApiProxyHealthCheckModule } from './api-proxy-health-check.module';
import { CurrentUserService } from '../../../../../ajs-upgraded-providers';
import { User } from '../../../../../entities/user';
import { GioHttpTestingModule } from '../../../../../shared/testing';

describe('ApiProxyHealthCheckComponent', () => {
  const currentUser = new User();
  currentUser.userPermissions = ['api-definition-u'];

  let fixture: ComponentFixture<ApiProxyHealthCheckComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, ApiProxyHealthCheckModule],
      providers: [{ provide: CurrentUserService, useValue: { currentUser } }],
    });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ApiProxyHealthCheckComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    TestbedHarnessEnvironment.documentRootLoader(fixture);

    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should work', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });
});
