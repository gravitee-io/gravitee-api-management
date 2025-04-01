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
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { Component } from '@angular/core';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HarnessLoader } from '@angular/cdk/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatButtonHarness } from '@angular/material/button/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { MatDialogHarness } from '@angular/material/dialog/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';

import { ApiReviewDialogComponent, ApiReviewDialogData, ApiReviewDialogResult } from './api-review-dialog.component';

import { QualityRule } from '../../../../entities/qualityRule';
import { ApiQualityRule } from '../../../../entities/apiQualityRule';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { ApiNavigationModule } from '../api-navigation.module';

const API_ID = 'apiId';
@Component({
  selector: 'gio-dialog-test',
  template: `<button mat-button id="open-dialog-test" (click)="openDialog()">Open dialog</button>`,
  standalone: false,
})
class TestComponent {
  public result?: any;
  constructor(private readonly matDialog: MatDialog) {}

  public openDialog() {
    this.matDialog
      .open<ApiReviewDialogComponent, ApiReviewDialogData, ApiReviewDialogResult>(ApiReviewDialogComponent, {
        data: {
          apiId: API_ID,
        },
        role: 'alertdialog',
      })
      .afterClosed()
      .subscribe((result) => (this.result = result));
  }
}

describe('ApiReviewDialogComponent', () => {
  let fixture: ComponentFixture<TestComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      declarations: [TestComponent],
      imports: [ApiNavigationModule, NoopAnimationsModule, MatDialogModule, MatIconTestingModule, GioTestingModule],
    });

    fixture = TestBed.createComponent(TestComponent);
    httpTestingController = TestBed.inject(HttpTestingController);

    loader = TestbedHarnessEnvironment.documentRootLoader(fixture);

    const openDialogButton = await loader.getHarness(MatButtonHarness.with({ selector: '#open-dialog-test' }));
    await openDialogButton.click();
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should Accept the review', async () => {
    const dialog = await loader.getHarness(MatDialogHarness);

    // Expect Loading... state
    expect(await dialog.getText()).toEqual('API Review');

    const qualityRules: QualityRule[] = [
      { id: 'withoutApiValue', name: 'QR withoutApiValue', description: 'description', weight: 42 },
      { id: 'withApiValue', name: 'QR withApiValue', description: 'description', weight: 42 },
    ];
    const apiQualityRules: ApiQualityRule[] = [
      {
        quality_rule: 'withApiValue',
        api: API_ID,
        checked: true,
        createdAt: '',
        updatedAt: '',
      },
    ];

    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/configuration/quality-rules`,
        method: 'GET',
      })
      .flush(qualityRules);

    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/${API_ID}/quality-rules`,
        method: 'GET',
      })
      .flush(apiQualityRules);

    const qrWithoutApiValueToggle = await dialog.getHarness(
      MatSlideToggleHarness.with({
        name: 'QR withoutApiValue',
      }),
    );
    expect(await qrWithoutApiValueToggle.isChecked()).toBe(false);
    await qrWithoutApiValueToggle.toggle();

    const qrWithApiValueToggle = await dialog.getHarness(
      MatSlideToggleHarness.with({
        name: 'QR withApiValue',
      }),
    );
    expect(await qrWithApiValueToggle.isChecked()).toBe(true);
    await qrWithApiValueToggle.toggle();

    const reviewCommentsInput = await dialog.getHarness(MatInputHarness.with({ ancestor: '.content__comments' }));
    await reviewCommentsInput.setValue('Review comments');

    const deployBtn = await dialog.getHarness(MatButtonHarness.with({ text: 'Accept' }));
    await deployBtn.click();

    const updateApiQualityRuleReq = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.baseURL}/apis/${API_ID}/quality-rules/withApiValue`,
      method: 'PUT',
    });
    expect(updateApiQualityRuleReq.request.body).toEqual({ checked: false });
    updateApiQualityRuleReq.flush({});

    const createApiQualityRuleReq = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.baseURL}/apis/${API_ID}/quality-rules`,
      method: 'POST',
    });
    expect(createApiQualityRuleReq.request.body).toEqual({ api: API_ID, quality_rule: 'withoutApiValue', checked: true });
    createApiQualityRuleReq.flush({});

    const acceptReq = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/reviews/_accept`,
      method: 'POST',
    });
    expect(acceptReq.request.body).toEqual({ message: 'Review comments' });
    acceptReq.flush({});
  });

  it('should Reject the review', async () => {
    const dialog = await loader.getHarness(MatDialogHarness);

    // Expect Loading... state
    expect(await dialog.getText()).toEqual('API Review');

    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/configuration/quality-rules`,
        method: 'GET',
      })
      .flush([]);

    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/${API_ID}/quality-rules`,
        method: 'GET',
      })
      .flush([]);

    const rejectBtn = await dialog.getHarness(MatButtonHarness.with({ text: 'Reject' }));
    await rejectBtn.click();

    const rejectReq = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/reviews/_reject`,
      method: 'POST',
    });
    expect(rejectReq.request.body).toEqual({ message: null });
    rejectReq.flush({});
  });
});
