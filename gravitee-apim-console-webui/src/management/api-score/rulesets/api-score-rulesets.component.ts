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

import { Component, DestroyRef, ElementRef, OnInit, ViewChild } from '@angular/core';
import { GIO_DIALOG_WIDTH, GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatDialog } from '@angular/material/dialog';
import { filter, switchMap } from 'rxjs/operators';

import { RulesetV2Service } from '../../../services-ngx/ruleset-v2.service';
import { ScoringFunction, ScoringRuleset } from '../../../entities/management-api-v2/api/v4/ruleset';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';

@Component({
  selector: 'app-api-score-rulesets',
  templateUrl: './api-score-rulesets.component.html',
  styleUrl: './api-score-rulesets.component.scss',
  standalone: false,
})
export class ApiScoreRulesetsComponent implements OnInit {
  public rulesets: ScoringRuleset[];
  public functions: ScoringFunction[];
  public isLoadingRulesets = true;
  public isLoadingFunctions = true;

  constructor(
    private readonly rulesetV2Service: RulesetV2Service,
    private readonly destroyRef: DestroyRef,
    private readonly snackBarService: SnackBarService,
    private readonly matDialog: MatDialog,
  ) {}

  @ViewChild('container') container: ElementRef;

  ngOnInit(): void {
    this.rulesetV2Service
      .listRulesets()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.isLoadingRulesets = false;
          this.rulesets = res.data;
        },
        error: () => {
          this.snackBarService.error('Rulesets error!');
        },
      });

    this.rulesetV2Service
      .listFunctions()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.isLoadingFunctions = false;
          this.functions = res.data;
        },
        error: () => {
          this.snackBarService.error('Functions error!');
        },
      });
  }

  public deleteRuleset(id: string) {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        width: GIO_DIALOG_WIDTH.SMALL,
        data: {
          title: 'Delete this ruleset',
          content: 'Please note that once your ruleset is deleted, it cannot be restored.',
          confirmButton: 'Delete ruleset',
        },
        role: 'alertdialog',
        id: 'deleteRulesetConfirmDialog',
      })
      .afterClosed()
      .pipe(
        filter((confirm) => !!confirm),
        switchMap(() => {
          this.isLoadingRulesets = true;
          return this.rulesetV2Service.deleteRuleset(id);
        }),
        switchMap(() => {
          return this.rulesetV2Service.listRulesets();
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (res) => {
          this.rulesets = res.data;
          this.snackBarService.success('Ruleset successfully deleted!');
          this.isLoadingRulesets = false;
        },
        error: () => {
          this.snackBarService.error('Ruleset deletion error!');
        },
      });
  }

  public deleteFunction(id: string) {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        width: GIO_DIALOG_WIDTH.SMALL,
        data: {
          title: 'Delete this function',
          content: 'Please note that once your function is deleted, it cannot be restored.',
          confirmButton: 'Delete function',
        },
        role: 'alertdialog',
        id: 'deleteFunctionConfirmDialog',
      })
      .afterClosed()
      .pipe(
        filter((confirm) => !!confirm),
        switchMap(() => {
          this.isLoadingFunctions = true;
          return this.rulesetV2Service.deleteFunction(id);
        }),
        switchMap(() => {
          return this.rulesetV2Service.listFunctions();
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (res) => {
          this.functions = res.data;
          this.snackBarService.success('Function successfully deleted!');
          this.isLoadingFunctions = false;
        },
        error: () => {
          this.snackBarService.error('Function deletion error!');
        },
      });
  }
}
