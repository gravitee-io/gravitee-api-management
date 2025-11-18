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
export class ApiUtils {
  public static getMcpErrorLabel(error?: string): string {
    if (!error) {
      return '';
    }
    const mcpErrorLabels: Record<string, string> = {
      '-32700': 'Parse Error',
      '-32600': 'Invalid Request',
      '-32601': 'Method not Found',
      '-32602': 'Invalid Params',
      '-32603': 'Internal Error',
      '-32002': 'Resource not Found',
    };
    return mcpErrorLabels[error] ?? error;
  }
}
