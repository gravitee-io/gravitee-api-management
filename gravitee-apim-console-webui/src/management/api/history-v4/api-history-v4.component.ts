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
import { Component, Signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { BehaviorSubject, Observable } from 'rxjs';
import { distinctUntilChanged, filter, map, shareReplay, switchMap, tap } from 'rxjs/operators';
import { isEqual, isNil } from 'lodash';
import { MatDialog } from '@angular/material/dialog';
import { GIO_DIALOG_WIDTH, GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { toSignal } from '@angular/core/rxjs-interop';

import {
  ApiHistoryV4DeploymentCompareDialogComponent,
  ApiHistoryV4DeploymentCompareDialogData,
  ApiHistoryV4DeploymentCompareDialogResult,
} from './deployment-compare-dialog/api-history-v4-deployment-compare-dialog.component';
import {
  ApiHistoryV4DeploymentInfoDialogComponent,
  ApiHistoryV4DeploymentInfoDialogData,
  ApiHistoryV4DeploymentInfoDialogResult,
} from './deployment-info-dialog/api-history-v4-deployment-info-dialog.component';

import { Event, SearchApiEventParam } from '../../../entities/management-api-v2';
import { ApiEventsV2Service } from '../../../services-ngx/api-events-v2.service';
import { ApiV2Service } from '../../../services-ngx/api-v2.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';

const INITIAL_SEARCH_PARAM: SearchApiEventParam = {
  page: 1,
  perPage: 10,
  types: 'PUBLISH_API',
};

@Component({
  selector: 'app-api-history-v4',
  templateUrl: './api-history-v4.component.html',
  styleUrls: ['./api-history-v4.component.scss'],
  standalone: false,
})
export class ApiHistoryV4Component {
  private apiId = this.activatedRoute.snapshot.params.apiId;

  protected filter$ = new BehaviorSubject<SearchApiEventParam>(INITIAL_SEARCH_PARAM);

  // Locally share the last API fetch to avoid multiple fetches
  private getLastApiFetch$ = this.apiService.getLastApiFetch(this.apiId).pipe(shareReplay(1));

  protected apiEvents$ = this.getLastApiFetch$.pipe(
    switchMap(() => this.filter$.pipe(distinctUntilChanged(isEqual))),
    switchMap(({ page, perPage }) =>
      this.eventsService.searchApiEvents(this.apiId, { page: page, perPage: perPage, types: 'PUBLISH_API' }),
    ),
    tap((events) => {
      this.definitionInUseId = this.filter$.value.page === 1 && !isNil(events.data[0]) ? events.data[0].id || null : null;
    }),
  );
  public definitionInUseId = null;

  protected deploymentState$: Observable<string> = this.getLastApiFetch$.pipe(map((api) => api.deploymentState));

  protected compareEvent?: [Event, Event];

  private isV4Native$: Signal<boolean> = toSignal(
    this.getLastApiFetch$.pipe(map((api) => api.definitionVersion === 'V4' && api.type === 'NATIVE')),
  );

  get compareEventLabel(): string {
    return `version ${this.compareEvent[0]?.properties['DEPLOYMENT_NUMBER']} with ${this.compareEvent[1]?.properties['DEPLOYMENT_NUMBER']}`;
  }

  constructor(
    private readonly eventsService: ApiEventsV2Service,
    private readonly apiService: ApiV2Service,
    private readonly activatedRoute: ActivatedRoute,
    private readonly matDialog: MatDialog,
    private readonly snackBarService: SnackBarService,
  ) {}

  protected paginationChange(searchParam: SearchApiEventParam) {
    this.filter$.next({ ...this.filter$.getValue(), page: searchParam.page, perPage: searchParam.perPage });
  }

  protected openCompareEventDialog(events: [Event, Event], deploymentStates: string) {
    if (events[0] === null || events[1] === null) {
      this.compareEvent = null;
      return;
    }
    this.compareEvent = events;
    const jsonDefinitionLeft = this.extractApiDefinition(events[0]);
    const jsonDefinitionRight = this.extractApiDefinition(events[1]);
    const isNativeV4 = this.isV4Native$();
    this.getCompareDialog$({
      left: {
        eventId: events[0].id,
        hideRollback: (deploymentStates !== 'NEED_REDEPLOY' && this.definitionInUseId === events[0].id) || isNativeV4,
        apiDefinition: jsonDefinitionLeft,
        version: events[0].properties['DEPLOYMENT_NUMBER'],
      },
      right: {
        eventId: events[1].id,
        hideRollback: (deploymentStates !== 'NEED_REDEPLOY' && this.definitionInUseId === events[1].id) || isNativeV4,
        apiDefinition: jsonDefinitionRight,
        version: events[1].properties['DEPLOYMENT_NUMBER'],
      },
    }).subscribe();
  }

  protected openCompareEventWithCurrentDialog(eventToCompare: Event) {
    this.apiService
      .getCurrentDeployment(this.apiId)
      .pipe(
        switchMap((currentDeploymentDefinition) => {
          const jsonDefinitionToCompare = this.extractApiDefinition(eventToCompare);
          const jsonCurrentDefinition = JSON.stringify(currentDeploymentDefinition, null, 2);
          const isNativeV4 = this.isV4Native$();

          return this.getCompareDialog$({
            left: {
              eventId: eventToCompare.id,
              apiDefinition: jsonDefinitionToCompare,
              version: eventToCompare.properties['DEPLOYMENT_NUMBER'],
              hideRollback: isNativeV4,
            },
            right: {
              eventId: null,
              apiDefinition: jsonCurrentDefinition,
              version: 'to be deployed',
              hideRollback: isNativeV4,
            },
          });
        }),
      )
      .subscribe();
  }

  protected openToDeployInfoDialog() {
    this.apiService
      .getCurrentDeployment(this.apiId)
      .pipe(
        switchMap((currentDeploymentDefinition) =>
          this.matDialog
            .open<ApiHistoryV4DeploymentInfoDialogComponent, ApiHistoryV4DeploymentInfoDialogData, ApiHistoryV4DeploymentInfoDialogResult>(
              ApiHistoryV4DeploymentInfoDialogComponent,
              {
                data: {
                  version: 'to be deployed',
                  hideRollback: this.isV4Native$(),
                  apiDefinition: JSON.stringify(currentDeploymentDefinition, null, 2),
                },
                width: GIO_DIALOG_WIDTH.LARGE,
                maxHeight: 'calc(100vh - 90px)',
              },
            )
            .afterClosed(),
        ),
      )
      .subscribe();
  }

  protected openVersionInfoDialog(event: Event) {
    this.matDialog
      .open<ApiHistoryV4DeploymentInfoDialogComponent, ApiHistoryV4DeploymentInfoDialogData, ApiHistoryV4DeploymentInfoDialogResult>(
        ApiHistoryV4DeploymentInfoDialogComponent,
        {
          data: {
            version: event.properties['DEPLOYMENT_NUMBER'],
            apiDefinition: this.extractApiDefinition(event),
            eventId: event.id,
            createdAt: event.createdAt,
            label: event.properties['DEPLOYMENT_LABEL'],
            user: event.initiator.displayName,
            hideRollback: this.isV4Native$(),
          },
          width: GIO_DIALOG_WIDTH.LARGE,
          maxHeight: 'calc(100vh - 90px)',
        },
      )
      .afterClosed()
      .pipe(
        filter((result) => !isNil(result?.rollbackTo)),
        tap(({ rollbackTo }) => this.openRollbackDialog(this.apiId, rollbackTo)),
      )
      .subscribe();
  }

  private getCompareDialog$(data: ApiHistoryV4DeploymentCompareDialogData) {
    return this.matDialog
      .open<
        ApiHistoryV4DeploymentCompareDialogComponent,
        ApiHistoryV4DeploymentCompareDialogData,
        ApiHistoryV4DeploymentCompareDialogResult
      >(ApiHistoryV4DeploymentCompareDialogComponent, {
        autoFocus: false,
        data,
        width: GIO_DIALOG_WIDTH.LARGE,
        maxHeight: 'calc(100vh - 90px)',
      })
      .afterClosed()
      .pipe(
        filter((result) => !isNil(result?.rollbackTo)),
        tap(({ rollbackTo }) => this.openRollbackDialog(this.apiId, rollbackTo)),
      );
  }

  private extractApiDefinition(event: Event): string {
    const payload = JSON.parse(event.payload);
    const definition = JSON.parse(payload.definition);
    return JSON.stringify(definition, null, 2);
  }

  private openRollbackDialog(apiId: string, eventId: string) {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
        data: {
          title: 'Rollback API',
          content: 'Are you sure you want to rollback this API? This change will update the API with this deployment version.',
        },
        width: GIO_DIALOG_WIDTH.MEDIUM,
      })
      .afterClosed()
      .pipe(
        filter((result) => !!result),
        switchMap(() => this.apiService.rollback(apiId, eventId)),
      )
      .subscribe({
        next: () => {
          this.snackBarService.success('API deployment has been rolled back successfully');
        },
        error: (error) => {
          this.snackBarService.error(error?.error?.message ?? 'An error occurred while rolling back the API deployment!');
        },
      });
  }
}
