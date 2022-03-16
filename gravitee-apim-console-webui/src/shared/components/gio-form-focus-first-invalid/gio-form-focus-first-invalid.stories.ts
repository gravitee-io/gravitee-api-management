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
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { GioFormTagsInputModule, GioSaveBarModule } from '@gravitee/ui-particles-angular';

import { GioFormFocusInvalidDirective } from './gio-form-focus-first-invalid.directive';
import { GioFormFocusInvalidModule } from './gio-form-focus-first-invalid.module';

import { GioFormColorInputModule } from '../gio-form-color-input/gio-form-color-input.module';

export default {
  title: 'Shared / Form focus invalid',
  component: GioFormFocusInvalidDirective,
  decorators: [
    moduleMetadata({
      imports: [
        GioFormFocusInvalidModule,
        GioSaveBarModule,
        BrowserAnimationsModule,
        ReactiveFormsModule,
        MatFormFieldModule,
        MatInputModule,
        MatSelectModule,
        GioFormTagsInputModule,
        GioFormColorInputModule,
      ],
    }),
  ],
} as Meta;

export const Default: Story = {
  render: () => {
    const form = new FormGroup({
      anInput: new FormControl(null, Validators.required),
      aSelect: new FormControl(null, Validators.required),
      aTextarea: new FormControl(null, Validators.required),
      aTagsInput: new FormControl(null, Validators.required),
      aColorInput: new FormControl(null, Validators.required),
    });

    return {
      template: `
    <form style="padding-bottom: 400px" [formGroup]="form" gioFormFocusInvalid>
      <p>A long form, with many many fields</p>
      <p>When clicking on save the 1st input in error is focused</p>
      <div style="display: flex; flex-direction: column;">

        <mat-form-field appearance="fill">
          <mat-label>Input</mat-label>
          <input matInput required formControlName="anInput">
        </mat-form-field>

        <br *ngFor="let item of [].constructor(30)">

        <mat-form-field appearance="fill">
          <mat-label>Select</mat-label>
          <mat-select required formControlName="aSelect">
            <mat-option value="one">First option</mat-option>
            <mat-option value="two">Second option</mat-option>
          </mat-select>
        </mat-form-field>

        <br *ngFor="let item of [].constructor(30)">

        <mat-form-field appearance="fill">
          <mat-label>Textarea</mat-label>
          <textarea matInput required formControlName="aTextarea"></textarea>
        </mat-form-field>

        <br *ngFor="let item of [].constructor(30)">

        <mat-form-field appearance="fill">
          <mat-label>My tags</mat-label>
          <gio-form-tags-input required formControlName="aTagsInput">
          </gio-form-tags-input>
        </mat-form-field>

        <br *ngFor="let item of [].constructor(30)">

        <mat-form-field appearance="fill">
          <mat-label>Select color</mat-label>
          <gio-form-color-input required formControlName="aColorInput">
          </gio-form-color-input>
        </mat-form-field>

        <br *ngFor="let item of [].constructor(30)">

      </div>
      <gio-save-bar
        [creationMode]="true"
        [form]="form">
      </gio-save-bar>
    </form>
    `,
      props: {
        form,
      },
    };
  },
};
