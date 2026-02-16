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
import { GraviteeMarkdownEditorModule } from '@gravitee/gravitee-markdown';

import { Component, computed, DestroyRef, effect, HostListener, inject, signal, WritableSignal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { catchError, filter, startWith, switchMap, tap } from 'rxjs/operators';
import { EMPTY, Observable, of } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog } from '@angular/material/dialog';
import { GIO_DIALOG_WIDTH, GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';

import { PortalHeaderComponent } from '../components/header/portal-header.component';
import { GioPermissionService } from '../../shared/components/gio-permission/gio-permission.service';
import { PortalNavigationItemService } from '../../services-ngx/portal-navigation-item.service';
import { PortalPageContentService } from '../../services-ngx/portal-page-content.service';
import { SnackBarService } from '../../services-ngx/snack-bar.service';
import { PortalNavigationPage, PortalPageContent } from '../../entities/management-api-v2';
import { HasUnsavedChanges } from '../../shared/guards/has-unsaved-changes.guard';
import { confirmDiscardChanges, normalizeContent } from '../../shared/utils/content.util';

export interface PortalHomepage {
  navigationItem: PortalNavigationPage;
  content: PortalPageContent;
}

@Component({
  selector: 'homepage',
  imports: [PortalHeaderComponent, ReactiveFormsModule, GraviteeMarkdownEditorModule, MatButtonModule, MatTooltipModule],
  templateUrl: './homepage.component.html',
  styleUrl: './homepage.component.scss',
})
export class HomepageComponent implements HasUnsavedChanges {
  contentControl = new FormControl({
    value: '',
    disabled: true,
  });

  togglePublishActionText = computed(() => {
    const published = this.portalHomepagePublished();
    return published ? 'Unpublish' : 'Publish';
  });

  isSaveDisabled = computed(() => {
    const currentContent = this.contentValue();
    const savedContent = this.portalHomepage()?.content?.content;
    const isEmpty = !currentContent?.trim();
    return !this.canUpdate() || isEmpty || currentContent === savedContent;
  });

  isTogglePublishActive = computed(() => {
    return this.canUpdate() && this.portalHomepage();
  });

  private readonly snackbarService = inject(SnackBarService);
  private readonly portalNavigationItemService = inject(PortalNavigationItemService);
  private readonly portalPageContentService = inject(PortalPageContentService);
  private readonly gioPermissionService = inject(GioPermissionService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly matDialog = inject(MatDialog);

  readonly portalHomepage: WritableSignal<PortalHomepage | null> = signal(null);
  readonly portalHomepagePublished = computed(() => this.portalHomepage()?.navigationItem?.published ?? false);
  private readonly canUpdate = signal(this.gioPermissionService.hasAnyMatching(['environment-documentation-u']));
  private readonly contentValue = toSignal(this.contentControl.valueChanges.pipe(startWith(this.contentControl.value)));

  @HostListener('window:beforeunload', ['$event'])
  beforeUnloadHandler(event: BeforeUnloadEvent) {
    if (this.hasUnsavedChanges()) {
      event.preventDefault();
      event.returnValue = '';
      return '';
    }
  }

  hasUnsavedChanges() {
    const currentValue = normalizeContent(this.contentControl.value);
    const initialValue = normalizeContent(this.portalHomepage()?.content?.content);
    return currentValue !== initialValue;
  }

  constructor() {
    this.getPortalHomepage()
      .pipe(
        tap(result => {
          this.portalHomepage.set(result);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();

    effect(() => {
      this.contentControl.reset(this.portalHomepage()?.content?.content || '');
    });

    effect(() => {
      this.canUpdate() ? this.contentControl.enable() : this.contentControl.disable();
    });
  }

  public togglePublish(): void {
    if (this.hasUnsavedChanges()) {
      confirmDiscardChanges(this.matDialog)
        .pipe(
          filter(confirmed => confirmed),
          tap(() => this.executeTogglePublish()),
          takeUntilDestroyed(this.destroyRef),
        )
        .subscribe();
    } else {
      this.executeTogglePublish();
    }
  }

  private executeTogglePublish(): void {
    const isCurrentlyPublished = this.portalHomepagePublished();
    const nav = this.portalHomepage().navigationItem as PortalNavigationPage;
    const pageId = nav.id;

    const updateObj = { ...nav, published: !isCurrentlyPublished };

    const data: GioConfirmDialogData = {
      title: `${this.togglePublishActionText()} page?`,
      content: isCurrentlyPublished
        ? `This action will unpublish the page. It will no longer be visible on the developer portal, but you can publish it again at any time.`
        : `Your changes will be published. The updated page will be visible on your developer portal.`,
      confirmButton: this.togglePublishActionText(),
    };
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
        width: GIO_DIALOG_WIDTH.SMALL,
        data,
        role: 'alertdialog',
        id: 'confirmDialog',
      })
      .afterClosed()
      .pipe(
        filter(confirmed => confirmed),
        switchMap(() => this.portalNavigationItemService.updateNavigationItem(pageId, updateObj)),
        tap(updatedPage => {
          const currentNav = this.portalHomepage()?.navigationItem ?? null;
          this.portalHomepage.update(current => {
            return {
              ...current,
              navigationItem: { ...currentNav, published: updatedPage.published },
            };
          });
          this.snackbarService.success(`Page has been ${updatedPage.published ? 'publish' : 'unpublish'}ed successfully.`);
        }),
        takeUntilDestroyed(this.destroyRef),
        catchError(({ error }) => {
          this.snackbarService.error(error?.message ?? `Failed to ${this.togglePublishActionText().toLowerCase()} page.`);
          return EMPTY;
        }),
      )
      .subscribe();
  }

  updatePortalPage(): void {
    const navForUpdate = this.portalHomepage()?.navigationItem ?? null;
    const pageId =
      navForUpdate?.type === 'PAGE'
        ? ((navForUpdate as PortalNavigationPage).portalPageContentId ?? navForUpdate.id)
        : (navForUpdate?.id ?? '');
    this.portalPageContentService
      .updatePageContent(pageId, {
        content: this.contentControl.value,
      })
      .pipe(
        tap(portalPage => {
          this.snackbarService.success(`The page has been updated successfully`);
          this.portalHomepage.update(current => {
            return {
              ...current,
              content: { id: portalPage.id, type: portalPage.type, content: portalPage.content },
            };
          });
        }),
        catchError(() => {
          this.snackbarService.error('An error occurred while updating the homepage');
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  private getPortalHomepage(): Observable<PortalHomepage> {
    return this.portalNavigationItemService.getNavigationItems('HOMEPAGE').pipe(
      switchMap(navResponse => {
        const items = navResponse?.items ?? [];
        if (items.length === 0) {
          return EMPTY;
        }
        const firstPageItem = items.find(i => i.type === 'PAGE');
        if (!firstPageItem) {
          return EMPTY;
        }
        const portalPage = firstPageItem as PortalNavigationPage;
        const portalPageContentId = portalPage.portalPageContentId;
        if (!portalPageContentId) {
          return EMPTY;
        }
        return this.portalPageContentService.getPageContent(portalPageContentId).pipe(
          switchMap(content => {
            if (!content) {
              return EMPTY;
            }
            return of({ navigationItem: portalPage, content } as PortalHomepage);
          }),
        );
      }),
      catchError(() => {
        this.snackbarService.error('An error occurred while loading the homepage');
        return EMPTY;
      }),
    );
  }
}
