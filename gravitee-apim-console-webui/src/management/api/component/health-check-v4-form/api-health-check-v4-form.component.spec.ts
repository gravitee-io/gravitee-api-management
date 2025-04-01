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
import { Component } from '@angular/core';
import { FormControl, UntypedFormGroup } from '@angular/forms';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { ApiHealthCheckV4FormHarness } from './api-health-check-v4-form.harness';
import { ApiHealthCheckV4FormModule } from './api-health-check-v4-form.module';

import { GioTestingModule } from '../../../../shared/testing';

@Component({
  template: ` <api-health-check-v4-form [healthCheckForm]="healthCheckForm"></api-health-check-v4-form> `,
  standalone: false,
})
class TestComponent {
  healthCheckForm = new UntypedFormGroup({
    enabled: new FormControl({
      value: false,
      disabled: false,
    }),
    configuration: new FormControl(
      {
        value: {},
        disabled: false,
      } ?? {},
    ),
  });
}

describe('ApiHealthCheckV4FormComponent', () => {
  let component: TestComponent;
  let fixture: ComponentFixture<TestComponent>;
  let httpTestingController: HttpTestingController;
  let loader: HarnessLoader;
  let healthCheckConfigurationHarness: ApiHealthCheckV4FormHarness;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [TestComponent],
      imports: [NoopAnimationsModule, GioTestingModule, ApiHealthCheckV4FormModule],
    }).compileComponents();

    fixture = TestBed.createComponent(TestComponent);
    component = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    healthCheckConfigurationHarness = await loader.getHarness(ApiHealthCheckV4FormHarness);
    fixture.detectChanges();

    expect(component).toBeTruthy();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should initialize the component with default configuration', async () => {
    expect(await healthCheckConfigurationHarness.getEnableToggleValue()).toBeFalsy();

    await healthCheckConfigurationHarness.toggleEnableInput();
    expect(await healthCheckConfigurationHarness.getEnableToggleValue()).toBeTruthy();
  });
});
