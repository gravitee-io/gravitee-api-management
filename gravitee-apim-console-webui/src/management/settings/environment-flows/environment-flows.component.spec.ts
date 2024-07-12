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

import { EnvironmentFlowsComponent } from './environment-flows.component';
import { EnvironmentFlowsHarness } from './environment-flows.harness';

describe('EnvironmentFlowsComponent', () => {
  let component: EnvironmentFlowsComponent;
  let fixture: ComponentFixture<EnvironmentFlowsComponent>;
  let componentHarness: EnvironmentFlowsHarness;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EnvironmentFlowsComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(EnvironmentFlowsComponent);
    component = fixture.componentInstance;
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, EnvironmentFlowsHarness);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
    expect(componentHarness).toBeTruthy();
  });
});
