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
import { Component, OnDestroy, OnInit } from '@angular/core';
import { EMPTY, Subject } from 'rxjs';
import { catchError, filter, switchMap, takeUntil, tap } from 'rxjs/operators';
import { GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, Router } from '@angular/router';

import { AuthenticationStrategyService } from '../../../services-ngx/authentication-strategy.service';
import { AuthenticationStrategy } from '../../../entities/authentication-strategy/authenticationStrategy';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';

export type StrategiesTableDS = {
  id: string;
  name: string;
  displayName: string;
  type: string;
  description: string;
  updatedAt: number;
};

@Component({
  selector: 'authentication-strategies',
  templateUrl: './authentication-strategies.component.html',
  styleUrls: ['./authentication-strategies.component.scss'],
  standalone: false,
})
export class AuthenticationStrategiesComponent implements OnInit, OnDestroy {
  strategiesTableDS: StrategiesTableDS[] = [];
  displayedColumns = ['name', 'displayName', 'type', 'description', 'updatedAt', 'actions'];
  isLoadingData = true;
  private unsubscribe$ = new Subject<void>();

  constructor(
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
    private readonly authenticationStrategyService: AuthenticationStrategyService,
    private readonly snackBarService: SnackBarService,
    private readonly matDialog: MatDialog,
  ) {}

  ngOnDestroy(): void {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  ngOnInit(): void {
    this.authenticationStrategyService
      .list()
      .pipe(
        tap(strategies => {
          this.strategiesTableDS = toStrategiesTableDS(strategies);
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => {
        this.isLoadingData = false;
      });
  }

  onAddStrategy() {
    return this.router.navigate(['new'], { relativeTo: this.activatedRoute });
  }

  onEditActionClicked(strategy: StrategiesTableDS) {
    return this.router.navigate([strategy.id], { relativeTo: this.activatedRoute });
  }

  onRemoveActionClicked(strategy: StrategiesTableDS) {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        width: '500px',
        data: {
          title: 'Delete authentication strategy',
          content: `Are you sure you want to delete the authentication strategy <strong>${strategy.name}</strong>?`,
          confirmButton: 'Delete',
        },
        role: 'alertdialog',
        id: 'removeAuthenticationStrategyConfirmDialog',
      })
      .afterClosed()
      .pipe(
        filter(confirm => confirm === true),
        switchMap(() => this.authenticationStrategyService.delete(strategy.id)),
        tap(() => this.snackBarService.success(`"${strategy.name}" has been deleted.`)),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => this.ngOnInit());
  }
}

const toStrategiesTableDS = (strategies: AuthenticationStrategy[]): StrategiesTableDS[] => {
  return (strategies || []).map(strategy => ({
    id: strategy.id,
    name: strategy.name,
    displayName: strategy.display_name || '',
    type: strategy.type,
    description: strategy.description || '',
    updatedAt: strategy.updated_at,
  }));
};
