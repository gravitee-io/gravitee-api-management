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
import { Component } from '@angular/core';

import { StepperMenuStepHarness } from './stepper-menu-step.harness';
import { StepperMenuStepComponent } from './stepper-menu-step.component';

import { ApiCreationV4Module } from '../../../api-creation-v4.module';

@Component({
  template: `<api-creation-step [stepNumber]="stepNumber" [currentStep]="currentStep">step</api-creation-step>`,
})
class TestHostComponent {
  public stepNumber = 1;
  public currentStep = 1;
}

describe('ApiCreationStepComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let component: TestHostComponent;
  let harness: StepperMenuStepHarness;

  const initConfigureTestingModule = async (currentStep: number, stepNumber: number) => {
    await TestBed.configureTestingModule({
      declarations: [TestHostComponent, StepperMenuStepComponent],
      imports: [ApiCreationV4Module],
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    component = fixture.componentInstance;
    component.stepNumber = stepNumber;
    component.currentStep = currentStep;
    fixture.detectChanges();
    harness = await TestbedHarnessEnvironment.loader(fixture).getHarness(StepperMenuStepHarness);
  };

  it('should show step number and title', async () => {
    await initConfigureTestingModule(1, 1);
    expect(component.stepNumber).toEqual(1);
    expect(await harness.getStepTitle()).toEqual('step');
    expect(await harness.getStepNumber()).toEqual('1');
  });

  it('should show active state', async () => {
    await initConfigureTestingModule(1, 1);
    expect(await harness.getStepIconName()).toEqual('edit-pencil');
  });

  it('should show filled state', async () => {
    await initConfigureTestingModule(2, 1);
    expect(await harness.getStepIconName()).toEqual('nav-arrow-down');
  });

  it('should show inactive state', async () => {
    await initConfigureTestingModule(1, 2);
    expect(await harness.hasStepIcon()).toEqual(false);
  });
});
