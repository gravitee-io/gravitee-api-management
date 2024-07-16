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

import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatDialog } from '@angular/material/dialog';
import { catchError, distinctUntilChanged, filter, switchMap, tap } from 'rxjs/operators';
import { BehaviorSubject, EMPTY } from 'rxjs';
import { GIO_DIALOG_WIDTH, GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { isEqual } from 'lodash';

import { IntegrationsService } from '../../../services-ngx/integrations.service';
import { AgentStatus, FederatedAPIsResponse, Integration } from '../integrations.model';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { GioTableWrapperFilters } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';

@Component({
  selector: 'app-integration-overview',
  templateUrl: './integration-overview.component.html',
  styleUrls: ['./integration-overview.component.scss'],
})
export class IntegrationOverviewComponent implements OnInit {
  protected readonly AgentStatus = AgentStatus;
  private destroyRef: DestroyRef = inject(DestroyRef);
  public integration: Integration;
  public isLoadingIntegration = true;
  public isLoadingFederatedAPI = true;
  public isLoadingPreview = false;
  public isIngesting = false;
  public federatedAPIs = [];
  public displayedColumns: string[] = ['name', 'actions'];

  public filters: GioTableWrapperFilters = {
    pagination: { index: 1, size: 10 },
    searchTerm: '',
  };
  public nbTotalInstances = this.federatedAPIs.length;
  private filters$ = new BehaviorSubject<GioTableWrapperFilters>(this.filters);

  constructor(
    public readonly integrationsService: IntegrationsService,
    private readonly activatedRoute: ActivatedRoute,
    private readonly router: Router,
    private readonly snackBarService: SnackBarService,
    private readonly matDialog: MatDialog,
  ) {}

  ngOnInit(): void {
    this.getIntegration();
    this.initFederatedAPIsList();
  }

  private initFederatedAPIsList(): void {
    this.isLoadingFederatedAPI = true;
    this.filters$
      .pipe(
        distinctUntilChanged(isEqual),
        switchMap((filters: GioTableWrapperFilters) => {
          this.isLoadingFederatedAPI = true;
          return this.integrationsService.getFederatedAPIs(
            this.activatedRoute.snapshot.paramMap.get('integrationId'),
            filters.pagination.index,
            filters.pagination.size,
          );
        }),
        catchError(({ error }) => {
          this.isLoadingFederatedAPI = false;
          this.snackBarService.error(`APIs list error: ${error.message}`);
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((response: FederatedAPIsResponse): void => {
        this.nbTotalInstances = response.pagination.totalCount;
        this.federatedAPIs = response.data;
        this.isLoadingFederatedAPI = false;
      });
  }

  private getIntegration(): void {
    this.isLoadingIntegration = true;
    this.integrationsService
      .getIntegration(this.activatedRoute.snapshot.paramMap.get('integrationId'))
      .pipe(
        catchError(({ error }) => {
          this.isLoadingIntegration = false;
          this.snackBarService.error(error.message);
          this.router.navigate(['..'], {
            relativeTo: this.activatedRoute,
          });
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((integration: Integration): void => {
        this.integration = integration;
        this.isLoadingIntegration = false;
      });
  }

  public ingest(): void {
    this.isLoadingPreview = true;
    this.snackBarService.success('Preparing discovery...');

    this.integrationsService
      .previewIntegration(this.activatedRoute.snapshot.paramMap.get('integrationId'))
      .pipe(
        switchMap(({ newCount, updateCount }) => {
          this.isLoadingPreview = false;
          return this.matDialog
            .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
              width: GIO_DIALOG_WIDTH.SMALL,
              data: {
                title: 'Discover',
                content: `By proceeding, you'll initiate the creation of ${newCount} new Federated APIs in Gravitee (and ${updateCount} already ingested), one for each API discovered at the provider. Are you ready to continue?`,
                confirmButton: 'Proceed',
              },
              role: 'alertdialog',
              id: 'ingestIntegrationConfirmDialog',
            })
            .afterClosed();
        }),
        filter((confirm) => !!confirm),
        switchMap(() => {
          this.isIngesting = true;
          this.snackBarService.success('We’re discovering assets from the provider...');
          return this.integrationsService.ingestIntegration(this.integration.id);
        }),
        tap(() => {
          this.isIngesting = false;
          this.snackBarService.success('APIs successfully created and ready for use!');
          this.initFederatedAPIsList();
        }),
        catchError(({ error }) => {
          this.isIngesting = false;
          this.isLoadingPreview = false;
          let message = 'Discovery error';

          if (error.httpStatus === 500) {
            message = 'Internal agent error: ' + error.message;
            this.getIntegration();
          }

          this.snackBarService.error(message);
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  onFiltersChanged(filters: GioTableWrapperFilters): void {
    this.filters = { ...this.filters, ...filters };
    this.filters$.next(this.filters);
  }
}
