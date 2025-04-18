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
import { JsonPipe } from '@angular/common';
import { Component, signal, WritableSignal } from '@angular/core';
import { MatCardModule } from '@angular/material/card';

import { CopyCodeIconComponent } from '../copy-code/copy-code-icon/copy-code-icon/copy-code-icon.component';

interface ClaudeNpxConfig {
  mcpServers: {
    gravitee: {
      command: string;
      args: string[];
    };
  };
}

@Component({
  selector: 'app-mcp-use-with-claude',
  standalone: true,
  imports: [MatCardModule, JsonPipe, CopyCodeIconComponent],
  templateUrl: './mcp-use-with-claude.component.html',
  styleUrl: './mcp-use-with-claude.component.scss',
})
export class McpUseWithClaudeComponent {
  npxConfig: WritableSignal<ClaudeNpxConfig> = signal({
    mcpServers: {
      gravitee: {
        command: 'npx',
        args: ['mcp-remote', 'http://localhost:18082/node/mcp/sse'],
      },
    },
  });
}
