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
import { APP_INITIALIZER } from '@angular/core';
import { Meta, moduleMetadata, StoryObj } from '@storybook/angular';

import { GioTableOfContentsComponent } from './gio-table-of-contents.component';
import { GioTableOfContentsModule } from './gio-table-of-contents.module';
import { GioTableOfContentsService } from './gio-table-of-contents.service';

export default {
  title: 'Shared / Table of contents',
  component: GioTableOfContentsComponent,
  decorators: [
    moduleMetadata({
      imports: [GioTableOfContentsModule],
    }),
  ],
} as Meta;

export const Default: StoryObj = {
  render: () => ({
    template: `
    <div style="display: flex;justify-content: space-between; align-items: flex-start;">
      <div style="width: 60%;">
        <h1 >Link in h1</h1>
        <br *ngFor="let item of [].constructor(10)">

        <h2 gioTableOfContents>Link 1.1</h2>
        <br *ngFor="let item of [].constructor(30)">

        <h3 gioTableOfContents>Link 1.1.1</h3>
        <br *ngFor="let item of [].constructor(30)">

        <h3 gioTableOfContents>Link 1.1.2</h3>
        <br *ngFor="let item of [].constructor(30)">

        <h4 gioTableOfContents>Link 1.1.2.a</h4>
        <br *ngFor="let item of [].constructor(30)">

        <h4 gioTableOfContents>Link 1.1.2.b</h4>
        <br *ngFor="let item of [].constructor(30)">

        <h2 gioTableOfContents>Link 1.2</h2>
        <br *ngFor="let item of [].constructor(30)">

        <h3 gioTableOfContents>Link 1.2.1</h3>
        <br *ngFor="let item of [].constructor(30)">

        <h3 gioTableOfContents>Link 1.2.1</h3>
      </div>

      <gio-table-of-contents></gio-table-of-contents>
    </div>
    `,
  }),
};

export const Multiple: StoryObj = {
  render: () => ({
    template: `
    <div style="display: flex;justify-content: space-between; align-items: flex-start;">
      <div style="width: 60%;">
        <h2 gioTableOfContents>Link 1.1</h2>
        <br *ngFor="let item of [].constructor(30)">

        <h3 gioTableOfContents>Link 1.1.1</h3>
        <br *ngFor="let item of [].constructor(30)">

        <h3 gioTableOfContents>Link 1.1.2</h3>
        <br *ngFor="let item of [].constructor(30)">

        <h2 gioTableOfContents gioTableOfContentsSectionId="second">Link 1.2</h2>
        <br *ngFor="let item of [].constructor(30)">

        <h3 gioTableOfContents gioTableOfContentsSectionId="second">Link 1.2.1</h3>
        <br *ngFor="let item of [].constructor(30)">

        <h3 gioTableOfContents gioTableOfContentsSectionId="second">Link 1.2.1</h3>
      </div>

      <gio-table-of-contents></gio-table-of-contents>
    </div>
    `,
    moduleMetadata: {
      providers: [
        {
          provide: APP_INITIALIZER,
          useFactory: toc => () => {
            toc.addSection('', 'First section');
            toc.addSection('second', 'Second section');
          },
          deps: [GioTableOfContentsService],
          multi: true,
        },
      ],
    },
  }),
};

export const AddAndRemoveDynamically: StoryObj = {
  render: () => ({
    template: `
    <div style="display: flex;justify-content: space-between; align-items: flex-start;">
      <div style="width: 60%;">
        <ng-container *ngIf="moreLinks">
          <h2 gioTableOfContents>Link 1.0</h2>
          <br *ngFor="let item of [].constructor(30)">
        </ng-container>

        <h2 gioTableOfContents>Link 1.1</h2>
        <br *ngFor="let item of [].constructor(30)">

        <h3 gioTableOfContents>Link 1.1.1</h3>
        <br *ngFor="let item of [].constructor(30)">

        <h3 gioTableOfContents>Link 1.1.2</h3>
        <br *ngFor="let item of [].constructor(30)">

        <ng-container *ngIf="moreLinks">
          <h3 gioTableOfContents>Link 1.1.3</h3>
          <br *ngFor="let item of [].constructor(30)">
        </ng-container>

        <button *ngIf="!moreLinks" (click)="moreLinks = true">+ Add</button>
        <ng-container *ngIf="moreLinks">
          <h2 gioTableOfContents>Link 1.2</h2>
          <br *ngFor="let item of [].constructor(30)">

          <h3 gioTableOfContents>Link 1.2.1</h3>
          <br *ngFor="let item of [].constructor(30)">

          <h3 gioTableOfContents>Link 1.2.1</h3>
          <br *ngFor="let item of [].constructor(30)">

          <h2 gioTableOfContents gioTableOfContentsSectionId="second">Link in new section</h2>
          <br *ngFor="let item of [].constructor(30)">

          <button (click)="moreLinks = false">- Remove</button>
        </ng-container>
      </div>

      <gio-table-of-contents></gio-table-of-contents>
    </div>
    `,
    moduleMetadata: {
      providers: [
        {
          provide: APP_INITIALIZER,
          useFactory: toc => () => {
            toc.addSection('', 'First section');
          },
          deps: [GioTableOfContentsService],
          multi: true,
        },
      ],
    },
  }),
};

export const ScrollingContainer: StoryObj = {
  render: () => ({
    template: `
    <div style="height:24px"> The TopBar 🍻 <button *ngIf="moreLinks" (click)="moreLinks = false">- Remove</button> <button *ngIf="!moreLinks" (click)="moreLinks = true">+ Add</button></div>
    <div
         id="scrollingContainer"
         style="display: flex; justify-content: space-between; align-items: flex-start; overflow: auto; height: calc(100vh - 24px);"
    >
      <div style="width: 60%;">
        <h2 gioTableOfContents>Link 1.1</h2>
        <br *ngFor="let item of [].constructor(30)">

        <h3 gioTableOfContents>Link 1.1.1</h3>
        <br *ngFor="let item of [].constructor(30)">

        <h3 gioTableOfContents>Link 1.1.2</h3>
        <br *ngFor="let item of [].constructor(30)">


        <ng-container *ngIf="moreLinks">
          <h2 gioTableOfContents>Link 1.2</h2>
          <br *ngFor="let item of [].constructor(30)">

          <h3 gioTableOfContents>Link 1.2.1</h3>
          <br *ngFor="let item of [].constructor(30)">

          <h3 gioTableOfContents>Link 1.2.1</h3>
          <br *ngFor="let item of [].constructor(30)">

          <h2 gioTableOfContents gioTableOfContentsSectionId="second">Link in new section</h2>
          <br *ngFor="let item of [].constructor(30)">

        </ng-container>
      </div>

      <gio-table-of-contents scrollingContainer="#scrollingContainer"></gio-table-of-contents>
    </div>
    `,
    moduleMetadata: {
      providers: [
        {
          provide: APP_INITIALIZER,
          useFactory: toc => () => {
            toc.addSection('', 'First section');
          },
          deps: [GioTableOfContentsService],
          multi: true,
        },
      ],
    },
  }),
};
