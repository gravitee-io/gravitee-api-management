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

import {
  ApiDocumentationV4EditFolderDialog,
  ApiDocumentationV4EditFolderDialogData,
} from './api-documentation-v4-edit-folder-dialog.component';
import { ApiDocumentationV4EditFolderDialogHarness } from './api-documentation-v4-edit-folder-dialog.harness';

import { ApiDocumentationV4Module } from '../../api-documentation-v4.module';

@Component({
  selector: 'gio-dialog-test',
  template: `<button mat-button id="open-dialog-test" (click)="openDialog()">Open dialog</button>`,
  standalone: false,
})
class TestComponent {
  public result?: any;
  public data: ApiDocumentationV4EditFolderDialogData;
  constructor(private readonly matDialog: MatDialog) {}

  public openDialog() {
    this.matDialog
      .open<ApiDocumentationV4EditFolderDialog, ApiDocumentationV4EditFolderDialogData>(ApiDocumentationV4EditFolderDialog, {
        data: this.data,
      })
      .afterClosed()
      .subscribe(result => (this.result = result));
  }
}

describe('ApiDocumentationV4EditFolderDialog', () => {
  let fixture: ComponentFixture<TestComponent>;
  let harnessLoader: HarnessLoader;

  const init = async (data: ApiDocumentationV4EditFolderDialogData) => {
    TestBed.configureTestingModule({
      declarations: [TestComponent],
      imports: [NoopAnimationsModule, ApiDocumentationV4Module, MatIconTestingModule],
    });

    fixture = TestBed.createComponent(TestComponent);
    harnessLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    fixture.componentInstance.data = data;
    const openDialogButton = await harnessLoader.getHarness(MatButtonHarness.with({ selector: '#open-dialog-test' }));
    await openDialogButton.click();
    fixture.detectChanges();
  };

  describe('Create Folder', () => {
    const EXISTING_FOLDER = 'folder-1';
    beforeEach(async () => await init({ mode: 'create', existingNames: [EXISTING_FOLDER] }));

    it('should show name input and public / private radio buttons', async () => {
      const addFolderDialogHarness = await harnessLoader.getHarness(ApiDocumentationV4EditFolderDialogHarness);
      expect(addFolderDialogHarness).toBeDefined();
      const input = await addFolderDialogHarness.getNameInput();
      expect(input).toBeDefined();
      await input.setValue('folder');

      const buttons = await addFolderDialogHarness.getSelectionInlineCards();
      const values = buttons.map(b => b.value);
      expect(values).toEqual(['PUBLIC', 'PRIVATE']);
      await addFolderDialogHarness.selectVisibility('PRIVATE');
      fixture.detectChanges();

      await addFolderDialogHarness.getSaveButton().then(btn => btn.click());
      expect(fixture.componentInstance.result).toEqual({ name: 'folder', visibility: 'PRIVATE' });
    });
    it('should not allow same name folder', async () => {
      const addFolderDialogHarness = await harnessLoader.getHarness(ApiDocumentationV4EditFolderDialogHarness);
      expect(addFolderDialogHarness).toBeDefined();
      const input = await addFolderDialogHarness.getNameInput();
      expect(input).toBeDefined();
      await input.setValue('Folder-1 ');

      expect(await addFolderDialogHarness.getSaveButton().then(btn => btn.isDisabled())).toEqual(true);
    });
  });

  describe('Update Folder', () => {
    beforeEach(async () => await init({ mode: 'edit', visibility: 'PUBLIC', name: 'folder-name', existingNames: ['existing-folder'] }));

    it('should not be able to submit after opening dialog', async () => {
      const addFolderDialogHarness = await harnessLoader.getHarness(ApiDocumentationV4EditFolderDialogHarness);
      expect(addFolderDialogHarness).toBeDefined();
      expect(await addFolderDialogHarness.getSaveButton().then(btn => btn.isDisabled())).toEqual(true);
    });

    it('should not be able to submit if data the same after changes', async () => {
      const addFolderDialogHarness = await harnessLoader.getHarness(ApiDocumentationV4EditFolderDialogHarness);
      expect(addFolderDialogHarness).toBeDefined();

      const input = await addFolderDialogHarness.getNameInput();
      expect(await input.getValue()).toEqual('folder-name');
      await input.setValue('hehe');
      fixture.detectChanges();
      await input.setValue('folder-name');
      fixture.detectChanges();

      expect(await addFolderDialogHarness.getSaveButton().then(btn => btn.isDisabled())).toEqual(true);
    });

    it('should not be able to submit empty name', async () => {
      const addFolderDialogHarness = await harnessLoader.getHarness(ApiDocumentationV4EditFolderDialogHarness);
      expect(addFolderDialogHarness).toBeDefined();

      const input = await addFolderDialogHarness.getNameInput();
      expect(await input.getValue()).toEqual('folder-name');
      await input.setValue('');
      expect(await addFolderDialogHarness.getSaveButton().then(btn => btn.isDisabled())).toEqual(true);
    });

    it('should not allow same name as existing page', async () => {
      const addFolderDialogHarness = await harnessLoader.getHarness(ApiDocumentationV4EditFolderDialogHarness);
      expect(addFolderDialogHarness).toBeDefined();

      const input = await addFolderDialogHarness.getNameInput();
      expect(await input.getValue()).toEqual('folder-name');
      await input.setValue(' Existing-folder ');
      expect(await addFolderDialogHarness.getSaveButton().then(btn => btn.isDisabled())).toEqual(true);
    });

    it('should update with name change', async () => {
      const addFolderDialogHarness = await harnessLoader.getHarness(ApiDocumentationV4EditFolderDialogHarness);
      expect(addFolderDialogHarness).toBeDefined();

      const input = await addFolderDialogHarness.getNameInput();
      expect(await input.getValue()).toEqual('folder-name');
      await input.setValue('new name');

      expect(await addFolderDialogHarness.getSaveButton().then(btn => btn.isDisabled())).toEqual(false);
      await addFolderDialogHarness.getSaveButton().then(btn => btn.click());
      expect(fixture.componentInstance.result).toEqual({ name: 'new name', visibility: 'PUBLIC' });
    });

    it('should update with visibility change', async () => {
      const addFolderDialogHarness = await harnessLoader.getHarness(ApiDocumentationV4EditFolderDialogHarness);
      expect(addFolderDialogHarness).toBeDefined();

      await addFolderDialogHarness.selectVisibility('PRIVATE');
      fixture.detectChanges();

      expect(await addFolderDialogHarness.getSaveButton().then(btn => btn.isDisabled())).toEqual(false);
      await addFolderDialogHarness.getSaveButton().then(btn => btn.click());
      expect(fixture.componentInstance.result).toEqual({ name: 'folder-name', visibility: 'PRIVATE' });
    });
  });

  describe('Empty parent folder', () => {
    it('should allow creation', async () => {
      await init({ mode: 'create', existingNames: [] });

      const addFolderDialogHarness = await harnessLoader.getHarness(ApiDocumentationV4EditFolderDialogHarness);
      const input = await addFolderDialogHarness.getNameInput();
      await input.setValue('folder');

      expect(await addFolderDialogHarness.getSaveButton().then(btn => btn.isDisabled())).toEqual(false);
    });
    it('should allow update', async () => {
      await init({ mode: 'edit', visibility: 'PUBLIC', name: 'folder-name', existingNames: [] });

      const addFolderDialogHarness = await harnessLoader.getHarness(ApiDocumentationV4EditFolderDialogHarness);
      const input = await addFolderDialogHarness.getNameInput();
      await input.setValue('folder');

      expect(await addFolderDialogHarness.getSaveButton().then(btn => btn.isDisabled())).toEqual(false);
    });
  });
});
