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
import { Injectable } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';

import { GioLicenseService } from './gio-license.service';
import { stringFeature } from './gio-license-features';
import { UTMMedium } from './gio-license-utm';

import { GioEeUnlockDialogComponent, GioEeUnlockDialogData } from '../../../components/gio-ee-unlock-dialog/gio-ee-unlock-dialog.component';

@Injectable({
  providedIn: 'root',
})
export class GioLicenseDialog {
  constructor(private readonly licenseService: GioLicenseService, private readonly matDialog: MatDialog) {}

  displayUpgradeCta(utmMedium: UTMMedium) {
    event?.stopPropagation();
    const featureMoreInformation = this.licenseService.getFeatureMoreInformation(stringFeature('apim-ee-upgrade'));
    const trialURL = this.licenseService.getTrialURL(utmMedium);
    this.matDialog
      .open<GioEeUnlockDialogComponent, GioEeUnlockDialogData, boolean>(GioEeUnlockDialogComponent, {
        data: {
          featureMoreInformation,
          trialURL,
        },
        role: 'alertdialog',
        id: 'gioLicenseDialog',
      })
      .afterClosed()
      .subscribe();
  }
}
