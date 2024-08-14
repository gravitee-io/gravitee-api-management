/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { InteractivityChecker } from '@angular/cdk/a11y';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { MoreFiltersDialogComponent, MoreFiltersDialogData } from './more-filters-dialog.component';
import { MoreFiltersDialogHarness } from './more-filters-dialog.harness';

@Component({
  selector: 'app-more-filters-dialog-test',
  template: ` <button (click)="openDialog()">Open sesame</button>`,
  standalone: true,
})
class TestComponent {
  public result: MoreFiltersDialogData = {};

  constructor(private readonly matDialog: MatDialog) {}

  public openDialog() {
    this.matDialog
      .open<MoreFiltersDialogComponent, MoreFiltersDialogData>(MoreFiltersDialogComponent, {
        id: 'moreFiltersDialog',
        data: this.result,
      })
      .afterClosed()
      .subscribe({ next: result => (this.result = result) });
  }
}

describe('MoreFiltersDialogComponent', () => {
  let fixture: ComponentFixture<TestComponent>;
  let rootHarnessLoader: HarnessLoader;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestComponent, MoreFiltersDialogComponent, NoopAnimationsModule],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
          isTabbable: () => true, // This traps focus checks and so avoid warnings when dealing with
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(TestComponent);
    rootHarnessLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);

    fixture.detectChanges();

    const btn: HTMLButtonElement = fixture.debugElement.query(By.css('button')).nativeElement;
    expect(btn).toBeTruthy();
    btn.click();
  });

  it('should only select start date that is before today', async () => {
    const dialog = await rootHarnessLoader.getHarness(MoreFiltersDialogHarness);
    const startDatePicker = await dialog.getStartDatePicker();
    expect(await startDatePicker.getValue()).toEqual('');

    await startDatePicker.openCalendar();
    const calendar = await startDatePicker.getCalendar();
    await calendar.selectCell({ text: '15' });
    expect(await startDatePicker.getValue()).toEqual('6/15/2016');

    await calendar.selectCell({ text: '30' });
    expect(await startDatePicker.getValue()).toEqual('6/15/2016');
  });

  it('should only select end date that is before today', async () => {
    const dialog = await rootHarnessLoader.getHarness(MoreFiltersDialogHarness);
    const endDatePicker = await dialog.getEndDatePicker();
    expect(await endDatePicker.getValue()).toEqual('');

    await endDatePicker.openCalendar();
    const calendar = await endDatePicker.getCalendar();
    await calendar.selectCell({ text: '15' });
    expect(await endDatePicker.getValue()).toEqual('6/15/2016');

    await calendar.selectCell({ text: '30' });
    expect(await endDatePicker.getValue()).toEqual('6/15/2016');
  });

  it('should only select end date that is between start date and today', async () => {
    const dialog = await rootHarnessLoader.getHarness(MoreFiltersDialogHarness);
    const startDatePicker = await dialog.getStartDatePicker();
    await startDatePicker.openCalendar();
    const startDateCalendar = await startDatePicker.getCalendar();
    await startDateCalendar.selectCell({ text: '15' });
    expect(await startDatePicker.getValue()).toEqual('6/15/2016');
    await startDatePicker.closeCalendar();

    const endDatePicker = await dialog.getEndDatePicker();
    expect(await endDatePicker.getValue()).toEqual('');
    await endDatePicker.openCalendar();
    const endDateCalendar = await endDatePicker.getCalendar();
    await endDateCalendar.selectCell({ text: '15' });
    expect(await endDatePicker.getValue()).toEqual('');

    await endDateCalendar.selectCell({ text: '1' });
    expect(await endDatePicker.getValue()).toEqual('');

    await endDateCalendar.selectCell({ text: '16' });
    expect(await endDatePicker.getValue()).toEqual('6/16/2016');
  });
});
