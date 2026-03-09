/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
import { FileSizePipe } from './file-size.pipe';

describe('FileSizePipe', () => {
  const pipe = new FileSizePipe();

  it('should return "-" for null', () => {
    expect(pipe.transform(null)).toBe('-');
  });

  it('should return "-" for undefined', () => {
    expect(pipe.transform(undefined)).toBe('-');
  });

  it('should return "-" for negative values', () => {
    expect(pipe.transform(-1)).toBe('-');
    expect(pipe.transform(-100)).toBe('-');
  });

  it('should return "0 B" for zero', () => {
    expect(pipe.transform(0)).toBe('0 B');
  });

  it('should format bytes', () => {
    expect(pipe.transform(500)).toBe('500 B');
  });

  it('should format kilobytes', () => {
    expect(pipe.transform(1024)).toBe('1 KB');
    expect(pipe.transform(1536)).toBe('1.5 KB');
  });

  it('should format megabytes', () => {
    expect(pipe.transform(1048576)).toBe('1 MB');
  });

  it('should format gigabytes', () => {
    expect(pipe.transform(1073741824)).toBe('1 GB');
  });

  it('should format terabytes', () => {
    expect(pipe.transform(1099511627776)).toBe('1 TB');
  });

  it('should cap at TB for very large values', () => {
    expect(pipe.transform(1099511627776 * 1024)).toBe('1024 TB');
  });
});
