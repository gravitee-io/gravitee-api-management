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
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { MatDialog } from '@angular/material/dialog';
import { GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';

import { Constants } from '../../../entities/Constants';

@Component({
  selector: 'api-product-create',
  templateUrl: './api-product-create.component.html',
  styleUrls: ['./api-product-create.component.scss'],
  standalone: false,
})
export class ApiProductCreateComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<void> = new Subject<void>();

  public form: UntypedFormGroup;
  public descriptionMaxLength = 250;

  constructor(
    @Inject(Constants) private readonly constants: Constants,
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
    private readonly formBuilder: UntypedFormBuilder,
    private readonly matDialog: MatDialog,
  ) {}

  ngOnInit(): void {
    this.form = this.formBuilder.group({
      name: this.formBuilder.control('', [Validators.required]),
      version: this.formBuilder.control('', [Validators.required]),
      description: this.formBuilder.control('', [Validators.maxLength(this.descriptionMaxLength)]),
    });
  }

  ngOnDestroy(): void {
    this.unsubscribe$.next();
    this.unsubscribe$.complete();
  }

  onExit(): void {
    if (this.form.dirty) {
      this.matDialog
        .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
          data: {
            title: 'Exit without saving?',
            content: 'You have unsaved changes. Are you sure you want to exit without saving?',
            confirmButton: 'Exit without saving',
            cancelButton: 'Cancel',
          },
        })
        .afterClosed()
        .pipe(takeUntil(this.unsubscribe$))
        .subscribe((confirmed) => {
          if (confirmed) {
            this.router.navigate(['..'], { relativeTo: this.activatedRoute });
          }
        });
      return;
    }
    this.router.navigate(['..'], { relativeTo: this.activatedRoute });
  }

  onBack(): void {
    this.router.navigate(['..'], { relativeTo: this.activatedRoute });
  }

  onCreate(): void {
    if (this.form.valid) {
      // TODO: Create API Product
      console.log('Creating API Product:', this.form.value);
      // Navigate back to the API Products list page
      this.router.navigate(['..'], { relativeTo: this.activatedRoute });
    } else {
      this.form.markAllAsTouched();
    }
  }

  getDescriptionLength(): number {
    return this.form.get('description')?.value?.length || 0;
  }
}

