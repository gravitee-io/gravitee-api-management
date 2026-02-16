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
import { Meta, moduleMetadata, componentWrapperDecorator, StoryObj } from '@storybook/angular';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { GioApiLifecycleStateComponent } from './gio-api-lifecycle-state.component';
import { GioApiLifecycleStateModule } from './gio-api-lifecycle-state.module';

export default {
  title: 'Home / Components / API Lifecycle State',
  component: GioApiLifecycleStateComponent,
  decorators: [
    moduleMetadata({
      imports: [GioApiLifecycleStateModule, BrowserAnimationsModule],
    }),
    componentWrapperDecorator(story => `<div style="height:400px;width: 400px">${story}</div>`),
  ],
  render: ({ data }) => ({
    props: { data },
    template: `<gio-api-lifecycle-state [data]="data"></gio-api-lifecycle-state>`,
  }),
} as Meta;

export const Default: StoryObj = {};
Default.args = {
  data: {
    values: {
      CREATED: 83,
      PUBLISHED: 24,
      UNPUBLISHED: 2,
      DEPRECATED: 2,
    },
  },
};
