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
import { MatDialog } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { Component } from '@angular/core';
import { MatButtonHarness } from '@angular/material/button/testing';

import { ApiDocumentationV4NewtAiDialogHarness } from './api-documentation-v4-newt-ai-dialog.harness';
import {
  ApiDocumentationV4NewtAiDialogComponent,
  ApiDocumentationV4NewtAiDialogData,
} from './api-documentation-v4-newt-ai-dialog.component';

import { ApiDocumentationV4Module } from '../../api-documentation-v4.module';
import { ApiSpecGenState } from '../../../../../services-ngx/api-spec-gen.service';

@Component({
  selector: 'gio-dialog-test',
  template: ` <button mat-button id="open-dialog-test" (click)="openDialog()">Open dialog</button>`,
})
class TestComponent {
  public result?: any;
  public data: ApiDocumentationV4NewtAiDialogData;

  constructor(private readonly matDialog: MatDialog) {}

  public openDialog() {
    this.matDialog
      .open<ApiDocumentationV4NewtAiDialogComponent, ApiDocumentationV4NewtAiDialogData>(ApiDocumentationV4NewtAiDialogComponent, {
        data: this.data,
      })
      .afterClosed()
      .subscribe((result) => (this.result = result));
  }
}

describe('ApiDocumentationV4NewtAiDialogComponent', () => {
  let harnessLoader: HarnessLoader;
  let fixture: ComponentFixture<TestComponent>;
  let harness: ApiDocumentationV4NewtAiDialogHarness;
  const init = async (data: ApiDocumentationV4NewtAiDialogData) => {
    TestBed.configureTestingModule({
      declarations: [TestComponent],
      imports: [NoopAnimationsModule, ApiDocumentationV4Module, MatIconTestingModule],
    });

    fixture = TestBed.createComponent(TestComponent);
    fixture.componentInstance.data = data;
    harnessLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    const openDialogButton = await harnessLoader.getHarness(MatButtonHarness.with({ selector: '#open-dialog-test' }));
    await openDialogButton.click();
    fixture.detectChanges();

    harness = await harnessLoader.getHarness(ApiDocumentationV4NewtAiDialogHarness);
  };

  describe('Generate Specification', () => {
    beforeEach(async () => {
      await init({ state: ApiSpecGenState.AVAILABLE });
    });

    it('should get list items', async () => {
      const items = await harness.getListItems();
      expect(items.length).toBe(3);
    });

    it('should click generate button', async () => {
      await harness.clickGenerate();
      expect(fixture.componentInstance.result).toBeTruthy();
    });

    it('should click cancel button', async () => {
      await harness.clickCancel();
      expect(fixture.componentInstance.result).toBeFalsy();
    });
  });

  describe('Check Info with STARTED state', () => {
    beforeEach(async () => {
      await init({ state: ApiSpecGenState.STARTED });
    });

    it('should get list items', async () => {
      const items = await harness.getListItems();
      expect(items.length).toBe(3);
    });

    it('should click cancel button', async () => {
      await harness.clickCancel();
      expect(fixture.componentInstance.result).toBeFalsy();
    });
  });

  describe('Check Info with GENERATING state', () => {
    beforeEach(async () => {
      await init({ state: ApiSpecGenState.GENERATING });
    });

    it('should get list items', async () => {
      const items = await harness.getListItems();
      expect(items.length).toBe(3);
    });

    it('should click cancel button', async () => {
      await harness.clickCancel();
      expect(fixture.componentInstance.result).toBeFalsy();
    });
  });
});
