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

import {
  ApiConfirmDeploymentDialogComponent,
  ApiConfirmDeploymentDialogData,
  ApiConfirmDeploymentDialogResult,
} from './api-confirm-deployment-dialog.component';

import { ApiNavigationModule } from '../api-navigation.module';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';

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
      .open<ApiConfirmDeploymentDialogComponent, ApiConfirmDeploymentDialogData, ApiConfirmDeploymentDialogResult>(
        ApiConfirmDeploymentDialogComponent,
        {
          data: {
            apiId: API_ID,
          },
          role: 'alertdialog',
        },
      )
      .afterClosed()
      .subscribe((result) => (this.result = result));
  }
}

describe('ApiConfirmDeploymentDialogComponent', () => {
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

  it('should deploy api', async () => {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.org.baseURL}/configuration/flows`,
        method: 'GET',
      })
      .flush({
        has_policies: true,
      });

    const dialog = await loader.getHarness(MatDialogHarness);

    expect(await dialog.getText()).toContain('Some policies are defined on the platform and may conflict with your API policies on flows.');

    const messageInput = await dialog.getHarness(MatInputHarness.with({ ancestor: '.content__deploymentLabel' }));
    await messageInput.setValue('Deployment label');

    const deployBtn = await dialog.getHarness(MatButtonHarness.with({ text: 'Deploy' }));
    await deployBtn.click();

    const req = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/deployments`,
      method: 'POST',
    });

    expect(req.request.body).toEqual({ deploymentLabel: 'Deployment label' });
  });
});
