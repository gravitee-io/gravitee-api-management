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

import { ShortNumberPipe } from './shortNumber.pipe';

describe('ShortNumberPipe', () => {
  let pipe: ShortNumberPipe;

  const init = async () => {
    pipe = new ShortNumberPipe();
  };

  beforeEach(async () => await init());

  it('should transform numbers to make them more short', () => {
    expect(pipe.transform(100_000_100)).toEqual('100M');

    expect(pipe.transform(100_109, 2)).toEqual('100.11K');

    expect(pipe.transform(100, 2)).toEqual('100');
    expect(pipe.transform(1000, 2)).toEqual('1K');

    expect(pipe.transform(1090, 2)).toEqual('1.09K');
  });
});
