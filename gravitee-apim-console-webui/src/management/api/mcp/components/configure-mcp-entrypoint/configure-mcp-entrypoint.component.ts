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
import { Component, DestroyRef, forwardRef, inject, OnInit } from '@angular/core';
import { ControlValueAccessor, FormControl, FormGroup, NG_VALUE_ACCESSOR, ReactiveFormsModule } from '@angular/forms';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { filter, map, tap } from 'rxjs/operators';
import { MatButtonModule } from '@angular/material/button';
import { GIO_DIALOG_WIDTH, GioIconsModule } from '@gravitee/ui-particles-angular';
import { MatDialog } from '@angular/material/dialog';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { DEFAULT_MCP_ENTRYPOINT_PATH, MCPConfiguration, MCPTool, MCPToolDefinition } from '../../../../../entities/entrypoint/mcp';
import { ToolDisplayComponent } from '../tool-display/tool-display.component';
import {
  ImportMcpToolsDialogComponent,
  ImportMcpToolsDialogData,
  ImportMcpToolsDialogResult,
} from '../import-mcp-tools-dialog/import-mcp-tools-dialog.component';
import { GioPermissionModule } from '../../../../../shared/components/gio-permission/gio-permission.module';
import { GioPermissionService } from '../../../../../shared/components/gio-permission/gio-permission.service';

interface ConfigurationMCPForm {
  tools: FormControl<MCPTool[]>;
  mcpPath: FormControl<string>;
}

@Component({
  selector: 'configure-mcp-entrypoint',
  imports: [
    ReactiveFormsModule,
    MatInputModule,
    MatFormFieldModule,
    MatButtonModule,
    GioIconsModule,
    ToolDisplayComponent,
    GioPermissionModule,
  ],
  templateUrl: './configure-mcp-entrypoint.component.html',
  styleUrl: './configure-mcp-entrypoint.component.scss',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ConfigureMcpEntrypointComponent),
      multi: true,
    },
  ],
})
export class ConfigureMcpEntrypointComponent implements OnInit, ControlValueAccessor {
  private onChange: (value: MCPConfiguration) => void = () => {};
  private onTouched: () => void = () => {};

  formGroup = new FormGroup<ConfigurationMCPForm>({
    tools: new FormControl<MCPTool[]>([]),
    mcpPath: new FormControl<string>(DEFAULT_MCP_ENTRYPOINT_PATH),
  });

  toolDefinitions: MCPToolDefinition[] = [];

  private destroyRef = inject(DestroyRef);

  constructor(
    private readonly matDialog: MatDialog,
    private readonly gioPermissionService: GioPermissionService,
  ) {}

  ngOnInit(): void {
    // Subscribe to form changes to emit values
    this.formGroup.valueChanges
      .pipe(
        tap(value => {
          this.onChange({
            tools: value.tools || [],
            mcpPath: value.mcpPath || DEFAULT_MCP_ENTRYPOINT_PATH,
          });
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();

    if (!this.gioPermissionService.hasAnyMatching(['api-definition-u'])) {
      this.formGroup.disable();
    }
  }

  // ControlValueAccessor implementation
  writeValue(value: MCPConfiguration | null): void {
    if (value) {
      const tools = value.tools || [];
      this.formGroup.patchValue(
        {
          tools,
          mcpPath: value.mcpPath || DEFAULT_MCP_ENTRYPOINT_PATH,
        },
        { emitEvent: false },
      );

      this.toolDefinitions = tools.map(tool => tool.toolDefinition);
    }
  }

  registerOnChange(fn: (value: MCPConfiguration) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.formGroup.disable();
    } else {
      this.formGroup.enable();
    }
  }

  importTools(): void {
    this.matDialog
      .open<ImportMcpToolsDialogComponent, ImportMcpToolsDialogData, ImportMcpToolsDialogResult>(ImportMcpToolsDialogComponent, {
        data: {
          hasPreviousTools: !!this.toolDefinitions,
        },
        width: GIO_DIALOG_WIDTH.LARGE,
      })
      .afterClosed()
      .pipe(
        filter(result => !!result),
        map(result => result?.tools || []),
        tap((tools: MCPTool[]) => {
          this.formGroup.patchValue({ tools });
          this.formGroup.markAsDirty();
          this.onTouched();

          // Update tool definitions based on imported tools
          this.toolDefinitions = tools.map(tool => tool.toolDefinition);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }
}
