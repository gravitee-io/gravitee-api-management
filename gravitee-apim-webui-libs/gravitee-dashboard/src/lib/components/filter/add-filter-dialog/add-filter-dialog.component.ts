/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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
import { AsyncPipe } from '@angular/common';
import {
  afterNextRender,
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  inject,
  Injector,
  OnInit,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatAutocomplete, MatAutocompleteSelectedEvent, MatAutocompleteTrigger, MatOption } from '@angular/material/autocomplete';
import { MatButton } from '@angular/material/button';
import { MatCheckbox, MatCheckboxChange } from '@angular/material/checkbox';
import {
  MAT_DIALOG_DATA,
  MatDialogActions,
  MatDialogClose,
  MatDialogContent,
  MatDialogRef,
  MatDialogTitle,
} from '@angular/material/dialog';
import { MatFormField, MatLabel } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { MatSelect } from '@angular/material/select';
import { MatTooltip } from '@angular/material/tooltip';
import { defer, Observable, of } from 'rxjs';
import { map, startWith } from 'rxjs/operators';

import { FILTER_DEFINITION_PROVIDER } from '../filter-providers';
import {
  FilterCondition,
  FilterDefinition,
  FilterOperator,
  FilterValueSelection,
  normalizeFilterValueSelection,
  normalizeMembershipOperatorForValues,
  OPERATOR_SYMBOLS,
} from '../filter.model';
import { EnumValueInputComponent } from './value-inputs/enum-value-input.component';
import { KeywordValueInputComponent } from './value-inputs/keyword-value-input.component';
import { NumberValueInputComponent } from './value-inputs/number-value-input.component';
import { StringValueInputComponent } from './value-inputs/string-value-input.component';

export interface AddFilterDialogData {
  existingCondition?: FilterCondition;
  timeFrom?: number;
  timeTo?: number;
}

const KNOWN_OPERATORS: ReadonlySet<string> = new Set<FilterOperator>(['EQ', 'NEQ', 'CONTAINS', 'IN', 'NOT_IN', 'LTE', 'GTE']);

/** Human-readable labels for `FilterDefinition.apiTypes` in the "Filter by" list; tokens not listed stay as-is (e.g. LLM, MCP). */
const ADD_FILTER_API_TYPE_DISPLAY_LABELS: Readonly<Record<string, string>> = {
  HTTP_PROXY: 'HTTP Proxy',
  MESSAGE: 'Message',
};

/** Canonical map key: spaces → underscores, trim, uppercase (matches `HTTP_PROXY`, `HTTP PROXY`, `http_proxy`). */
function normalizeApiTypeTokenForLookup(token: string): string {
  return token.trim().replace(/\s+/g, '_').toUpperCase();
}

@Component({
  selector: 'gd-add-filter-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AsyncPipe,
    ReactiveFormsModule,
    MatAutocomplete,
    MatAutocompleteTrigger,
    MatOption,
    MatSelect,
    MatButton,
    MatCheckbox,
    MatTooltip,
    MatDialogTitle,
    MatDialogContent,
    MatDialogActions,
    MatDialogClose,
    MatFormField,
    MatLabel,
    MatInput,
    EnumValueInputComponent,
    KeywordValueInputComponent,
    NumberValueInputComponent,
    StringValueInputComponent,
  ],
  templateUrl: './add-filter-dialog.component.html',
  styleUrl: './add-filter-dialog.component.scss',
})
export class AddFilterDialogComponent implements OnInit, AfterViewInit {
  private readonly dialogRef = inject(MatDialogRef<AddFilterDialogComponent, FilterCondition>);
  protected readonly data = inject<AddFilterDialogData>(MAT_DIALOG_DATA);
  private readonly definitionProvider = inject(FILTER_DEFINITION_PROVIDER);
  private readonly destroyRef = inject(DestroyRef);
  private readonly injector = inject(Injector);

  private readonly fieldTrigger = viewChild<MatAutocompleteTrigger>('fieldTrigger');
  private readonly fieldAutocomplete = viewChild<MatAutocomplete>('fieldAuto');

  protected readonly fieldControl = new FormControl<FilterDefinition | string | null>(null);
  protected readonly operatorControl = new FormControl<string | null>(null);

  /** When both dashboard bounds exist, default true so value suggestions stay scoped (previous behavior). */
  protected readonly limitKeywordValuesToTimeframe = signal(this.data.timeFrom != null && this.data.timeTo != null);

  protected definitions = signal<FilterDefinition[]>([]);
  protected selectedDefinition = signal<FilterDefinition | null>(null);
  protected selectedOperator = signal<string | null>(null);
  protected selectedValues = signal<string[]>([]);
  /** Parallel to selectedValues — display names for KEYWORD filters (ids stay in selectedValues). */
  protected selectedValueLabels = signal<string[]>([]);

  protected filteredDefinitions$: Observable<FilterDefinition[]> = of([]);

  protected isEditMode = computed(() => this.data.existingCondition != null);

  protected availableOperators = computed(() => {
    const def = this.selectedDefinition();
    if (!def) return [];
    return def.operators.filter(op => {
      if (!KNOWN_OPERATORS.has(op)) {
        console.warn(`[gd-add-filter-dialog] Ignoring unknown operator '${op}' for filter '${def.name}'`);
        return false;
      }
      return true;
    });
  });

  protected resolvedFilterType = computed(() => {
    const def = this.selectedDefinition();
    if (!def) return null;
    const type = def.type;
    switch (type) {
      case 'ENUM':
      case 'KEYWORD':
      case 'NUMBER':
      case 'STRING':
        return type;
      default:
        console.warn(`[gd-add-filter-dialog] Unsupported filter type '${type}' for filter '${def.name}', falling back to STRING`);
        return 'STRING';
    }
  });

  protected canConfirm = computed(() => {
    return this.selectedDefinition() != null && this.selectedOperator() != null && this.selectedValues().length > 0;
  });

  protected readonly hasKeywordTimeframeBounds = computed(() => this.data.timeFrom != null && this.data.timeTo != null);

  /** Effective range passed to KEYWORD value search; omitted when user disables timeframe limiting. */
  protected readonly keywordValuesTimeFrom = computed(() => {
    if (this.selectedDefinition()?.type !== 'KEYWORD' || !this.hasKeywordTimeframeBounds() || !this.limitKeywordValuesToTimeframe()) {
      return undefined;
    }
    return this.data.timeFrom;
  });

  protected readonly keywordValuesTimeTo = computed(() => {
    if (this.selectedDefinition()?.type !== 'KEYWORD' || !this.hasKeywordTimeframeBounds() || !this.limitKeywordValuesToTimeframe()) {
      return undefined;
    }
    return this.data.timeTo;
  });

  ngOnInit(): void {
    this.definitionProvider.getDefinitions().subscribe(defs => {
      const sorted = this.sortDefinitionsByLabel(defs);
      this.definitions.set(sorted);
      this.setupFieldAutocomplete();
      this.setupOperatorSelect();
      this.restoreExistingCondition(sorted);
    });
  }

  ngAfterViewInit(): void {
    // Default dialog focus can still target the first input in some cases; ensure the
    // "Filter by" autocomplete stays closed until the user opens it explicitly.
    const close = (): void => this.fieldTrigger()?.closePanel();
    Promise.resolve().then(close);
    afterNextRender(close, { injector: this.injector });
    requestAnimationFrame(close);
  }

  protected onLimitKeywordValuesToTimeframeChange(event: MatCheckboxChange): void {
    this.limitKeywordValuesToTimeframe.set(event.checked);
  }

  protected selectDefinition(def: FilterDefinition): void {
    this.selectedDefinition.set(def);
    this.selectedValues.set([]);
    this.selectedValueLabels.set([]);
    // Reset operator control so mat-select reflects the new definition's operators.
    this.operatorControl.setValue(null);
    this.selectedOperator.set(null);

    const operators = def.operators.filter(op => KNOWN_OPERATORS.has(op));
    if (operators.length === 1) {
      this.operatorControl.setValue(operators[0]);
      this.selectedOperator.set(operators[0]);
    }
  }

  protected onFieldSelected(event: MatAutocompleteSelectedEvent): void {
    const value = event.option.value;
    if (this.isFilterDefinition(value)) {
      this.selectDefinition(value);
    }
  }

  protected onValuesChange(payload: string[] | FilterValueSelection): void {
    const { values, valueLabels } = normalizeFilterValueSelection(payload);
    this.selectedValues.set(values);
    this.selectedValueLabels.set(valueLabels);
    this.syncOperatorToValueCount();
  }

  protected displayDefinition = (value: FilterDefinition | string | null): string => {
    if (!value) return '';
    if (this.isFilterDefinition(value)) return value.label;
    return String(value);
  };

  protected isSignalExclusive(def: FilterDefinition): string | null {
    if (!def.signals || def.signals.length !== 1) return null;
    const raw = def.signals[0];
    return raw.charAt(0).toUpperCase() + raw.slice(1).toLowerCase();
  }

  /** Badge text for an API/engine type token from filter definitions. */
  protected formatApiTypeForDisplay(apiType: string): string {
    const key = normalizeApiTypeTokenForLookup(apiType);
    const label = ADD_FILTER_API_TYPE_DISPLAY_LABELS[key];
    return label ?? apiType.trim();
  }

  /** Human-readable operator text only (no raw token suffix like "in IN"). */
  protected operatorLabel(op: string): string {
    switch (op) {
      case 'EQ':
        return '=';
      case 'NEQ':
        return '≠';
      case 'CONTAINS':
        return 'Contains';
      case 'IN':
        return 'In';
      case 'NOT_IN':
        return 'Not in';
      case 'GTE':
        return '≥';
      case 'LTE':
        return '≤';
      default:
        return OPERATOR_SYMBOLS[op] ?? op;
    }
  }

  protected onFieldInputClick(): void {
    // Clicking a field that already has a selection clears the value so the user
    // can type to search again. We keep this on `click` (not `focus`) because the
    // autocomplete returns focus to the input after a selection, which would
    // otherwise wipe the value we just selected.
    if (this.isFilterDefinition(this.fieldControl.value)) {
      this.fieldControl.setValue('');
    }
  }

  protected confirm(): void {
    const def = this.selectedDefinition();
    const op = this.selectedOperator();
    const vals = this.selectedValues();
    if (!def || !op || vals.length === 0) return;

    const operator = normalizeMembershipOperatorForValues(def, op, vals.length);
    const lbls = this.selectedValueLabels();
    const valueLabels = lbls.length === vals.length ? lbls : vals.map((v, i) => lbls[i] ?? v);
    const condition: FilterCondition = {
      field: def.name,
      label: def.label,
      operator,
      values: vals,
      valueLabels,
    };
    this.dialogRef.close(condition);
  }

  /** Material shrink-wraps option rows; set a concrete inner width like a manual fixed `width` in DevTools. */
  protected onFieldAutocompletePanelOpened(): void {
    afterNextRender(
      () => {
        this.syncFieldAutocompletePanelInnerWidth();
        // Scrollbar appears after layout; measure again so width matches the visible track (no clipped badges).
        requestAnimationFrame(() => this.syncFieldAutocompletePanelInnerWidth());
      },
      { injector: this.injector },
    );
  }

  protected onFieldAutocompletePanelClosed(): void {
    this.fieldAutocomplete()?.panel?.nativeElement?.style.removeProperty('--gd-add-filter-field-panel-inner-px');
  }

  private syncFieldAutocompletePanelInnerWidth(): void {
    if (!this.fieldTrigger()?.panelOpen) return;
    const panel = this.fieldAutocomplete()?.panel?.nativeElement;
    if (!panel) return;
    const w = this.measureFieldAutocompleteOptionContentWidth(panel);
    panel.style.setProperty('--gd-add-filter-field-panel-inner-px', `${w}px`);
  }

  /**
   * Row must fit inside `mat-option`’s **content** box (padding excluded), not the full panel width,
   * or badges sit under the scrollbar / past the clip edge.
   */
  private measureFieldAutocompleteOptionContentWidth(panel: HTMLElement): number {
    const opt = panel.querySelector<HTMLElement>('mat-option.gd-add-filter__field-option');
    if (!opt) {
      return Math.max(0, Math.round(panel.clientWidth));
    }
    const cs = getComputedStyle(opt);
    const padX = (parseFloat(cs.paddingLeft) || 0) + (parseFloat(cs.paddingRight) || 0);
    const fromOption = Math.max(0, Math.round(opt.clientWidth - padX));
    // Options can span the full scroll width (under the vertical scrollbar); cap to the visible track.
    const fromPanel = Math.max(0, Math.round(panel.clientWidth));
    return Math.min(fromOption, fromPanel);
  }

  /** Client-side filter list for `mat-autocomplete` (Material v20 Autocomplete pattern). */
  private setupFieldAutocomplete(): void {
    this.filteredDefinitions$ = defer(() =>
      this.fieldControl.valueChanges.pipe(
        startWith(this.fieldControl.value),
        map(value => this.filterDefinitions(value)),
      ),
    );

    this.fieldControl.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(value => {
      // User cleared or is typing — reset selection if the current selected def no longer matches
      if (value == null || typeof value === 'string') {
        const current = this.selectedDefinition();
        if (current && current.label !== value) {
          this.selectedDefinition.set(null);
          this.selectedOperator.set(null);
          this.selectedValues.set([]);
          this.selectedValueLabels.set([]);
          this.operatorControl.setValue(null);
        }
      }
      if (this.fieldTrigger()?.panelOpen) {
        afterNextRender(() => this.syncFieldAutocompletePanelInnerWidth(), { injector: this.injector });
      }
    });
  }

  private setupOperatorSelect(): void {
    this.operatorControl.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(value => {
      if (value == null || value === '') {
        this.selectedOperator.set(null);
      } else {
        this.selectedOperator.set(value);
        this.syncOperatorToValueCount();
      }
    });
  }

  private filterDefinitions(value: FilterDefinition | string | null): FilterDefinition[] {
    const all = this.definitions();
    if (!value || this.isFilterDefinition(value)) return all;
    const term = value.toLowerCase();
    if (!term) return all;
    return this.sortDefinitionsByLabel(
      all.filter(
        d =>
          d.label.toLowerCase().includes(term) ||
          d.name.toLowerCase().includes(term) ||
          (d.apiTypes ?? []).some(t => t.toLowerCase().includes(term)),
      ),
    );
  }

  /** Stable alphabetical order for "Filter by" (label first, then technical name). */
  private sortDefinitionsByLabel(defs: ReadonlyArray<FilterDefinition>): FilterDefinition[] {
    return [...defs].sort((a, b) => {
      const byLabel = a.label.localeCompare(b.label, undefined, { sensitivity: 'base' });
      if (byLabel !== 0) {
        return byLabel;
      }
      return a.name.localeCompare(b.name, undefined, { sensitivity: 'base' });
    });
  }

  private isFilterDefinition(value: unknown): value is FilterDefinition {
    return value != null && typeof value === 'object' && 'name' in value && 'label' in value && 'type' in value && 'operators' in value;
  }

  private restoreExistingCondition(defs: FilterDefinition[]): void {
    const existing = this.data.existingCondition;
    if (!existing) return;

    const match = defs.find(d => d.name === existing.field);
    if (match) {
      this.selectedDefinition.set(match);
      this.fieldControl.setValue(match, { emitEvent: false });
      this.selectedOperator.set(existing.operator);
      this.operatorControl.setValue(existing.operator, { emitEvent: false });
      this.selectedValues.set([...existing.values]);
      this.selectedValueLabels.set(
        existing.valueLabels != null && existing.valueLabels.length === existing.values.length
          ? [...existing.valueLabels]
          : [...existing.values],
      );
      this.syncOperatorToValueCount();
    }
  }

  private syncOperatorToValueCount(): void {
    const def = this.selectedDefinition();
    const op = this.selectedOperator();
    if (!def || !op) return;
    const n = this.selectedValues().length;
    if (n === 0) return;
    const next = normalizeMembershipOperatorForValues(def, op, n);
    if (next !== op) {
      this.selectedOperator.set(next);
      this.operatorControl.setValue(next, { emitEvent: false });
    }
  }
}
