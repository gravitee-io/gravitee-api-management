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
import { BehaviorSubject, Observable, of } from 'rxjs';
import { distinctUntilChanged, map, shareReplay, switchMap } from 'rxjs/operators';
import { isEqual } from 'lodash';
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
  );

  protected currentDeploymentDefinition$: Observable<unknown | null> = this.getLastApiFetch$.pipe(
    switchMap((api) => (api.deploymentState === 'NEED_REDEPLOY' ? this.apiService.getCurrentDeployment(this.apiId) : of(null))),
  );

  protected deploymentStates$: Observable<string> = this.getLastApiFetch$.pipe(map((api) => api.deploymentState));

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

  protected openCompareEventDialog(events: [Event, Event]) {
    this.compareEvent = events;
    const jsonDefinitionLeft = this.extractApiDefinition(events[0]);
    const jsonDefinitionRight = this.extractApiDefinition(events[1]);
    this.matDialog
      .open(ApiHistoryV4DeploymentCompareComponent, {
        autoFocus: false,
        data: {
          left: { apiDefinition: jsonDefinitionLeft, version: events[0].properties['DEPLOYMENT_NUMBER'] },
          right: { apiDefinition: jsonDefinitionRight, version: events[1].properties['DEPLOYMENT_NUMBER'] },
        },
        width: GIO_DIALOG_WIDTH.LARGE,
        maxHeight: 'calc(100vh - 90px)',
      })
      .afterClosed()
      .pipe();
  }

  private extractApiDefinition(event: Event): string {
    const payload = JSON.parse(event.payload);
    const definition = JSON.parse(payload.definition);
    return JSON.stringify(definition, null, 2);
  }
}
