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
import { MatDialog } from '@angular/material/dialog';
import { MatButtonHarness } from '@angular/material/button/testing';

import { ApiDocumentationV4AddFolderDialog } from './api-documentation-v4-add-folder-dialog.component';
import { ApiDocumentationV4AddFolderDialogHarness } from './api-documentation-v4-add-folder-dialog.harness';

import { ApiDocumentationV4Module } from '../api-documentation-v4.module';

@Component({
  selector: 'gio-dialog-test',
  template: `<button mat-button id="open-dialog-test" (click)="openDialog()">Open dialog</button>`,
})
class TestComponent {
  public result?: any;
  constructor(private readonly matDialog: MatDialog) {}

  public openDialog() {
    this.matDialog
      .open<ApiDocumentationV4AddFolderDialog>(ApiDocumentationV4AddFolderDialog)
      .afterClosed()
      .subscribe((result) => (this.result = result));
  }
}

describe('ApiDocumentationV4AddFolderDialog', () => {
  let fixture: ComponentFixture<TestComponent>;
  let harnessLoader: HarnessLoader;

  const init = async () => {
    TestBed.configureTestingModule({
      declarations: [TestComponent],
      imports: [NoopAnimationsModule, ApiDocumentationV4Module, MatIconTestingModule],
    });

    fixture = TestBed.createComponent(TestComponent);
    harnessLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    const openDialogButton = await harnessLoader.getHarness(MatButtonHarness.with({ selector: '#open-dialog-test' }));
    await openDialogButton.click();
    fixture.detectChanges();
  };

  beforeEach(async () => await init());

  it('should show name input and public / private radio buttons', async () => {
    const addFolderDialogHarness = await harnessLoader.getHarness(ApiDocumentationV4AddFolderDialogHarness);
    expect(addFolderDialogHarness).toBeDefined();
    const input = await addFolderDialogHarness.getNameInput();
    expect(input).toBeDefined();
    await input.setValue('folder');

    const buttons = await addFolderDialogHarness.getRadioButtons();
    const values = await Promise.all(buttons.map(async (btn) => await btn.getValue()));
    expect(values).toEqual(['PUBLIC', 'PRIVATE']);
    await addFolderDialogHarness.selectVisibility('PRIVATE');
    fixture.detectChanges();

    await addFolderDialogHarness.getSaveButton().then((btn) => btn.click());
    expect(fixture.componentInstance.result).toEqual({ name: 'folder', visibility: 'PRIVATE' });
  });
});
