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
import { ChangeDetectionStrategy, Component, input, output, computed } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { GioCardEmptyStateModule } from '@gravitee/ui-particles-angular';

const TRIAL_URL =
  'https://www.gravitee.io/self-hosted-trial?utm_source=oss_apim&utm_medium=apim-mcp-tool-server&utm_campaign=oss_apim_to_ee_apim';

@Component({
  selector: 'no-mcp-entrypoint',
  templateUrl: './no-mcp-entrypoint.component.html',
  styleUrls: ['./no-mcp-entrypoint.component.scss'],
  imports: [MatCardModule, MatButtonModule, MatIconModule, GioCardEmptyStateModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NoMcpEntrypointComponent {
  canEnableMcp = input(false);
  enableMcpEntrypoint = output<boolean>();

  title = computed(() => (this.canEnableMcp() ? 'Bring your tools to life by enabling MCP' : 'MCP Tool Server unavailable'));

  description = computed(() =>
    this.canEnableMcp()
      ? 'Once activated, you can configure, manage and integrate tools seamlessly with your environment.'
      : 'MCP Tool Server is part of Gravitee Enterprise. Accelerate your AI Agent capabilities by letting Gravitee transform your existing Open API Specifications into MCP Tool Servers.',
  );

  onMcpClick(): void {
    if (this.canEnableMcp()) {
      this.enableMcpEntrypoint.emit(true);
      return;
    }

    window.open(TRIAL_URL, '_blank', 'noopener,noreferrer');
  }
}
