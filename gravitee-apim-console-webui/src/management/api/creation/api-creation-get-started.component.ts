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

import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { StateService } from '@uirouter/core';
import { catchError, filter, switchMap, takeUntil, tap } from 'rxjs/operators';
import { EMPTY, of, Subject } from 'rxjs';
import { MatDialog } from '@angular/material/dialog';

import { UIRouterState } from '../../../ajs-upgraded-providers';
import { CockpitService, UtmCampaign } from '../../../services-ngx/cockpit.service';
import { Constants } from '../../../entities/Constants';
import { InstallationService } from '../../../services-ngx/installation.service';
import { Installation } from '../../../entities/installation/installation';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';
import { PolicyService } from '../../../services-ngx/policy.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import {
  GioApiImportDialogComponent,
  GioApiImportDialogData,
} from '../../../shared/components/gio-api-import-dialog/gio-api-import-dialog.component';

@Component({
  selector: 'api-creation-get-started',
  template: require('./api-creation-get-started.component.html'),
  styles: [require('./api-creation-get-started.component.scss')],
})
export class ApiCreationGetStartedComponent implements OnInit, OnDestroy {
  cockpitLink: string;

  isLoading = true;

  policies = [];

  private unsubscribe$: Subject<void> = new Subject<void>();

  constructor(
    @Inject(UIRouterState) private readonly ajsState: StateService,
    @Inject('Constants') private readonly constants: Constants,
    private readonly cockpitService: CockpitService,
    private readonly installationService: InstallationService,
    private readonly permissionService: GioPermissionService,
    private readonly policyService: PolicyService,
    private readonly snackBarService: SnackBarService,
    private readonly matDialog: MatDialog,
  ) {}

  ngOnInit(): void {
    const hasInstallationPermission = this.permissionService.hasAnyMatching(['organization-installation-r']);

    (hasInstallationPermission ? this.installationService.get() : of(undefined))
      .pipe(
        takeUntil(this.unsubscribe$),
        tap((installation) => {
          this.cockpitLink = this.getCockpitLink(installation);
          this.isLoading = false;
        }),
      )
      .subscribe();
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  goToApiCreationWizard(definitionVersion = '2.0.0') {
    this.ajsState.go('management.apis.create', { definitionVersion });
  }

  goToApiImport(definitionVersion = '2.0.0') {
    this.policyService
      .listSwaggerPolicies()
      .pipe(
        takeUntil(this.unsubscribe$),
        switchMap((policies) =>
          this.matDialog
            .open<GioApiImportDialogComponent, GioApiImportDialogData>(GioApiImportDialogComponent, {
              data: {
                definitionVersion,
                policies,
              },
              role: 'alertdialog',
              id: 'importApiDialog',
            })
            .afterClosed(),
        ),
        filter((apiId) => !!apiId),
        tap((apiId) => this.ajsState.go('management.apis.detail.portal.general', { apiId })),
        catchError((err) => {
          this.snackBarService.error(err.error?.message ?? 'An error occurred while importing the API.');
          return EMPTY;
        }),
      )
      .subscribe();
  }

  private getCockpitLink(installation?: Installation): string {
    const cockpitURL = installation?.cockpitURL ?? 'https://cockpit.gravitee.io';

    return this.cockpitService.addQueryParamsForAnalytics(
      cockpitURL,
      UtmCampaign.API_DESIGNER,
      installation?.additionalInformation.COCKPIT_INSTALLATION_STATUS === 'ACCEPTED' ? 'ACCEPTED' : undefined,
    );
  }
}
