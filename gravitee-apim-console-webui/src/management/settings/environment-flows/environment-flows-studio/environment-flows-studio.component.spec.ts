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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';

import { EnvironmentFlowsStudioComponent } from './environment-flows-studio.component';
import { EnvironmentFlowsStudioHarness } from './environment-flows-studio.harness';

import { GioTestingModule } from '../../../../shared/testing';

describe('EnvironmentFlowsStudioComponent', () => {
  let component: EnvironmentFlowsStudioComponent;
  let fixture: ComponentFixture<EnvironmentFlowsStudioComponent>;
  let componentHarness: EnvironmentFlowsStudioHarness;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EnvironmentFlowsStudioComponent, GioTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(EnvironmentFlowsStudioComponent);
    component = fixture.componentInstance;
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, EnvironmentFlowsStudioHarness);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
    expect(componentHarness).toBeTruthy();
  });
});
