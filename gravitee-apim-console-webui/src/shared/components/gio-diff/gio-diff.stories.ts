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
import { moduleMetadata } from '@storybook/angular';
import { Story, Meta } from '@storybook/angular/dist/ts3.9/client/preview/types-7-0';

import { GioDiffComponent } from './gio-diff.component';
import { GioDiffModule } from './gio-diff.module';

export default {
  title: 'Shared / Diff',
  component: GioDiffComponent,
  decorators: [
    moduleMetadata({
      imports: [GioDiffModule],
    }),
  ],
  argTypes: {},
  render: () => ({
    props: {},
  }),
} as Meta;

export const Default: Story = {};
