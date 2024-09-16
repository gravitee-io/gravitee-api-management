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
import { DateAgoPipe } from './date-ago.pipe';

describe('DateAgoPipe', () => {
  it('create an instance', () => {
    const pipe = new DateAgoPipe();
    expect(pipe).toBeTruthy();
  });

  it('returns just now', () => {
    const pipe = new DateAgoPipe();
    expect(pipe.transform(new Date())).toEqual('just now');
  });

  it('handles recent days', () => {
    const pipe = new DateAgoPipe();
    expect(pipe.transform(new Date(new Date().getTime() - 1000 * 60 * 60 * 24 * 3))).toEqual('3 days ago');
  });

  it('handles recent weeks', () => {
    const pipe = new DateAgoPipe();
    expect(pipe.transform(new Date(new Date().getTime() - 1000 * 60 * 60 * 24 * 20))).toEqual('2 weeks ago');
  });

  it('handles recent months', () => {
    const pipe = new DateAgoPipe();
    expect(pipe.transform(new Date(new Date().getTime() - 1000 * 60 * 60 * 24 * 357))).toEqual('11 months ago');
  });
});
