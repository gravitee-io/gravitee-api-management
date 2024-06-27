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
import { Meta, StoryObj } from '@storybook/angular';

import { DeveloperPortalCatalogComponent } from './developer-portal-catalog.component';

export default {
  title: 'DeveloperPortalCatalogComponent story',
  component: DeveloperPortalCatalogComponent,
  argTypes: {},
  render: (args) => ({
    template: `
      <div style="width: 800px">
        <developer-portal-catalog  [dataSource]="dataSource" [loading]="loading"></developer-portal-catalog>
      </div>
    `,
    props: args,
  }),
} as Meta;

export const Empty: StoryObj = {};
Empty.args = {
  loading: false,
  dataSource: [],
};

export const Loading: StoryObj = {};
Loading.args = {
  loading: true,
  dataSource: [],
};

export const WithData: StoryObj = {};
WithData.args = {
  loading: false,
  dataSource: [{ name: 'foo' }, { name: 'bar' }, { name: 'baz' }],
};
