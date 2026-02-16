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
import { Meta, moduleMetadata, StoryObj, applicationConfig } from '@storybook/angular';
import { of } from 'rxjs';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { WebhookSettingsDialogComponent, WebhookSettingsDialogData } from './webhook-settings-dialog.component';

import { ApiV4, fakeApiV4 } from '../../../../../../entities/management-api-v2';
import { ApiV2Service } from '../../../../../../services-ngx/api-v2.service';
import { SnackBarService } from '../../../../../../services-ngx/snack-bar.service';

const createApiWithSampling = (samplingType: string, samplingValue: string) =>
  fakeApiV4({
    analytics: {
      enabled: true,
      logging: {
        mode: { entrypoint: true, endpoint: true },
        phase: { request: true, response: true },
        content: {
          messagePayload: true,
          messageHeaders: true,
          messageMetadata: false,
          headers: true,
          payload: true,
        },
        condition: undefined,
        messageCondition: undefined,
      },
      sampling: {
        type: samplingType as any,
        value: samplingValue,
      },
    },
  });

const apiCountPerTimeWindow = createApiWithSampling('WINDOWED_COUNT', '60/PT60S');
const apiCount = createApiWithSampling('COUNT', '100');
const apiProbability = createApiWithSampling('PROBABILITY', '0.5');
const apiTemporal = createApiWithSampling('TEMPORAL', 'PT10S');

export default {
  title: 'API / Traffic / Webhook Logs / Settings Dialog',
  component: WebhookSettingsDialogComponent,
  decorators: [
    applicationConfig({
      providers: [provideRouter([]), provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()],
    }),
    moduleMetadata({
      imports: [WebhookSettingsDialogComponent, BrowserAnimationsModule, MatDialogModule],
      providers: [
        {
          provide: MatDialogRef,
          useValue: {
            close: () => {},
          },
        },
        {
          provide: SnackBarService,
          useValue: {
            success: () => {},
            error: () => {},
          },
        },
        {
          provide: ApiV2Service,
          useValue: {
            update: (_: string, updatedApi: ApiV4) => of(updatedApi),
          },
        },
      ],
    }),
  ],
  argTypes: {},
  render: args => ({
    template: `
      <div style="width: 750px; margin: 24px; padding: 24px; background-color: #f4f6fb; min-height: 100vh;">
        <div class="storybook-dialog-wrapper" style="padding: 24px; background: white; border-radius: 4px; box-shadow: 0px 11px 15px -7px rgba(0, 0, 0, 0.2), 0px 24px 38px 3px rgba(0, 0, 0, 0.14), 0px 9px 46px 8px rgba(0, 0, 0, 0.12); display: flex; flex-direction: column;">
          <webhook-settings-dialog></webhook-settings-dialog>
        </div>
      </div>
    `,
    props: args,
  }),
} as Meta;

export const CountPerTimeWindow: StoryObj = {
  decorators: [
    moduleMetadata({
      providers: [
        {
          provide: MAT_DIALOG_DATA,
          useValue: { api: apiCountPerTimeWindow } as WebhookSettingsDialogData,
        },
      ],
    }),
  ],
};

export const Count: StoryObj = {
  decorators: [
    moduleMetadata({
      providers: [
        {
          provide: MAT_DIALOG_DATA,
          useValue: { api: apiCount } as WebhookSettingsDialogData,
        },
      ],
    }),
  ],
};

export const Probability: StoryObj = {
  decorators: [
    moduleMetadata({
      providers: [
        {
          provide: MAT_DIALOG_DATA,
          useValue: { api: apiProbability } as WebhookSettingsDialogData,
        },
      ],
    }),
  ],
};

export const Temporal: StoryObj = {
  decorators: [
    moduleMetadata({
      providers: [
        {
          provide: MAT_DIALOG_DATA,
          useValue: { api: apiTemporal } as WebhookSettingsDialogData,
        },
      ],
    }),
  ],
};
