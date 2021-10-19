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
import { Meta } from '@storybook/angular';
import { Story } from '@storybook/angular/dist/ts3.9/client/preview/types-7-0';

export default {
  title: 'Shared / Description list (dl,dt,dd)',
  decorators: [],
} as Meta;

export const Default: Story = {
  render: () => {
    return {
      template: `
          <div>
          <dl class="gio-description-list">
            <dt>Email</dt>
            <dd>gaetan.maisse+user@graviteesource.com</dd>

            <dt>Source</dt>
            <dd>gravitee</dd>

            <dt>Organization Role</dt>
            <dd>ADMIN, USER</dd>

            <dt>Last connection at</dt>
            <dd>Sep 28, 2021 5:34:40 PM</dd>

            <dt>Created at</dt>
            <dd>Sep 28, 2021 5:27:00 PM</dd>
          </dl>
        </div>
      `,
      props: {},
    };
  },
};
