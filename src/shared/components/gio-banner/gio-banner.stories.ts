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
import { Meta, moduleMetadata, Story } from '@storybook/angular';
import { MatCardModule } from '@angular/material/card';

import { GioBannerModule } from './gio-banner.module';
import { GioBannerComponent } from './gio-banner.component';

export default {
  title: 'Shared / Banner',
  component: GioBannerComponent,
  decorators: [
    moduleMetadata({
      imports: [GioBannerModule, MatCardModule],
    }),
  ],
  render: () => ({}),
} as Meta;

export const All: Story = {
  render: () => ({
    template: `
    <gio-banner-error>Error <br> Second line <br> Wow another one</gio-banner-error>
    <br>
    <gio-banner-info>This is an info banner!</gio-banner-info>
    <br>
    <gio-banner-success>This is a success banner!</gio-banner-success>
    <br>
    <gio-banner-warning>This is a warning banner!</gio-banner-warning>
    `,
  }),
};

export const AllInMatCard: Story = {
  render: () => ({
    template: `
    <mat-card>
      <gio-banner-error>Error <br> Second line <br> Wow another one</gio-banner-error>
      <br>
      <gio-banner-info>This is an info banner!</gio-banner-info>
      <br>
      <gio-banner-success>This is a success banner!</gio-banner-success>
      <br>
      <gio-banner-warning>This is a warning banner!</gio-banner-warning>
    </mat-card>`,
  }),
};

export const AllWithTypeInput: Story = {
  render: () => ({
    template: `
    <gio-banner type="error">Error <br> Second line <br> Wow another one</gio-banner>
    <br>
    <gio-banner type="info">This is an info banner!</gio-banner>
    <br>
    <gio-banner type="success">This is a success banner!</gio-banner>
    <br>
    <gio-banner type="warning">This is a warning banner!</gio-banner>
    `,
  }),
};

export const Default: Story = {
  render: () => ({
    template: `<gio-banner>This is an Default banner!</gio-banner>`,
  }),
};
