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

import { Component, DestroyRef, OnInit } from '@angular/core';
import { GIO_DIALOG_WIDTH, GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatDialog } from '@angular/material/dialog';
import { filter, switchMap } from 'rxjs/operators';

import { RulesetV2Service } from '../../../services-ngx/ruleset-v2.service';
import { ScoringRuleset } from '../../../entities/management-api-v2/api/v4/ruleset';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';

@Component({
  selector: 'app-api-score-rulesets',
  templateUrl: './api-score-rulesets.component.html',
  styleUrl: './api-score-rulesets.component.scss',
})
export class ApiScoreRulesetsComponent implements OnInit {
  public rulesets: ScoringRuleset[];
  public isLoading = false;

  constructor(
    private readonly rulesetV2Service: RulesetV2Service,
    private readonly destroyRef: DestroyRef,
    private readonly snackBarService: SnackBarService,
    private readonly matDialog: MatDialog,
  ) {}

  ngOnInit(): void {
    this.isLoading = true;
    this.rulesetV2Service
      .listRulesets()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.isLoading = false;
          this.rulesets = res.data;
        },
        error: () => {
          this.snackBarService.error('Something went wrong!');
          this.isLoading = false;
        },
      });
  }

  public delete(id: string) {
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
          this.isLoading = true;
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
          this.isLoading = false;
        },
        error: () => {
          this.snackBarService.error('Something went wrong!');
          this.isLoading = false;
        },
      });
  }
}
