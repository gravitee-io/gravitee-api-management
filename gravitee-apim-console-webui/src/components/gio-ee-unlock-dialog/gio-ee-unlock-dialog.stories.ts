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

import { FeatureMoreInformation } from '../../entities/feature/FeatureMoreInformation';

@Component({
  selector: 'gio-ee-unlock-dialog-story',
  template: `<button id="open-dialog" (click)="openDialog()">More information</button>`,
})
class GioEeUnlockDialogStoryComponent {
  @Input() public featureMoreInformation: FeatureMoreInformation;
  constructor(private readonly matDialog: MatDialog) {}

  public openDialog() {
    this.matDialog
      .open<GioEeUnlockDialogComponent, GioEeUnlockDialogData, boolean>(GioEeUnlockDialogComponent, {
        data: {
          featureMoreInformation: this.featureMoreInformation,
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
    featureMoreInformation: {
      type: { name: 'object', value: {} },
    },
  },
  render: (args) => ({
    template: `<gio-ee-unlock-dialog-story [name]="name" [featureMoreInformation]="featureMoreInformation"></gio-ee-unlock-dialog-story>`,
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
  featureMoreInformation: {
    image: '../../assets/gio-ee-unlock-dialog/audit-trail.png',
    description:
      'Audit is part of Gravitee Enterprise. Audit gives you a complete understanding of events and their context to strengthen your security posture.',
  },
};

export const DCRProviders: StoryObj = {
  play: (context) => {
    const button = context.canvasElement.querySelector('#open-dialog') as HTMLButtonElement;
    button.click();
  },
};

DCRProviders.args = {
  featureMoreInformation: {
    image: '../../assets/gio-ee-unlock-dialog/dcr-providers.png',
    description:
      "Dynamic Client Registration (DCR) Provider is part of Gravitee Enterprise. DCR enhances your API's security by seamlessly integrating OAuth 2.0 and OpenID Connect.",
  },
};

export const DebugMode: StoryObj = {
  play: (context) => {
    const button = context.canvasElement.querySelector('#open-dialog') as HTMLButtonElement;
    button.click();
  },
};

DebugMode.args = {
  featureMoreInformation: {
    image: '../../assets/gio-ee-unlock-dialog/debug-mode.png',
    description:
      'Debug Mode is part of Gravitee Enterprise. Debug Mode allows you to troubleshooting your API proxies running on API and to understand your policies execution.',
  },
};
export const OpenIDConnect: StoryObj = {
  play: (context) => {
    const button = context.canvasElement.querySelector('#open-dialog') as HTMLButtonElement;
    button.click();
  },
};

OpenIDConnect.args = {
  featureMoreInformation: {
    image: '../../assets/gio-ee-unlock-dialog/openid-connect.png',
    description:
      'OpenID Connect is part of Gravitee Enterprise. OpenID Connect allows clients of all types, including Web-based, mobile, and JavaScript, to authenticate.',
  },
};

export const Roles: StoryObj = {
  play: (context) => {
    const button = context.canvasElement.querySelector('#open-dialog') as HTMLButtonElement;
    button.click();
  },
};

Roles.args = {
  featureMoreInformation: {
    image: '../../assets/gio-ee-unlock-dialog/roles-customisation.png',
    description: 'Roles is part of Gravitee Enterprise. Customized roles allows you to specify system access to authorized users.',
  },
};

export const ShardingTags: StoryObj = {
  play: (context) => {
    const button = context.canvasElement.querySelector('#open-dialog') as HTMLButtonElement;
    button.click();
  },
};

ShardingTags.args = {
  featureMoreInformation: {
    image: '../../assets/gio-ee-unlock-dialog/sharding-tags.png',
    description: 'Sharding Tags is part of Gravitee Enterprise. Sharding Tags allows you to “tag” a Gateway with a specific keyword.',
  },
};
