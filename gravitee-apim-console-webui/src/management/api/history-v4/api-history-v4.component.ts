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
import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { BehaviorSubject, Observable } from 'rxjs';
import { distinctUntilChanged, filter, map, shareReplay, switchMap, tap } from 'rxjs/operators';
import { isEqual, isNil } from 'lodash';
import { MatDialog } from '@angular/material/dialog';
import { GIO_DIALOG_WIDTH } from '@gravitee/ui-particles-angular';

import { openRollbackDialog } from './rollback-dialog';
import { ApiHistoryV4DeploymentCompareComponent } from './deployment-compare/api-history-v4-deployment-compare.component';

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
})
export class ApiHistoryV4Component {
  private apiId = this.activatedRoute.snapshot.params.apiId;

  protected filter$ = new BehaviorSubject<SearchApiEventParam>(INITIAL_SEARCH_PARAM);

  // Locally share the last API fetch to avoid multiple fetches
  private getLastApiFetch$ = this.apiService.getLastApiFetch(this.apiId).pipe(shareReplay(1));

  protected apiEvents$ = this.getLastApiFetch$.pipe(
    switchMap(() => this.filter$),
    distinctUntilChanged(isEqual),
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

  protected rollback(eventId: string) {
    openRollbackDialog(this.matDialog, this.snackBarService, this.apiService, this.apiId, eventId);
  }

  protected openCompareEventDialog(events: [Event, Event], deploymentStates: string) {
    this.compareEvent = events;
    if (events[0] === null || events[1] === null) {
      return;
    }
    const jsonDefinitionLeft = this.extractApiDefinition(events[0]);
    const jsonDefinitionRight = this.extractApiDefinition(events[1]);
    this.matDialog
      .open(ApiHistoryV4DeploymentCompareComponent, {
        autoFocus: false,
        data: {
          left: {
            eventId: events[0].id,
            hideRollback: deploymentStates !== 'NEED_REDEPLOY' && this.definitionInUseId === events[0].id,
            apiDefinition: jsonDefinitionLeft,
            version: events[0].properties['DEPLOYMENT_NUMBER'],
          },
          right: {
            eventId: events[1].id,
            hideRollback: deploymentStates !== 'NEED_REDEPLOY' && this.definitionInUseId === events[1].id,
            apiDefinition: jsonDefinitionRight,
            version: events[1].properties['DEPLOYMENT_NUMBER'],
          },
        },
        width: GIO_DIALOG_WIDTH.LARGE,
        maxHeight: 'calc(100vh - 90px)',
      })
      .afterClosed()
      .pipe(
        filter((result) => !isNil(result?.rollbackTo)),
        tap((eventId) => openRollbackDialog(this.matDialog, this.snackBarService, this.apiService, this.apiId, eventId)),
      )
      .subscribe();
  }

  protected openCompareEventWithCurrentDialog(eventToCompare: Event) {
    this.apiService
      .getCurrentDeployment(this.apiId)
      .pipe(
        switchMap((currentDeploymentDefinition) => {
          const jsonDefinitionToCompare = this.extractApiDefinition(eventToCompare);
          const jsonCurrentDefinition = JSON.stringify(currentDeploymentDefinition, null, 2);

          return this.matDialog
            .open(ApiHistoryV4DeploymentCompareComponent, {
              autoFocus: false,
              data: {
                left: {
                  eventId: eventToCompare.id,
                  apiDefinition: jsonDefinitionToCompare,
                  version: eventToCompare.properties['DEPLOYMENT_NUMBER'],
                  hideRollback: false,
                },
                right: {
                  eventId: null,
                  apiDefinition: jsonCurrentDefinition,
                  version: 'to deploy',
                  hideRollback: true,
                },
              },
              width: GIO_DIALOG_WIDTH.LARGE,
              maxHeight: 'calc(100vh - 90px)',
            })
            .afterClosed();
        }),
        filter((result) => !isNil(result?.rollbackTo)),
        tap((eventId) => openRollbackDialog(this.matDialog, this.snackBarService, this.apiService, this.apiId, eventId)),
      )
      .subscribe();
  }

  private extractApiDefinition(event: Event): string {
    const payload = JSON.parse(event.payload);
    const definition = JSON.parse(payload.definition);
    return JSON.stringify(definition, null, 2);
  }
}
