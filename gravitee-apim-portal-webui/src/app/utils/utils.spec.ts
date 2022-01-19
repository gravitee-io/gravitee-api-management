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
import { formatCurlCommandLine } from './utils';

describe('Utils', () => {
  it('should format curl as command line', () => {
    const curl = formatCurlCommandLine('http://localhost');
    expect(curl).toEqual(`curl http://localhost`);
  });

  it('should format curl as command line with header', () => {
    const curl = formatCurlCommandLine('http://localhost', { name: 'Content-Type', value: 'application/json' });
    expect(curl).toEqual(`curl --header "Content-Type: application/json" \\
     http://localhost`);
  });

  it('should format curl as command line with headers', () => {
    const curl = formatCurlCommandLine(
      'http://localhost',
      { name: 'Content-Type', value: 'application/json' },
      { name: 'Authorization', value: 'Bearer xxxx-xxxx-xxxx-xxxx' },
    );
    expect(curl).toEqual(`curl --header "Content-Type: application/json" \\
     --header "Authorization: Bearer xxxx-xxxx-xxxx-xxxx" \\
     http://localhost`);
  });
});
