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
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { ReactiveFormsModule, FormBuilder } from '@angular/forms';
import { OverlayModule } from '@angular/cdk/overlay';

import { GioSelectSearchComponent, SelectOption } from './gio-select-search.component';

export default {
  title: 'Shared / Gio Select Search Component',
  component: GioSelectSearchComponent,
  decorators: [
    moduleMetadata({
      imports: [MatCardModule, MatButtonModule, ReactiveFormsModule, OverlayModule, GioSelectSearchComponent],
    }),
  ],
} as Meta;

// HTTP Status options for stories
const httpStatusOptions: SelectOption[] = [
  { value: '200', label: '200 - OK' },
  { value: '201', label: '201 - Created' },
  { value: '202', label: '202 - Accepted' },
  { value: '204', label: '204 - No Content' },
  { value: '300', label: '300 - Multiple Choices' },
  { value: '301', label: '301 - Moved Permanently' },
  { value: '302', label: '302 - Found' },
  { value: '304', label: '304 - Not Modified' },
  { value: '400', label: '400 - Bad Request' },
  { value: '401', label: '401 - Unauthorized' },
  { value: '403', label: '403 - Forbidden' },
  { value: '404', label: '404 - Not Found' },
  { value: '405', label: '405 - Method Not Allowed' },
  { value: '409', label: '409 - Conflict' },
  { value: '422', label: '422 - Unprocessable Entity' },
  { value: '429', label: '429 - Too Many Requests' },
  { value: '500', label: '500 - Internal Server Error' },
  { value: '501', label: '501 - Not Implemented' },
  { value: '502', label: '502 - Bad Gateway' },
  { value: '503', label: '503 - Service Unavailable' },
  { value: '504', label: '504 - Gateway Timeout' },
];

// HTTP Method options for stories
const methodOptions: SelectOption[] = [
  { value: 'GET', label: 'GET' },
  { value: 'POST', label: 'POST' },
  { value: 'PUT', label: 'PUT' },
  { value: 'DELETE', label: 'DELETE' },
  { value: 'PATCH', label: 'PATCH' },
  { value: 'HEAD', label: 'HEAD' },
  { value: 'OPTIONS', label: 'OPTIONS' },
  { value: 'TRACE', label: 'TRACE' },
];

// Fruit options for stories
const fruitOptions: SelectOption[] = [
  { value: 'apple', label: 'Apple' },
  { value: 'banana', label: 'Banana' },
  { value: 'cherry', label: 'Cherry' },
  { value: 'date', label: 'Date' },
  { value: 'elderberry', label: 'Elderberry' },
  { value: 'fig', label: 'Fig' },
  { value: 'grape', label: 'Grape' },
  { value: 'honeydew', label: 'Honeydew' },
];

export const Basic: StoryObj = {
  render: () => ({
    template: `
      <mat-card style="width: 500px; padding: 20px;">
        <h3>Basic Multi-Select with Overlay</h3>
        <p style="color: #666; margin-bottom: 15px;">
          Click the trigger to open the overlay with checkboxes. Select multiple options and see the count update.
        </p>
        <gio-select-search
          [options]="options"
          label="HTTP Status Codes"
          placeholder="Search status codes..."
        />
      </mat-card>
    `,
    props: {
      options: httpStatusOptions,
    },
  }),
};

export const WithFormIntegration: StoryObj = {
  render: () => {
    const fb = new FormBuilder();
    const form = fb.group({
      httpStatuses: [['200', '404']],
      methods: [['GET', 'POST']],
    });

    return {
      template: `
        <mat-card style="width: 500px; padding: 20px;">
          <h3>Form Integration with Overlay</h3>
          <p style="color: #666; margin-bottom: 15px;">
            The component integrates with Angular reactive forms. Selected values are displayed below each component.
          </p>
          <form [formGroup]="form">
            <div style="margin-bottom: 20px;">
              <label>HTTP Status Codes:</label>
              <gio-select-search
                [options]="httpStatusOptions"
                formControlName="httpStatuses"
                label="HTTP Status"
                placeholder="Search status codes..."
              />
              <div style="margin-top: 10px; font-size: 12px; color: #666;">
                Selected: {{ form.get('httpStatuses')?.value?.join(', ') || 'None' }}
              </div>
            </div>

            <div style="margin-bottom: 20px;">
              <label>HTTP Methods:</label>
              <gio-select-search
                [options]="methodOptions"
                formControlName="methods"
                label="HTTP Methods"
                placeholder="Search methods..."
              />
              <div style="margin-top: 10px; font-size: 12px; color: #666;">
                Selected: {{ form.get('methods')?.value?.join(', ') || 'None' }}
              </div>
            </div>

            <div style="margin-top: 20px;">
              <button mat-button type="button" (click)="resetForm()">Reset</button>
              <button mat-button type="button" (click)="setDefaultValues()">Set Defaults</button>
            </div>
          </form>
        </mat-card>
      `,
      props: {
        form,
        httpStatusOptions,
        methodOptions,
        resetForm: () => form.reset(),
        setDefaultValues: () =>
          form.patchValue({
            httpStatuses: ['200', '500'],
            methods: ['GET', 'PUT', 'DELETE'],
          }),
      },
    };
  },
};

export const WithSearch: StoryObj = {
  render: () => ({
    template: `
      <mat-card style="width: 500px; padding: 20px;">
        <h3>Search Functionality</h3>
        <p style="color: #666; margin-bottom: 15px;">
          Type in the search field to filter options. Try searching for "a" to see filtering in action.
          The search is debounced and updates the options list in real-time.
        </p>
        <gio-select-search
          [options]="options"
          label="Fruits"
          placeholder="Search fruits..."
        />
      </mat-card>
    `,
    props: {
      options: fruitOptions,
    },
  }),
};

export const Disabled: StoryObj = {
  render: () => {
    const fb = new FormBuilder();
    const form = fb.group({
      httpStatuses: [{ value: ['200', '404'], disabled: true }],
    });

    return {
      template: `
        <mat-card style="width: 500px; padding: 20px;">
          <h3>Disabled State</h3>
          <p style="color: #666; margin-bottom: 15px;">
            The component is disabled through form control, preventing interaction with the overlay.
            The form control is disabled, so the component cannot be interacted with.
          </p>
          <form [formGroup]="form">
            <div style="margin-bottom: 20px;">
              <label>HTTP Status Codes (Disabled):</label>
              <gio-select-search
                [options]="options"
                formControlName="httpStatuses"
                label="Disabled HTTP Status"
                placeholder="Search status codes..."
              />
              <div style="margin-top: 10px; font-size: 12px; color: #666;">
                Selected: {{ form.get('httpStatuses')?.value?.join(', ') || 'None' }}
              </div>
            </div>

            <div style="margin-top: 20px;">
              <button mat-button type="button" (click)="enableControl()">Enable Control</button>
              <button mat-button type="button" (click)="disableControl()">Disable Control</button>
            </div>
          </form>
        </mat-card>
      `,
      props: {
        form,
        options: httpStatusOptions,
        enableControl: () => form.get('httpStatuses')?.enable(),
        disableControl: () => form.get('httpStatuses')?.disable(),
      },
    };
  },
};

export const EmptyOptions: StoryObj = {
  render: () => ({
    template: `
      <mat-card style="width: 500px; padding: 20px;">
        <h3>Empty Options</h3>
        <p style="color: #666; margin-bottom: 15px;">
          When no options are provided, the overlay shows a "No options available" message.
        </p>
        <gio-select-search
          [options]="[]"
          label="No Options"
          placeholder="No options available..."
        />
      </mat-card>
    `,
    props: {
      options: [],
    },
  }),
};

export const LargeDataset: StoryObj = {
  render: () => {
    // Generate a large dataset for testing performance
    const largeOptions: SelectOption[] = Array.from({ length: 100 }, (_, i) => ({
      value: `option-${i}`,
      label: `Option ${i + 1} - This is a longer label to test text wrapping and search functionality`,
    }));

    return {
      template: `
        <mat-card style="width: 500px; padding: 20px;">
          <h3>Large Dataset (100 options)</h3>
          <p style="color: #666; margin-bottom: 15px;">
            Test performance with a large number of options. Try searching for "Option" to see filtering.
            The overlay handles large datasets efficiently with virtual scrolling.
          </p>
          <gio-select-search
            [options]="options"
            label="Large Dataset"
            placeholder="Search options..."
          />
        </mat-card>
      `,
      props: {
        options: largeOptions,
      },
    };
  },
};

export const WithDisabledOptions: StoryObj = {
  render: () => {
    const optionsWithDisabled: SelectOption[] = [
      { value: 'apple', label: 'Apple' },
      { value: 'banana', label: 'Banana', disabled: true },
      { value: 'cherry', label: 'Cherry' },
      { value: 'date', label: 'Date', disabled: true },
      { value: 'elderberry', label: 'Elderberry' },
    ];

    return {
      template: `
        <mat-card style="width: 500px; padding: 20px;">
          <h3>With Disabled Options</h3>
          <p style="color: #666; margin-bottom: 15px;">
            Individual options can be disabled and cannot be selected. Disabled options are visually distinct.
          </p>
          <gio-select-search
            [options]="options"
            label="Fruits"
            placeholder="Search fruits..."
          />
        </mat-card>
      `,
      props: {
        options: optionsWithDisabled,
      },
    };
  },
};

export const KeyboardAccessibility: StoryObj = {
  render: () => ({
    template: `
      <mat-card style="width: 500px; padding: 20px;">
        <h3>Keyboard Accessibility</h3>
        <p style="color: #666; margin-bottom: 15px;">
          The component supports keyboard navigation:
          <br>• Tab to focus the trigger
          <br>• Enter/Space to open/close the overlay
          <br>• Tab to navigate through options
          <br>• Enter/Space to select/deselect options
        </p>
        <gio-select-search
          [options]="options"
          label="Accessible Component"
          placeholder="Try keyboard navigation..."
        />
      </mat-card>
    `,
    props: {
      options: httpStatusOptions,
    },
  }),
};
