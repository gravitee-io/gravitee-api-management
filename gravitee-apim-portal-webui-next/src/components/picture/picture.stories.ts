/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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

import { PictureComponent } from './picture.component';

export default {
  title: 'Picture',
  decorators: [
    moduleMetadata({
      imports: [PictureComponent],
    }),
  ],
  render: () => ({}),
} as Meta;

export const ApiPicture: StoryObj = {
  render: () => ({
    template: `
        <div>
          <h3>Generated API Picture</h3>
          <app-picture hashValue="pic-does-not-exist 1.2" picture="pic-does-not-exist" size="100"></app-picture>
          <br />
          <br />
          <h3>Found API Picture</h3>
          <app-picture hashValue="name 1.2" picture="images/logo.png" size="100"></app-picture>
        </div>
        `,
    styles: [
      `
            .button-container {
                display: flex;
                flex-direction: row;
                justify-content: space-around;
                width: 500px;
                margin-bottom: 16px;
            }
       `,
    ],
  }),
};
