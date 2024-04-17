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
import { importProvidersFrom } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { action } from '@storybook/addon-actions';
import { applicationConfig, Meta, moduleMetadata, StoryObj } from '@storybook/angular';

import { PageTreeComponent, PageTreeNode } from './page-tree.component';

const PAGE_DATA: PageTreeNode[] = [
  {
    id: 'fruit',
    name: 'Fruit',
    children: [
      { id: 'Apple', name: 'Apple' },
      { id: 'Banana', name: 'Banana' },
      { id: 'Fruit loops', name: 'Fruit loops' },
    ],
  },
  {
    id: 'Vegetables',
    name: 'Vegetables',
    children: [
      {
        id: 'Green',
        name: 'Green',
        children: [
          { id: 'Broccoli', name: 'Broccoli' },
          { id: 'Brussels sprouts', name: 'Brussels sprouts' },
        ],
      },
      {
        id: 'Orange',
        name: 'Orange',
        children: [
          { id: 'Pumpkins', name: 'Pumpkins' },
          { id: 'Carrots', name: 'Carrots' },
        ],
      },
    ],
  },
];

export default {
  title: 'Components / Page Tree',
  decorators: [
    moduleMetadata({
      imports: [PageTreeComponent],
    }),
    applicationConfig({
      providers: [importProvidersFrom(MatIconModule)],
    }),
  ],
  render: () => ({}),
} as Meta;

export const PageTree: StoryObj = {
  render: () => ({
    template: `
        <div style="width: 300px">
          <app-page-tree [pages]="pages" (openFile)="onOpenFile($event)"/>
        </div>
        `,
    styles: [``],
    props: {
      pages: PAGE_DATA,
      onOpenFile: (id: string) => action('File selected')(id),
    },
  }),
};

const PAGE_DATA_LONG_NAMES: PageTreeNode[] = [
  {
    id: 'fruit',
    name: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. ',
    children: [
      { id: 'Apple', name: 'Apple' },
      {
        id: 'Banana',
        name: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. ',
      },
      { id: 'Fruit loops', name: 'Fruit loops' },
    ],
  },
  {
    id: 'Vegetables',
    name: 'Vegetables',
    children: [
      {
        id: 'Green',
        name: 'Green',
        children: [
          {
            id: 'Broccoli',
            name: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. ',
          },
          { id: 'Brussels sprouts', name: 'Brussels sprouts' },
        ],
      },
      {
        id: 'Orange',
        name: 'Orange',
        children: [
          {
            id: 'Pumpkins',
            name: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. ',
          },
          { id: 'Carrots', name: 'Carrots' },
        ],
      },
    ],
  },
];

export const PageTreeWithLongNames: StoryObj = {
  render: () => ({
    template: `
        <div style="width: 300px">
          <app-page-tree [pages]="pages" (openFile)="onOpenFile($event)"/>
        </div>
        `,
    styles: [``],
    props: {
      pages: PAGE_DATA_LONG_NAMES,
      onOpenFile: (id: string) => action('File selected')(id),
    },
  }),
};
