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
import { Component, forwardRef, Input, OnDestroy } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { catchError, switchMap, takeUntil } from 'rxjs/operators';
import { of, Subject } from 'rxjs';
import { MatDialog } from '@angular/material/dialog';
import { isEmpty } from 'lodash';

import { GioInformationDialogComponent, GioConnectorDialogData } from '../gio-information-dialog/gio-information-dialog.component';
import { ConnectorPluginsV2Service } from '../../../../services-ngx/connector-plugins-v2.service';
import { ConnectorVM } from '../../../../entities/management-api-v2';

@Component({
  selector: 'gio-entrypoints-selection-list',
  templateUrl: './gio-entrypoints-selection-list.component.html',
  styleUrls: ['./gio-entrypoints-selection-list.component.scss'],
  standalone: false,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => GioEntrypointsSelectionListComponent),
      multi: true,
    },
  ],
})
export class GioEntrypointsSelectionListComponent implements OnDestroy, ControlValueAccessor {
  private unsubscribe$: Subject<void> = new Subject<void>();
  @Input()
  public entrypoints: ConnectorVM[];
  @Input()
  public singleChoice = false;

  public _entrypointSelection: string[] | string;

  get entrypointSelection() {
    return this._entrypointSelection;
  }
  set entrypointSelection(selection: string[] | string) {
    const selectionAsArray = Array.isArray(selection) ? selection : [selection];
    // If single choice (radio button), then return the first element, else the whole array
    this._entrypointSelection = this.singleChoice ? selectionAsArray[0] : selectionAsArray;
    this._onTouched();
    // Always use selection as an array for consistency
    this._onChange(selectionAsArray);
  }
  protected _onChange: (_selection: string[] | null) => void = () => ({});

  protected _onTouched: () => void = () => ({});

  constructor(
    private readonly connectorPluginsV2Service: ConnectorPluginsV2Service,
    private readonly matDialog: MatDialog,
  ) {}
  onMoreInfoClick(event, entrypoint: ConnectorVM) {
    event.stopPropagation();

    this.connectorPluginsV2Service
      .getEntrypointPluginMoreInformation(entrypoint.id)
      .pipe(
        catchError(() => of({})),
        switchMap((pluginMoreInformation) =>
          this.matDialog
            .open<GioInformationDialogComponent, GioConnectorDialogData, boolean>(GioInformationDialogComponent, {
              data: {
                name: entrypoint.name,
                information: pluginMoreInformation,
              },
              role: 'alertdialog',
              id: 'moreInfoDialog',
            })
            .afterClosed(),
        ),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  // From ControlValueAccessor interface
  public writeValue(selection: string[] | string | null): void {
    if (!selection || isEmpty(selection)) {
      return;
    }

    this.entrypointSelection = selection;
  }

  // From ControlValueAccessor interface
  public registerOnChange(fn: (selection: string[] | null) => void): void {
    this._onChange = fn;
  }

  // From ControlValueAccessor interface
  public registerOnTouched(fn: () => void): void {
    this._onTouched = fn;
  }
}
