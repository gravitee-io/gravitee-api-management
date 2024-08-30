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
import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { MatButton, MatIconButton } from '@angular/material/button';
import { MatDialog, MatDialogActions, MatDialogClose, MatDialogContent } from '@angular/material/dialog';
import { MatIcon } from '@angular/material/icon';
import { Subject } from 'rxjs';
import { ActivatedRoute, Router } from '@angular/router';
import { filter, switchMap, takeUntil, tap } from 'rxjs/operators';
import { MatCard, MatCardContent } from '@angular/material/card';
import { GioBannerModule, GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';

import { ApiDocumentationChoosePageListComponent } from '../api-documentation-choose-page-list/api-documentation-choose-page-list.component';
import { Api, Page } from '../../../../../entities/management-api-v2';
import { ApiV2Service } from '../../../../../services-ngx/api-v2.service';
import { ApiDocumentationV2Service } from '../../../../../services-ngx/api-documentation-v2.service';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';

@Component({
  selector: 'api-documentation-choose-existing-page',
  standalone: true,
  imports: [
    ApiDocumentationChoosePageListComponent,
    MatButton,
    MatDialogActions,
    MatDialogClose,
    MatDialogContent,
    MatIcon,
    MatIconButton,
    MatCard,
    GioBannerModule,
    MatCardContent,
  ],
  templateUrl: './api-documentation-choose-existing-page.component.html',
  styleUrl: './api-documentation-choose-existing-page.component.scss',
})
export class ApiDocumentationChooseExistingPageComponent implements OnInit {
  api: Api;
  pages: Page[];
  isLoading = false;
  selectedPageId: string;
  isReadOnly: boolean;
  private unsubscribe$: Subject<void> = new Subject<void>();

  @Output() pageSelected = new EventEmitter<string>();
  @Output() cancelled = new EventEmitter<void>();

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly router: Router,
    private readonly apiV2Service: ApiV2Service,
    private readonly apiDocumentationV2Service: ApiDocumentationV2Service,
    private readonly snackBarService: SnackBarService,
    private readonly matDialog: MatDialog,
  ) {}

  ngOnInit() {
    this.isLoading = true;

    this.apiV2Service
      .get(this.activatedRoute.snapshot.params.apiId)
      .pipe(
        tap((api) => {
          this.api = api;
          this.isReadOnly = this.api.originContext?.origin === 'KUBERNETES';
        }),
        switchMap(() => this.apiDocumentationV2Service.getApiPages(this.api.id)),
        takeUntil(this.unsubscribe$),
      )
      .subscribe((res) => {
        this.pages = res.pages.filter((p) => p.generalConditions !== true && p.homepage !== true);
        this.isLoading = false;
      });
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  saveWithConfirmation() {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
        data: {
          title: 'Confirm Homepage Selection',
          content: 'This choice is final. Once a page becomes the homepage, it will no longer be accessible in your Documentation Pages.',
          confirmButton: 'Confirm',
        },
      })
      .afterClosed()
      .pipe(
        filter((confirmed) => !!confirmed),
        switchMap(() => this.apiDocumentationV2Service.getApiPage(this.api.id, this.selectedPageId)),
        switchMap((chosenPage: Page) =>
          this.apiDocumentationV2Service.updateDocumentationPage(this.api.id, chosenPage.id, {
            ...chosenPage,
            homepage: true,
          }),
        ),
      )
      .subscribe({
        next: () => {
          this.snackBarService.success('Homepage chosen successfully');
          this.goBackToDefaultPage();
        },
        error: (error) => {
          this.snackBarService.error(error?.error?.message ?? 'An error occurred when choosing a homepage');
        },
      });
  }

  cancel() {
    this.cancelled.emit();
  }

  selectPage(pageId: string) {
    this.selectedPageId = pageId;
  }

  goBackToDefaultPage() {
    this.router.navigate(['../../'], {
      relativeTo: this.activatedRoute,
    });
  }
}
