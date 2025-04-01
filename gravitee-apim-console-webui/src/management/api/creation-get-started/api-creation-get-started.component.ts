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
import { catchError, filter, switchMap, takeUntil, tap } from 'rxjs/operators';
import { EMPTY, of, Subject } from 'rxjs';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, Router } from '@angular/router';

import { CockpitService, UtmCampaign } from '../../../services-ngx/cockpit.service';
import { Constants } from '../../../entities/Constants';
import { InstallationService } from '../../../services-ngx/installation.service';
import { Installation } from '../../../entities/installation/installation';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';
import { PolicyService } from '../../../services-ngx/policy.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { GioApiImportDialogComponent, GioApiImportDialogData } from '../component/gio-api-import-dialog/gio-api-import-dialog.component';
import {
  GioInformationDialogComponent,
  GioConnectorDialogData,
} from '../component/gio-information-dialog/gio-information-dialog.component';

@Component({
  selector: 'api-creation-get-started',
  templateUrl: './api-creation-get-started.component.html',
  styleUrls: ['./api-creation-get-started.component.scss'],
  standalone: false,
})
export class ApiCreationGetStartedComponent implements OnInit, OnDestroy {
  cockpitLink: string;

  isLoading = true;
  isOEM = false;

  policies = [];

  private unsubscribe$: Subject<void> = new Subject<void>();

  constructor(
    @Inject(Constants) private readonly constants: Constants,
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
    private readonly cockpitService: CockpitService,
    private readonly installationService: InstallationService,
    private readonly permissionService: GioPermissionService,
    private readonly policyService: PolicyService,
    private readonly snackBarService: SnackBarService,
    private readonly matDialog: MatDialog,
  ) {}

  ngOnInit(): void {
    const hasInstallationPermission = this.permissionService.hasAnyMatching(['organization-installation-r']);
    this.isOEM = this.constants.isOEM;

    (hasInstallationPermission ? this.installationService.get() : of(undefined))
      .pipe(
        tap((installation) => {
          this.cockpitLink = this.getCockpitLink(installation);
          this.isLoading = false;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  goToApiImport() {
    this.policyService
      .listSwaggerPolicies()
      .pipe(
        switchMap((policies) =>
          this.matDialog
            .open<GioApiImportDialogComponent, GioApiImportDialogData>(GioApiImportDialogComponent, {
              data: {
                policies,
              },
              role: 'alertdialog',
              id: 'importApiDialog',
            })
            .afterClosed(),
        ),
        filter((apiId) => !!apiId),
        tap((apiId) => this.router.navigate(['..', apiId], { relativeTo: this.activatedRoute })),
        catchError((err) => {
          this.snackBarService.error(err.error?.message ?? 'An error occurred while importing the API.');
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
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

  onMoreInfoClick(event: MouseEvent) {
    event.stopPropagation();
    this.matDialog
      .open<GioInformationDialogComponent, GioConnectorDialogData, boolean>(GioInformationDialogComponent, {
        data: {
          name: 'Classic (V2) and New (V4) APIs compared',
          information: {
            description:
              'Gravitee V4 APIs contain most of the same capabilities as V2 APIs, including analytics, logs, failover, and health check. They also support some features not available in V2 APIs, such as protocol mediation, shared policy groups, and native Kafka support. Some functionalities are not yet included, however, including alerts, tenants, and some analytics capabilities.',
          },
        },
        role: 'alertdialog',
        id: 'moreInfoDialog',
      })
      .afterClosed()
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe();
  }
}
