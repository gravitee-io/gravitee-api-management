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
import { Component, Input } from '@angular/core';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { Meta, moduleMetadata, StoryObj } from '@storybook/angular';
import { action } from '@storybook/addon-actions';
import { tap } from 'rxjs/operators';
import { MatButtonModule } from '@angular/material/button';

import { GioEeUnlockDialogComponent, GioEeUnlockDialogData } from './gio-ee-unlock-dialog.component';
import { GioEeUnlockDialogModule } from './gio-ee-unlock-dialog.module';

import { Feature, FeatureInfo, FeatureInfoData } from '../../shared/components/gio-license/gio-license-features';
import { UTMMedium } from '../../shared/components/gio-license/gio-license-utm';

@Component({
  selector: 'gio-ee-unlock-dialog-story',
  template: `<button id="open-dialog" (click)="openDialog()">More information</button>`,
})
class GioEeUnlockDialogStoryComponent {
  @Input() public featureInfo: FeatureInfo;
  constructor(private readonly matDialog: MatDialog) {}

  public openDialog() {
    this.matDialog
      .open<GioEeUnlockDialogComponent, GioEeUnlockDialogData, boolean>(GioEeUnlockDialogComponent, {
        data: {
          featureInfo: this.featureInfo,
          trialURL: 'https://gravitee.io/self-hosted-trial',
        },
        role: 'alertdialog',
        id: 'dialog',
      })
      .afterClosed()
      .pipe(
        tap((confirmed) => {
          action('confirmed?')(confirmed);
        }),
      )
      .subscribe();
  }
}

export default {
  title: 'Shared / EE unlock dialog',
  component: GioEeUnlockDialogComponent,
  decorators: [
    moduleMetadata({
      declarations: [GioEeUnlockDialogStoryComponent],
      imports: [GioEeUnlockDialogModule, MatButtonModule, MatDialogModule, BrowserAnimationsModule],
    }),
  ],
  argTypes: {
    featureInfo: {
      type: { name: 'object', value: {} },
    },
  },
  render: (args) => ({
    template: `<gio-ee-unlock-dialog-story [name]="name" [featureInfo]="featureInfo"></gio-ee-unlock-dialog-story>`,
    props: { ...args },
  }),
  parameters: {
    chromatic: { delay: 1000 },
  },
} as Meta;

export const Audit: StoryObj = {
  play: (context) => {
    const button = context.canvasElement.querySelector('#open-dialog') as HTMLButtonElement;
    button.click();
  },
};

Audit.args = {
  featureInfo: FeatureInfoData[Feature.APIM_AUDIT_TRAIL],
  utmMedium: UTMMedium.AUDIT_TRAIL_API,
};

export const DCRProviders: StoryObj = {
  play: (context) => {
    const button = context.canvasElement.querySelector('#open-dialog') as HTMLButtonElement;
    button.click();
  },
};

DCRProviders.args = {
  featureInfo: FeatureInfoData[Feature.APIM_DCR_REGISTRATION],
  utmMedium: UTMMedium.DCR_REGISTRATION,
};

export const DebugMode: StoryObj = {
  play: (context) => {
    const button = context.canvasElement.querySelector('#open-dialog') as HTMLButtonElement;
    button.click();
  },
};

DebugMode.args = {
  featureInfo: FeatureInfoData[Feature.APIM_DEBUG_MODE],
  utmMedium: UTMMedium.DEBUG_MODE,
};

export const OpenIDConnect: StoryObj = {
  play: (context) => {
    const button = context.canvasElement.querySelector('#open-dialog') as HTMLButtonElement;
    button.click();
  },
};

OpenIDConnect.args = {
  featureInfo: FeatureInfoData[Feature.APIM_OPENID_CONNECT_SSO],
  utmMedium: UTMMedium.OPENID_CONNECT,
};

export const Roles: StoryObj = {
  play: (context) => {
    const button = context.canvasElement.querySelector('#open-dialog') as HTMLButtonElement;
    button.click();
  },
};

Roles.args = {
  featureInfo: FeatureInfoData[Feature.APIM_CUSTOM_ROLES],
  utmMedium: UTMMedium.CUSTOM_ROLES,
};

export const ShardingTags: StoryObj = {
  play: (context) => {
    const button = context.canvasElement.querySelector('#open-dialog') as HTMLButtonElement;
    button.click();
  },
};

ShardingTags.args = {
  featureInfo: FeatureInfoData[Feature.APIM_SHARDING_TAGS],
  utmMedium: UTMMedium.SHARDING_TAGS,
};

export const SchemaRegistryArgs: StoryObj = {
  play: (context) => {
    const button = context.canvasElement.querySelector('#open-dialog') as HTMLButtonElement;
    button.click();
  },
};

SchemaRegistryArgs.args = {
  featureInfo: FeatureInfoData[Feature.APIM_SCHEMA_REGISTRY_PROVIDER],
  utmMedium: UTMMedium.CONFLUENT_SCHEMA_REGISTRY,
};
