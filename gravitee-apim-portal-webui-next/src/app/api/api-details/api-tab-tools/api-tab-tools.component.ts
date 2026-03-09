/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { Component, computed, input } from '@angular/core';

import { CopyCodeComponent } from '../../../../components/copy-code/copy-code.component';
import { McpToolComponent } from '../../../../components/mcp-tool/mcp-tool.component';
import { Api } from '../../../../entities/api/api';

@Component({
  selector: 'app-api-tab-tools',
  imports: [McpToolComponent, CopyCodeComponent],
  templateUrl: './api-tab-tools.component.html',
  styleUrl: './api-tab-tools.component.scss',
})
export class ApiTabToolsComponent {
  api = input.required<Api>();

  mcpTools = computed(() => {
    return this.api().mcp?.tools ?? [];
  });

  mcpEntrypoint = computed(() => {
    if (this.api().entrypoints[0]) {
      return this.api().entrypoints[0].replace(/\/$/, '') + this.api().mcp?.mcpPath;
    }
    return undefined;
  });
}
