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

import { Component, computed, DestroyRef, effect, inject, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { GioBannerModule, GIO_DIALOG_WIDTH, GioIconsModule } from '@gravitee/ui-particles-angular';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import {
  RedactionRuleDialogComponent,
  RedactionRuleDialogData,
  RedactionRuleDialogResult,
} from './redaction-rule-dialog/redaction-rule-dialog.component';
import { RedactionMaskingDisplayPipe } from './redaction-masking-display.pipe';

import { ApiV4, RedactionRule } from '../../../../../entities/management-api-v2';

type RedactionRuleRow = RedactionRule & {
  _id: string;
};

function buildRow(rule: RedactionRule, id?: string): RedactionRuleRow {
  return { ...rule, _id: id ?? crypto.randomUUID() };
}

function stripDisplayFields({ _id, ...rule }: RedactionRuleRow): RedactionRule {
  return rule;
}

@Component({
  selector: 'api-redaction-rules',
  templateUrl: './api-redaction-rules.component.html',
  styleUrls: ['./api-redaction-rules.component.scss'],
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatTooltipModule,
    GioBannerModule,
    GioIconsModule,
    RedactionMaskingDisplayPipe,
  ],
})
export class ApiRedactionRulesComponent {
  private readonly matDialog = inject(MatDialog);
  private readonly destroyRef = inject(DestroyRef);

  api = input.required<ApiV4>();
  resetTrigger = input(0);

  rulesChange = output<RedactionRule[]>();

  protected readonly displayedColumns = ['index', 'pattern', 'masking', 'valueFilter', 'actions'];
  protected rows = signal<RedactionRuleRow[]>([]);
  protected isReadOnly = computed(() => this.api().definitionContext?.origin === 'KUBERNETES');

  private readonly resetEffect = effect(() => {
    this.resetTrigger();
    this.resetToApiState();
  });

  protected addRule(): void {
    this.matDialog
      .open<RedactionRuleDialogComponent, RedactionRuleDialogData, RedactionRuleDialogResult>(RedactionRuleDialogComponent, {
        width: GIO_DIALOG_WIDTH.MEDIUM,
        data: { rule: null, existingPatterns: this.rows().map(r => r.attributeNamePattern) },
      })
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(result => {
        if (result) {
          this.rows.update(rows => [...rows, buildRow(result)]);
          this.emitRulesChange();
        }
      });
  }

  protected editRule(id: string): void {
    const row = this.rows().find(r => r._id === id);
    if (!row) return;
    this.matDialog
      .open<RedactionRuleDialogComponent, RedactionRuleDialogData, RedactionRuleDialogResult>(RedactionRuleDialogComponent, {
        width: GIO_DIALOG_WIDTH.MEDIUM,
        data: {
          rule: row,
          existingPatterns: this.rows()
            .filter(r => r._id !== id)
            .map(r => r.attributeNamePattern),
        },
      })
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(result => {
        if (result) {
          this.rows.update(rows => rows.map(r => (r._id === id ? buildRow(result, id) : r)));
          this.emitRulesChange();
        }
      });
  }

  protected removeRule(id: string): void {
    this.rows.update(rows => rows.filter(r => r._id !== id));
    this.emitRulesChange();
  }

  private emitRulesChange(): void {
    this.rulesChange.emit(this.rows().map(stripDisplayFields));
  }

  private resetToApiState(): void {
    const existing = this.api().analytics?.tracing?.redaction?.rules ?? [];
    this.rows.set(existing.map(rule => buildRow(rule)));
  }
}
