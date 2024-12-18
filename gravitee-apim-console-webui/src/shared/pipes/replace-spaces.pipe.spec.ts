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
import { ReplaceSpacesPipe } from './replace-spaces.pipe';

describe('ReplaceSpacesPipe', () => {
  it('create an instance', () => {
    const pipe = new ReplaceSpacesPipe();
    expect(pipe).toBeTruthy();
  });

  it('should return modified spaces if there are more than one next to each other', () => {
    const pipe = new ReplaceSpacesPipe();
    expect(pipe.transform('  ')).toEqual('\xa0\xa0');
  });

  it('should return the same spaces if only one next to each other exists', () => {
    const pipe = new ReplaceSpacesPipe();
    expect(pipe.transform(' ')).toEqual(' ');
  });

  it('should not modify spaces if they are not next to each other', () => {
    const pipe = new ReplaceSpacesPipe();
    expect(pipe.transform(' A ')).toEqual(' A ');
  });
});
