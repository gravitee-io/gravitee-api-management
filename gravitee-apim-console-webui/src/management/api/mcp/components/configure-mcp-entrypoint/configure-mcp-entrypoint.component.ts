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
import { Component, computed, input, Signal } from '@angular/core';
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatInput, MatLabel } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';

import { ToolsDisplayComponent } from '../tools-display/tools-display.component';
import { MCPTool, MCPToolDefinition } from '../../../../../entities/entrypoint/mcp';

export interface ConfigurationMCPForm {
  tools: FormControl<MCPTool[]>;
  mcpPath: FormControl<string>;
}

@Component({
  selector: 'configure-mcp-entrypoint',
  imports: [FormsModule, MatInput, MatLabel, ReactiveFormsModule, ToolsDisplayComponent, MatFormFieldModule],
  templateUrl: './configure-mcp-entrypoint.component.html',
  styleUrl: './configure-mcp-entrypoint.component.scss',
})
export class ConfigureMcpEntrypointComponent {
  formGroup = input<FormGroup<ConfigurationMCPForm>>();

  toolDefinitions: Signal<MCPToolDefinition[]> = computed(() => {
    return this.formGroup()?.controls.tools.value.map((tool) => tool.toolDefinition) ?? [];
  });
}
