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
import { MatCardModule } from '@angular/material/card';
import { GioIconsModule } from '@gravitee/ui-particles-angular';

import { ApiCreationStepperComponent } from './api-creation-stepper.component';
import { ApiCreationStepComponent } from './api-creation-step/api-creation-step.component';

export default {
  title: 'Shared / API creation stepper',
  component: ApiCreationStepperComponent,
  decorators: [
    moduleMetadata({
      declarations: [ApiCreationStepComponent],
      imports: [MatCardModule, GioIconsModule],
    }),
  ],
  argTypes: {},
  render: () => ({
    template: `
      <div style="width: 300px">
        <api-creation-stepper [currentStep]="3"></api-creation-stepper>
      </div>
    `,
    props: {},
  }),
} as Meta;

export const Default: Story = {};
Default.args = {};
