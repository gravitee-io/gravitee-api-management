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
import { Story, Meta, moduleMetadata } from '@storybook/angular';

import { ConsoleSettingsComponent } from './console-settings';

import { OrganizationSettingsModule } from '../organization-settings.module';

export default {
  title: 'Organization Settings / Console Settings',
  component: ConsoleSettingsComponent,
  decorators: [moduleMetadata({ imports: [OrganizationSettingsModule] })],
} as Meta;

// @ts-ignore
export const Default: Story<ConsoleSettingsComponent> = {
  render: (args) => ({
    props: args,
  }),
  parameters: {
    design: {
      type: 'figma',
      url: 'https://www.figma.com/file/wKU2mCqdmzhtDKlKbh5MPL/Front_End_lib?node-id=242%3A7153',
    },
  },
};
