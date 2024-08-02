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
import { Meta, moduleMetadata, StoryObj } from '@storybook/angular';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { MatCardModule } from '@angular/material/card';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatListModule } from '@angular/material/list';
import { MatButtonModule } from '@angular/material/button';
import { GioIconsModule } from '@gravitee/ui-particles-angular';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { action } from '@storybook/addon-actions';
import { MatRadioModule } from '@angular/material/radio';

import { GioSelectionListModule } from './gio-selection-list.module';
import { GioSelectionListOptionLayoutComponent } from './gio-selection-list-option-layout.component';

export default {
  title: 'Shared / Selection List Option Layout',
  component: GioSelectionListOptionLayoutComponent,
  decorators: [
    moduleMetadata({
      imports: [
        CommonModule,
        BrowserAnimationsModule,
        ReactiveFormsModule,
        MatCardModule,
        MatIconModule,
        MatListModule,
        GioIconsModule,
        GioSelectionListModule,
        MatButtonModule,
        MatRadioModule,
      ],
    }),
  ],
  render: () => ({}),
} as Meta;

export const SelectionList: StoryObj = {
  render: () => {
    const formGroup = new FormGroup({
      checkboxControl: new FormControl('', Validators.required),
    });
    const optionList = [
      {
        id: 1,
        name: 'Option 1',
        description: 'Option 1 description',
      },
      {
        id: 2,
        name: 'Option 2',
        description: 'Option 2 description',
      },
      {
        id: 3,
        name: 'Option 3',
        description: 'Option 3 description',
      },
    ];
    return {
      template: `
    <p>This component is to be used within the <b>mat-list-option</b> of <b>mat-selection-list</b>.</p>
    <p>The <b>mat-selection-list</b> element must contain the class <i>gio-selection-list</i> in order to have the correct overrides for the component.</p>
    <form [formGroup]="formGroup" (ngSubmit)="onSubmit(formGroup.value.checkboxControl)" style="width: 700px">
      <mat-selection-list class="gio-selection-list" formControlName="checkboxControl">
        <mat-list-option
            *ngFor="let option of optionList"
            [value]="option.id"
            togglePosition="before"
            [disableRipple]="true"
            #listOption
            [class.gio-selection__selected]="listOption.selected"
            >
            <gio-selection-list-option-layout>
                <gio-selection-list-option-layout-title>
                    {{ option.name }}
                </gio-selection-list-option-layout-title>
                <gio-selection-list-option-layout-body>
                    {{ option.description }}
                </gio-selection-list-option-layout-body>
                <gio-selection-list-option-layout-action>
                    <button type="button" mat-raised-button (click)="onActionButtonClicked($event, option.id)">
                      <mat-icon svgIcon="gio:eye-empty"></mat-icon>
                      Action Button
                    </button>
                </gio-selection-list-option-layout-action>
            </gio-selection-list-option-layout>
        </mat-list-option>
      </mat-selection-list >
      <button type="submit" [disabled]="formGroup.invalid" mat-raised-button style="margin: 16px 0;">More than one checkbox selected</button>
    </form>
    `,
      props: {
        formGroup: formGroup,
        optionList: optionList,
        onActionButtonClicked: ($event, val) => {
          $event.stopPropagation();
          action('Action button clicked')(val);
        },
        onSubmit: (val) => action('Submit values')(val),
      },
      styleUrls: ['./gio-selection-list-option-layout.component.scss'],
    };
  },
};
export const RadioGroup: StoryObj = {
  render: () => {
    const formGroup = new FormGroup({
      radioControl: new FormControl('', Validators.required),
    });
    const optionList = [
      {
        id: 1,
        name: 'Option 1',
        description: 'Option 1 description',
      },
      {
        id: 2,
        name: 'Option 2',
        description: 'Option 2 description',
      },
      {
        id: 3,
        name: 'Option 3',
        description: 'Option 3 description',
      },
    ];
    return {
      template: `
    <p>This component is to be used within the <b>mat-radio-button</b> of <b>mat-radio-group</b>.</p>
    <p>The <b>mat-radio-group</b> element must contain the class <i>gio-selection-list</i> in order to have the correct overrides for the component.</p>
    <form [formGroup]="formGroup" (ngSubmit)="onSubmit(formGroup.value.radioControl)" style="width: 700px">
      <mat-radio-group class="gio-selection-list" formControlName="radioControl">
        <mat-radio-button
            *ngFor="let option of optionList"
            [value]="option.id"
            labelPosition="after"
            #listOption
            [class.gio-selection__selected]="listOption.checked"
            >
            <gio-selection-list-option-layout>
                <gio-selection-list-option-layout-title>
                    {{ option.name }}
                </gio-selection-list-option-layout-title>
                <gio-selection-list-option-layout-body>
                    {{ option.description }}
                </gio-selection-list-option-layout-body>
                <gio-selection-list-option-layout-action>
                    <button type="button" mat-raised-button (click)="onActionButtonClicked($event, option.id)">
                      <mat-icon svgIcon="gio:eye-empty"></mat-icon>
                      Action Button
                    </button>
                </gio-selection-list-option-layout-action>
            </gio-selection-list-option-layout>
        </mat-radio-button>
      </mat-radio-group >
      <button type="submit" [disabled]="formGroup.invalid" mat-raised-button style="margin: 16px 0;">One option selected</button>
    </form>
    `,
      props: {
        formGroup: formGroup,
        optionList: optionList,
        onActionButtonClicked: ($event, val) => {
          $event.stopPropagation();
          action('Action button clicked')(val);
        },
        onSubmit: (val) => action('Submit values')(val),
      },
      styleUrls: ['./gio-selection-list-option-layout.component.scss'],
    };
  },
};
