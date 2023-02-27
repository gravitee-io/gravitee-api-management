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

import { TestBed } from '@angular/core/testing';
import { BrowserModule, DomSanitizer } from '@angular/platform-browser';

import { SafePipe } from './safe.pipe';

describe('SafePipe', () => {
  let pipe;

  const init = async () => {
    await TestBed.configureTestingModule({
      declarations: [],
      providers: [],
      imports: [BrowserModule],
    }).compileComponents();

    const domSanitizer = TestBed.inject(DomSanitizer);
    pipe = new SafePipe(domSanitizer);
  };

  beforeEach(async () => await init());

  it('should throw an Error when type is unknown', () => {
    try {
      pipe.transform('http://foo.bar', 'foobar');
    } catch (e) {
      expect(e).toEqual(new Error(`Invalid safe type specified: foobar`));
    }
  });

  it('should safe HTML', () => {
    expect(pipe.transform('<div>foo</div>', 'html')).toBeDefined();
  });

  it('should safe Script', () => {
    expect(pipe.transform('var foo = bar*2;', 'script')).toBeDefined();
  });

  it('should safe Resource URL', () => {
    expect(pipe.transform('/assets/foo/bar', 'resourceUrl')).toBeDefined();
  });

  it('should safe style', () => {
    expect(pipe.transform('<style>.foo { display: none;}</style>', 'style')).toBeDefined();
  });

  it('should safe URL', () => {
    expect(pipe.transform('http://foo.bar', 'url')).toBeDefined();
  });
});
