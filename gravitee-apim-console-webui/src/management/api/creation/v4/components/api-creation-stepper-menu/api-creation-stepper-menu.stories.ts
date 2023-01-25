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
import { Meta, moduleMetadata } from '@storybook/angular';
import { Story } from '@storybook/angular/dist/ts3.9/client/preview/types-7-0';

import { ApiCreationStepperMenuComponent, MenuStepItem } from './api-creation-stepper-menu.component';
import { ApiCreationStepperMenuModule } from './api-creation-stepper-menu.module';
import { TestStepMenuItemComponent } from './test-step-menu-item.component';

const FAKE_STEPS: MenuStepItem[] = [
  {
    id: 'step-1',
    component: undefined,
    menuItemComponent: TestStepMenuItemComponent,
    label: 'Step 1',
    labelNumber: 1,
    state: 'valid',
    patchPayload: () => ({}),
    payload: { name: 'test' },
  },
  {
    id: 'step-2',
    component: undefined,
    menuItemComponent: TestStepMenuItemComponent,
    label: 'Step 2',
    labelNumber: 2,
    state: 'initial',
    patchPayload: () => ({}),
    payload: {},
  },
  {
    id: 'step-3',
    component: undefined,
    label: 'Step 3',
    labelNumber: 3,
    state: 'initial',
    patchPayload: () => ({}),
    payload: {},
  },
];

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
      currentStep: FAKE_STEPS[1],
      steps: FAKE_STEPS,
    },
  }),
} as Meta;

export const Default: Story = {};
Default.args = {};
