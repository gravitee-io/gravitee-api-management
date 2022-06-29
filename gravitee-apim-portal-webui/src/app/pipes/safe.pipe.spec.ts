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

import { createPipeFactory, SpectatorPipe } from '@ngneat/spectator/jest';
import { SafePipe } from './safe.pipe';

describe('SafePipe', () => {
  let spectator: SpectatorPipe<SafePipe>;
  const createPipe = createPipeFactory({
    pipe: SafePipe,
  });

  it('create an instance', () => {
    spectator = createPipe();
    expect(spectator.element).toBeTruthy();
  });

  it('should throw an Error when type is unknown', () => {
    try {
      createPipe(`{{ 'http://foo.bar' | safe:'foobar' }}`);
    } catch (e) {
      expect(e).toEqual(new Error(`Invalid safe type specified: foobar`));
    }
  });

  it('should safe HTML', () => {
    spectator = createPipe(`{{ '<div>foo</div>' | safe:'html' }}`);
    expect(spectator.element.innerHTML).toContain('foo');
  });

  it('should safe Script', () => {
    const value = 'var foo = bar*2;';
    spectator = createPipe(`{{ value | safe:'script' }}`, {
      hostProps: {
        value,
      },
    });
    expect(spectator.element.innerHTML).toContain(value);
  });

  it('should safe Resource URL', () => {
    spectator = createPipe(`{{ '/assets/foo/bar' | safe:'resourceUrl' }}`);
    expect(spectator.element.innerHTML).toContain('/assets/foo/bar');
  });

  it('should safe style', () => {
    spectator = createPipe(`{{ '<style>.foo { display: none;}</style>' | safe:'style' }}`);
    expect(spectator.element).toBeDefined();
  });

  it('should safe URL', () => {
    spectator = createPipe(`{{ 'http://foo.bar' | safe:'url' }}`);
    expect(spectator.element.innerHTML).toContain('http://foo.bar');
  });
});
