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
import {Component, computed, EventEmitter, Input, input, OnInit, output, Signal} from '@angular/core';
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatInput, MatLabel } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';

import { ToolsDisplayComponent } from '../tools-display/tools-display.component';
import {
  DEFAULT_MCP_ENTRYPOINT_PATH,
  MCPConfiguration,
  MCPTool,
  MCPToolDefinition
} from '../../../../../entities/entrypoint/mcp';
import {MatButtonModule} from "@angular/material/button";
import {GIO_DIALOG_WIDTH, GioIconsModule, GioSaveBarModule} from "@gravitee/ui-particles-angular";
import {ToolDisplayComponent} from "../tool-display/tool-display.component";
import {MatDialog} from "@angular/material/dialog";
import {
  ImportMcpToolsDialogComponent,
  ImportMcpToolsDialogData,
  ImportMcpToolsDialogResult
} from "../import-mcp-tools-dialog/import-mcp-tools-dialog.component";
import {GioPermissionModule} from "../../../../../shared/components/gio-permission/gio-permission.module";
import {toSignal} from "@angular/core/rxjs-interop";
import {map} from "rxjs/operators";
import {GioPermissionService} from "../../../../../shared/components/gio-permission/gio-permission.service";

export interface ConfigurationMCPForm {
  tools: FormControl<MCPTool[]>;
  mcpPath: FormControl<string>;
}

@Component({
  selector: 'configure-mcp-entrypoint',
  imports: [FormsModule, MatInput, MatLabel, ReactiveFormsModule, MatFormFieldModule, MatButtonModule, GioIconsModule, ToolDisplayComponent, GioPermissionModule, GioSaveBarModule],
  templateUrl: './configure-mcp-entrypoint.component.html',
  styleUrl: './configure-mcp-entrypoint.component.scss',
})
export class ConfigureMcpEntrypointComponent implements OnInit {
  @Input()
  form: FormGroup<ConfigurationMCPForm> = new FormGroup<ConfigurationMCPForm>({
    tools: new FormControl<MCPTool[]>([]),
    mcpPath: new FormControl<string>(DEFAULT_MCP_ENTRYPOINT_PATH),
  });

  formInitialValues: { tools: MCPTool[]; mcpPath: string };

  toolDefinitions: Signal<MCPToolDefinition[]> = toSignal(this.form.valueChanges.pipe(
    map((form) => form.tools.map((tool: MCPTool) => tool.toolDefinition) || [])
  ))

  submitted = output<MCPConfiguration>();


  constructor(private matDialog: MatDialog, private gioPermissionService: GioPermissionService) {}

  ngOnInit(): void {
    // if (this.creationMode()) {
    //   this.form.patchValue({
    //     tools: [],
    //     mcpPath: DEFAULT_MCP_ENTRYPOINT_PATH,
    //   });
    // } else {
    //   this.formInitialValues = {
    //     tools: this.form.value.tools || [],
    //     mcpPath: this.form.value.mcpPath || DEFAULT_MCP_ENTRYPOINT_PATH,
    //   };
    // }
    // this.formInitialValues = this.form.getRawValue();
    //
    // if (!this.gioPermissionService.hasAnyMatching(['api-definition-u'])) {
    //   this.form.disable();
    // }
  }

  importTools() {
    this.matDialog.open<ImportMcpToolsDialogComponent, ImportMcpToolsDialogData, ImportMcpToolsDialogResult>(ImportMcpToolsDialogComponent,{
      data: {},
      width: GIO_DIALOG_WIDTH.LARGE,
    }).afterClosed().subscribe((result) => {
      if (result?.tools) {
        this.form.patchValue({tools: result.tools});
        this.form.markAsDirty();
      }
    });

  }
}
