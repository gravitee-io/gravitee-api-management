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
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { filter, switchMap } from 'rxjs/operators';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { GIO_DIALOG_WIDTH, GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { MatDialog } from '@angular/material/dialog';

import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { RulesetV2Service } from '../../../../services-ngx/ruleset-v2.service';
import { EditRulesetRequestData } from '../../../../entities/management-api-v2/api/v4/ruleset';

@Component({
  selector: 'edit-api-score-ruleset',
  templateUrl: './edit-api-score-ruleset.component.html',
  styleUrl: './edit-api-score-ruleset.component.scss',
  standalone: false,
})
export class EditApiScoreRulesetComponent implements OnInit {
  public isLoading = false;
  public filePreview = null;

  public form: FormGroup = this.formBuilder.group({
    name: ['', [Validators.required, Validators.maxLength(50), Validators.minLength(1)]],
    description: ['', Validators.maxLength(250)],
  });

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly snackBarService: SnackBarService,
    private readonly rulesetV2Service: RulesetV2Service,
    private readonly activatedRoute: ActivatedRoute,
    private readonly destroyRef: DestroyRef,
    private readonly matDialog: MatDialog,
    private readonly router: Router,
  ) {}

  ngOnInit() {
    this.isLoading = true;
    this.rulesetV2Service
      .getRuleset(this.activatedRoute.snapshot.params.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.form.patchValue({
            name: res.name,
            description: res.description,
          });
          this.filePreview = res.payload;
          this.isLoading = false;
        },
        error: () => {
          this.snackBarService.error('Ruleset error');
        },
      });
  }

  public edit() {
    this.isLoading = true;
    const data: EditRulesetRequestData = {
      name: this.form.value.name,
      description: this.form.value.description,
    };

    this.rulesetV2Service
      .editRuleset(this.activatedRoute.snapshot.params.id, data)
      .pipe(switchMap(() => this.rulesetV2Service.getRuleset(this.activatedRoute.snapshot.params.id)))
      .subscribe({
        next: (res) => {
          this.form.patchValue({
            name: res.name,
            description: res.description,
          });
          this.isLoading = false;
          this.snackBarService.success('Ruleset updated.');
          this.form.markAsPristine();
        },
        error: () => {
          this.snackBarService.error('Ruleset update error!');
        },
      });
  }

  public delete() {
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
          return this.rulesetV2Service.deleteRuleset(this.activatedRoute.snapshot.params.id);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.isLoading = false;
          this.router.navigate(['../..'], {
            relativeTo: this.activatedRoute,
          });
          this.snackBarService.success('Ruleset successfully deleted!');
        },
        error: () => {
          this.snackBarService.error('Delete ruleset error!');
        },
      });
  }
}
