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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Component } from '@angular/core';
import { HarnessLoader } from '@angular/cdk/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';

import { EnvironmentFlowsAddEditDialogHarness } from './environment-flows-add-edit-dialog.harness';
import {
  EnvironmentFlowsAddEditDialogComponent,
  EnvironmentFlowsAddEditDialogData,
  EnvironmentFlowsAddEditDialogResult,
} from './environment-flows-add-edit-dialog.component';

@Component({
  selector: 'test-component',
  template: ``,
  imports: [EnvironmentFlowsAddEditDialogComponent, MatDialogModule, MatIconTestingModule],
  standalone: true,
})
class SpecDialogComponent {
  constructor(private readonly matDialog: MatDialog) {}

  public open(data?: EnvironmentFlowsAddEditDialogData) {
    return this.matDialog.open<
      EnvironmentFlowsAddEditDialogComponent,
      EnvironmentFlowsAddEditDialogData,
      EnvironmentFlowsAddEditDialogResult
    >(EnvironmentFlowsAddEditDialogComponent, {
      data,
      role: 'dialog',
      id: 'test-dialog',
    });
  }
}

describe('EnvironmentFlowsAddEditDialogComponent', () => {
  let fixture: ComponentFixture<SpecDialogComponent>;
  let loader: HarnessLoader;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule],
    }).compileComponents();

    fixture = TestBed.createComponent(SpecDialogComponent);
    fixture.detectChanges();
    loader = TestbedHarnessEnvironment.documentRootLoader(fixture);
  });

  it('should open dialog', async () => {
    fixture.componentInstance.open();

    const dialog = await loader.getHarness(EnvironmentFlowsAddEditDialogHarness);

    expect(await dialog.getTitleText()).toEqual('environment-flows-add-edit-dialog');
    expect(await dialog.getContentText()).toContain('Lorem');
    expect(await dialog.getActionsText()).toContain('My action');
  });

  it('should close dialog', async () => {
    let dialogResult: undefined | boolean;
    fixture.componentInstance
      .open()
      .afterClosed()
      .subscribe((result) => (dialogResult = result));

    const dialog = await loader.getHarness(EnvironmentFlowsAddEditDialogHarness);

    await dialog.close();
    expect(dialogResult).toEqual(false);
  });

  it('should validate & close dialog', async () => {
    let dialogResult: undefined | boolean;
    fixture.componentInstance
      .open()
      .afterClosed()
      .subscribe((result) => (dialogResult = result));

    const dialog = await loader.getHarness(EnvironmentFlowsAddEditDialogHarness);

    await dialog.confirmMyAction();
    expect(dialogResult).toEqual(true);
  });
});
