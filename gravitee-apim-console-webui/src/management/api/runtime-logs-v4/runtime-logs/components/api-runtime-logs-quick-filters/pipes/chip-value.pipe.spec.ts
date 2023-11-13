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
import { ChipValuePipe } from './chip-value.pipe';

describe('ChipValuePipe', () => {
  it("should return the value when it's not an array", () => {
    const pipe = new ChipValuePipe();

    expect(pipe.transform({ value: '1', label: 'foo' })).toStrictEqual('foo');
  });

  it("should join values when it's an array", () => {
    const pipe = new ChipValuePipe();

    expect(
      pipe.transform([
        { value: '2', label: 'foo' },
        { value: '2', label: 'bar' },
      ]),
    ).toStrictEqual('foo, bar');
  });

  it("should join values when it's a string array", () => {
    const pipe = new ChipValuePipe();

    expect(pipe.transform(['foo', 'bar'])).toStrictEqual('foo, bar');
  });
});
