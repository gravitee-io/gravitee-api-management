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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { Component } from '@angular/core';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatButtonModule } from '@angular/material/button';
import { HttpTestingController } from '@angular/common/http/testing';
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';

import { WebhookSettingsDialogComponent, WebhookSettingsDialogData } from './webhook-settings-dialog.component';
import { WebhookSettingsDialogHarness } from './webhook-settings-dialog.harness';

import { GioTestingModule } from '../../../../../../shared/testing';
import { ApiV4, fakeProxyApiV4 } from '../../../../../../entities/management-api-v2';

@Component({
  selector: 'gio-dialog-test',
  template: `<button mat-button id="open-dialog-test" (click)="openDialog()">Open dialog</button>`,
  standalone: true,
  imports: [MatButtonModule, MatDialogModule],
})
class TestComponent {
  public result?: any;
  public dialogData: WebhookSettingsDialogData;
  constructor(private readonly matDialog: MatDialog) {}

  public openDialog() {
    this.matDialog
      .open<WebhookSettingsDialogComponent, WebhookSettingsDialogData>(WebhookSettingsDialogComponent, {
        data: this.dialogData,
      })
      .afterClosed()
      .subscribe((result) => (this.result = result));
  }
}

describe('WebhookSettingsDialogComponent', () => {
  const API_ID = 'api-id';
  let fixture: ComponentFixture<TestComponent>;
  let harnessLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  const init = async (api: ApiV4) => {
    fixture = TestBed.createComponent(TestComponent);
    harnessLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.componentInstance.dialogData = { api };
    const openDialogButton = await harnessLoader.getHarness(MatButtonHarness.with({ selector: '#open-dialog-test' }));
    await openDialogButton.click();
    fixture.detectChanges();
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestComponent, WebhookSettingsDialogComponent, NoopAnimationsModule, MatIconTestingModule, GioTestingModule],
    }).compileComponents();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('when analytics is enabled initially', () => {
    const api = fakeProxyApiV4({
      id: API_ID,
      analytics: {
        enabled: true,
        sampling: { type: 'COUNT', value: '100' },
        logging: {
          content: {
            messagePayload: false,
            messageHeaders: false,
            payload: false,
            headers: false,
          },
        },
      },
    });

    beforeEach(async () => await init(api));

    it('should render the dialog content once the API is loaded', async () => {
      const dialogHarness = await harnessLoader.getHarness(WebhookSettingsDialogHarness);
      expect(dialogHarness).toBeDefined();
      expect(await dialogHarness.isLoading()).toBe(false);
      expect(await dialogHarness.getTitleText()).toBe('Webhook Logs reporting settings');
    });

    it('should have top toggle enabled and checked', async () => {
      const enabledToggle = await harnessLoader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="enabled"]' }));
      expect(await enabledToggle.isChecked()).toBe(true);
      expect(await enabledToggle.isDisabled()).toBe(false);
    });

    it('should enable content data toggles when top toggle is turned on', async () => {
      const enabledToggle = await harnessLoader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="enabled"]' }));
      const requestBodyToggle = await harnessLoader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="requestBody"]' }));

      // Turn off and then back on
      await enabledToggle.toggle();
      expect(await requestBodyToggle.isDisabled()).toBe(true);

      await enabledToggle.toggle();
      expect(await requestBodyToggle.isDisabled()).toBe(false);
    });

    it('should have content data toggles enabled', async () => {
      const requestBodyToggle = await harnessLoader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="requestBody"]' }));
      const requestHeadersToggle = await harnessLoader.getHarness(
        MatSlideToggleHarness.with({ selector: '[formControlName="requestHeaders"]' }),
      );
      const responseBodyToggle = await harnessLoader.getHarness(
        MatSlideToggleHarness.with({ selector: '[formControlName="responseBody"]' }),
      );
      const responseHeadersToggle = await harnessLoader.getHarness(
        MatSlideToggleHarness.with({ selector: '[formControlName="responseHeaders"]' }),
      );

      expect(await requestBodyToggle.isDisabled()).toBe(false);
      expect(await requestHeadersToggle.isDisabled()).toBe(false);
      expect(await responseBodyToggle.isDisabled()).toBe(false);
      expect(await responseHeadersToggle.isDisabled()).toBe(false);
    });

    it('should disable and set content data toggles to false when top toggle is turned off', async () => {
      const enabledToggle = await harnessLoader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="enabled"]' }));
      const requestBodyToggle = await harnessLoader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="requestBody"]' }));
      const requestHeadersToggle = await harnessLoader.getHarness(
        MatSlideToggleHarness.with({ selector: '[formControlName="requestHeaders"]' }),
      );
      const responseBodyToggle = await harnessLoader.getHarness(
        MatSlideToggleHarness.with({ selector: '[formControlName="responseBody"]' }),
      );
      const responseHeadersToggle = await harnessLoader.getHarness(
        MatSlideToggleHarness.with({ selector: '[formControlName="responseHeaders"]' }),
      );

      // Set some content toggles to true first
      await requestBodyToggle.toggle();
      await requestHeadersToggle.toggle();
      expect(await requestBodyToggle.isChecked()).toBe(true);
      expect(await requestHeadersToggle.isChecked()).toBe(true);

      // Turn off the top toggle
      await enabledToggle.toggle();

      // All content toggles should be disabled and false
      expect(await requestBodyToggle.isChecked()).toBe(false);
      expect(await requestBodyToggle.isDisabled()).toBe(true);
      expect(await requestHeadersToggle.isChecked()).toBe(false);
      expect(await requestHeadersToggle.isDisabled()).toBe(true);
      expect(await responseBodyToggle.isChecked()).toBe(false);
      expect(await responseBodyToggle.isDisabled()).toBe(true);
      expect(await responseHeadersToggle.isChecked()).toBe(false);
      expect(await responseHeadersToggle.isDisabled()).toBe(true);
    });

    it('should close the dialog when discard is clicked', async () => {
      // Make a change to ensure the save bar is visible (it only appears when form has unsaved changes)
      const enabledToggle = await harnessLoader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="enabled"]' }));
      await enabledToggle.toggle();
      await fixture.whenStable();
      fixture.detectChanges();

      const dialogHarness = await harnessLoader.getHarness(WebhookSettingsDialogHarness);
      await dialogHarness.clickClose();
      expect(fixture.componentInstance.result).toBeUndefined();
    });
  });

  describe('when analytics is disabled initially', () => {
    const api = fakeProxyApiV4({
      id: API_ID,
      analytics: {
        enabled: false,
        sampling: { type: 'COUNT', value: '100' },
        logging: {
          content: {
            messagePayload: false,
            messageHeaders: false,
            payload: false,
            headers: false,
          },
        },
      },
    });

    beforeEach(async () => await init(api));

    it('should have top toggle enabled and unchecked', async () => {
      const enabledToggle = await harnessLoader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="enabled"]' }));
      expect(await enabledToggle.isChecked()).toBe(false);
      expect(await enabledToggle.isDisabled()).toBe(false);
    });

    it('should have content data toggles disabled', async () => {
      const requestBodyToggle = await harnessLoader.getHarness(MatSlideToggleHarness.with({ selector: '[formControlName="requestBody"]' }));
      const requestHeadersToggle = await harnessLoader.getHarness(
        MatSlideToggleHarness.with({ selector: '[formControlName="requestHeaders"]' }),
      );
      const responseBodyToggle = await harnessLoader.getHarness(
        MatSlideToggleHarness.with({ selector: '[formControlName="responseBody"]' }),
      );
      const responseHeadersToggle = await harnessLoader.getHarness(
        MatSlideToggleHarness.with({ selector: '[formControlName="responseHeaders"]' }),
      );

      expect(await requestBodyToggle.isDisabled()).toBe(true);
      expect(await requestHeadersToggle.isDisabled()).toBe(true);
      expect(await responseBodyToggle.isDisabled()).toBe(true);
      expect(await responseHeadersToggle.isDisabled()).toBe(true);
    });
  });
});
