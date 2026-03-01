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
import { ComponentHarness } from '@angular/cdk/testing';
import { MatButtonHarness } from '@angular/material/button/testing';

export class NoMcpEntrypointHarness extends ComponentHarness {
  static readonly hostSelector = 'no-mcp-entrypoint';
  protected locateEnableMcpEntrypointButton = this.locatorFor(MatButtonHarness.with({ text: 'Enable MCP' }));
  protected locateLearnMoreMcpEntrypointButton = this.locatorFor(MatButtonHarness.with({ text: 'Learn More' }));

  async getEnableMcpButton(): Promise<MatButtonHarness> {
    return this.locateEnableMcpEntrypointButton();
  }

  async getLearnMoreMcpButton(): Promise<MatButtonHarness> {
    return this.locateLearnMoreMcpEntrypointButton();
  }
}
