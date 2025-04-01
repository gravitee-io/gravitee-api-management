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
import { HarnessLoader } from '@angular/cdk/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';

import { ApiCreationStepperMenuModule } from './api-creation-stepper-menu.module';
import { StepperMenuStepHarness } from './stepper-menu-step/stepper-menu-step.harness';
import { MenuStepItem } from './api-creation-stepper-menu.component';
import { TestStepMenuItemComponent } from './test-step-menu-item.component';

import { ApiCreationStep } from '../../services/api-creation-stepper.service';

@Component({
  template: `<api-creation-stepper-menu [menuSteps]="menuSteps" [currentStep]="currentStep"></api-creation-stepper-menu>`,
  standalone: false,
})
class TestHostComponent {
  public menuSteps: MenuStepItem[] = [];
  public currentStep: ApiCreationStep = undefined;
}

const FAKE_MENU_STEPS: MenuStepItem[] = [
  {
    label: 'Step 1',
    menuItemComponent: TestStepMenuItemComponent,
    groupNumber: 1,
    state: 'valid',
    payload: { name: 'test' },
  },
  {
    label: 'Step 2',
    groupNumber: 2,
    state: 'initial',
    payload: {},
  },
  {
    label: 'Step 3',
    groupNumber: 3,
    state: 'initial',
    payload: {},
  },
  {
    label: 'Step 4',
    groupNumber: 4,
    state: 'invalid',
    payload: {},
  },
];

const CURRENT_STEP: ApiCreationStep = {
  id: 'step-1',
  group: {
    groupNumber: 1,
    label: 'Step 1',
    menuItemComponent: TestStepMenuItemComponent,
  },
  state: 'valid',
  patchPayload: (p) => p,
  component: undefined,
};

describe('ApiCreationStepperMenuComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let component: TestHostComponent;
  let harnessLoader: HarnessLoader;

  const initConfigureTestingModule = async (steps: MenuStepItem[], currentStep: ApiCreationStep) => {
    await TestBed.configureTestingModule({
      declarations: [TestHostComponent, TestStepMenuItemComponent],
      imports: [ApiCreationStepperMenuModule, MatIconTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    harnessLoader = await TestbedHarnessEnvironment.loader(fixture);
    component = fixture.componentInstance;
    component.menuSteps = steps;
    component.currentStep = currentStep;
    fixture.detectChanges();
  };

  it('should show step number and title', async () => {
    await initConfigureTestingModule(FAKE_MENU_STEPS, CURRENT_STEP);

    const menuSteps = await harnessLoader.getAllHarnesses(StepperMenuStepHarness);

    expect(await menuSteps[0].getStepTitle()).toEqual('Step 1');
    expect(await menuSteps[0].getStepNumber()).toEqual('1');
  });

  it('should show active state', async () => {
    await initConfigureTestingModule(FAKE_MENU_STEPS, {
      ...CURRENT_STEP,
      group: {
        groupNumber: 2,
        label: 'Step 2',
      },
    });
    const menuSteps = await harnessLoader.getAllHarnesses(StepperMenuStepHarness);

    expect(await menuSteps[1].getStepIconName()).toEqual('edit-pencil');
  });

  it('should show filled state', async () => {
    await initConfigureTestingModule(FAKE_MENU_STEPS, {
      ...CURRENT_STEP,
      group: {
        groupNumber: 3,
        label: 'Step 3',
      },
    });
    const menuSteps = await harnessLoader.getAllHarnesses(StepperMenuStepHarness);

    expect(await menuSteps[0].getStepIconName()).toEqual('nav-arrow-down');
  });

  it('should show inactive state', async () => {
    await initConfigureTestingModule(FAKE_MENU_STEPS, CURRENT_STEP);
    const menuSteps = await harnessLoader.getAllHarnesses(StepperMenuStepHarness);

    expect(await menuSteps[2].hasStepIcon()).toEqual(false);
  });

  it('should show invalid state', async () => {
    await initConfigureTestingModule(FAKE_MENU_STEPS, CURRENT_STEP);
    const menuSteps = await harnessLoader.getAllHarnesses(StepperMenuStepHarness);

    expect(await menuSteps[3].hasStepIcon()).toEqual(false);
    expect(await menuSteps[3].isInvalidStep()).toBeTruthy();
  });

  it('should show content', async () => {
    await initConfigureTestingModule(FAKE_MENU_STEPS, CURRENT_STEP);
    const menuSteps = await harnessLoader.getAllHarnesses(StepperMenuStepHarness);

    expect(await menuSteps[0].getStepContent()).toContain('test');
  });
});
