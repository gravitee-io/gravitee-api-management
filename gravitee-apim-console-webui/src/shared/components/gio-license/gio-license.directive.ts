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
import { Directive, ElementRef, Input, OnDestroy, OnInit } from '@angular/core';
import { filter, map, takeUntil, tap } from 'rxjs/operators';
import { action } from '@storybook/addon-actions';
import { MatDialog } from '@angular/material/dialog';
import { Subject } from 'rxjs';

import { GioLicenseService } from './gio-license.service';
import { Feature } from './gio-license-features';
import { UTMMedium } from './gio-license-utm';

import { GioEeUnlockDialogComponent, GioEeUnlockDialogData } from '../../../components/gio-ee-unlock-dialog/gio-ee-unlock-dialog.component';
import { FeatureMoreInformation } from '../../../entities/feature/FeatureMoreInformation';

export interface GioLicenseOptions {
  feature?: Feature;
  utmMedium?: UTMMedium;
}

@Directive({
  selector: '[gioLicense]',
})
export class GioLicenseDirective implements OnInit, OnDestroy {
  @Input()
  public gioLicense: GioLicenseOptions = {};

  private featureMoreInformation: FeatureMoreInformation;
  private trialURL: string;

  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  private onClick = this.click.bind(this);

  constructor(private readonly licenseService: GioLicenseService, private readonly matDialog: MatDialog, private elRef: ElementRef) {}

  ngOnInit(): void {
    this.licenseService
      .isMissingFeature$(this.gioLicense?.feature)
      .pipe(
        tap(() => {
          this.elRef.nativeElement.removeEventListener('click', this.onClick, true);
        }),
        filter((notAllowed) => this.gioLicense != null && notAllowed),
        map(() => this.licenseService.getFeatureMoreInformation(this.gioLicense.feature)),
        tap((featureMoreInformation) => {
          this.featureMoreInformation = featureMoreInformation;
          this.elRef.nativeElement.addEventListener('click', this.onClick, true);
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();

    if (this.gioLicense?.utmMedium) {
      this.trialURL = this.licenseService.getTrialURL(this.gioLicense.utmMedium);
    }
  }

  ngOnDestroy(): void {
    this.elRef.nativeElement.removeEventListener('click', this.onClick, true);
    this.unsubscribe$.next(false);
    this.unsubscribe$.unsubscribe();
  }

  private click($event: PointerEvent) {
    $event.preventDefault();
    $event.stopPropagation();

    this.matDialog
      .open<GioEeUnlockDialogComponent, GioEeUnlockDialogData, boolean>(GioEeUnlockDialogComponent, {
        data: {
          featureMoreInformation: this.featureMoreInformation,
          trialURL: this.trialURL,
        },
        role: 'alertdialog',
        id: 'gioLicenseDialog',
      })
      .afterClosed()
      .pipe(
        tap((confirmed) => {
          action('confirmed?')(confirmed);
        }),
      )
      .subscribe();
    return false;
  }
}
