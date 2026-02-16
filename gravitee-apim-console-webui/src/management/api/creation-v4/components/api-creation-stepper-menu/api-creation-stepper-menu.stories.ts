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
import { Meta, moduleMetadata, StoryObj } from '@storybook/angular';

import { ApiCreationStepperMenuComponent, MenuStepItem } from './api-creation-stepper-menu.component';
import { ApiCreationStepperMenuModule } from './api-creation-stepper-menu.module';
import { TestStepMenuItemComponent } from './test-step-menu-item.component';

import { ApiCreationStep } from '../../services/api-creation-stepper.service';

const FAKE_MENU_STEPS: MenuStepItem[] = [
  {
    menuItemComponent: TestStepMenuItemComponent,
    label: 'Step 1',
    groupNumber: 1,
    state: 'valid',
    payload: { name: 'test' },
  },
  {
    menuItemComponent: TestStepMenuItemComponent,
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
];

const CURRENT_STEP: ApiCreationStep = {
  id: 'step-1',
  group: {
    groupNumber: 1,
    label: 'Step 1',
    menuItemComponent: TestStepMenuItemComponent,
  },
  state: 'valid',
  patchPayload: p => p,
  component: undefined,
};

export default {
  title: 'Shared / API creation stepper',
  component: ApiCreationStepperMenuComponent,
  decorators: [
    moduleMetadata({
      declarations: [TestStepMenuItemComponent],
      imports: [ApiCreationStepperMenuModule],
    }),
  ],
  argTypes: {},
  render: () => ({
    template: `
      <div style="width: 300px">
        <api-creation-stepper-menu [menuSteps]="steps" [currentStep]="currentStep"></api-creation-stepper-menu>
      </div>
    `,
    props: {
      currentStep: CURRENT_STEP,
      steps: FAKE_MENU_STEPS,
    },
  }),
} as Meta;

export const Default: StoryObj = {};
Default.args = {};
