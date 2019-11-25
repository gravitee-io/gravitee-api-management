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
import { SafePipe } from './safe.pipe';
import {
  DomSanitizer,
  SafeValue,
} from '@angular/platform-browser';
import { SecurityContext } from '@angular/core';

class MockDomSanitizer extends DomSanitizer {
  bypassSecurityTrustHtml = jasmine.createSpy('bypassSecurityTrustHtml');

  bypassSecurityTrustResourceUrl = jasmine.createSpy('bypassSecurityTrustResourceUrl');

  bypassSecurityTrustScript = jasmine.createSpy('bypassSecurityTrustScript');

  bypassSecurityTrustStyle = jasmine.createSpy('bypassSecurityTrustStyle');

  bypassSecurityTrustUrl = jasmine.createSpy('bypassSecurityTrustUrl');

  sanitize(context: SecurityContext, value: SafeValue | string | null): string | null;
  sanitize(context: SecurityContext, value: SafeValue | string | null | {}): string | null {
    return undefined;
  }

}

describe('SafePipe', () => {

  it('create an instance', () => {
    const tds = new MockDomSanitizer();
    const pipe = new SafePipe(tds);
    expect(pipe).toBeTruthy();
  });

  it('should throw an Error when type is unknown', () => {
    const unsafeCode = 'http://foo.bar';
    const domSanitizer = new MockDomSanitizer();
    const pipe = new SafePipe(domSanitizer);
    const fakePipe = 'fakePipe';
    expect(() => {
      pipe.transform(unsafeCode, fakePipe);
    }).toThrow(new Error(`Invalid safe type specified: ${fakePipe}`));
  });

  it('should safe HTML', () => {
    const unsafeCode = '<foo></foo>';
    const domSanitizer = new MockDomSanitizer();
    const pipe = new SafePipe(domSanitizer);

    pipe.transform(unsafeCode, 'html');

    expect(domSanitizer.bypassSecurityTrustHtml).toHaveBeenCalledWith(unsafeCode);
  });

  it('should safe Script', () => {
    const unsafeCode = 'var foo = bar*2;';
    const domSanitizer = new MockDomSanitizer();
    const pipe = new SafePipe(domSanitizer);

    pipe.transform(unsafeCode, 'script');

    expect(domSanitizer.bypassSecurityTrustScript).toHaveBeenCalledWith(unsafeCode);
  });

  it('should safe Resource URL', () => {
    const unsafeCode = '/assets/foo/bar';
    const domSanitizer = new MockDomSanitizer();
    const pipe = new SafePipe(domSanitizer);

    pipe.transform(unsafeCode, 'resourceUrl');

    expect(domSanitizer.bypassSecurityTrustResourceUrl).toHaveBeenCalledWith(unsafeCode);
  });

  it('should safe style', () => {
    const unsafeCode = '<style>--foo: bar;</style>';
    const domSanitizer = new MockDomSanitizer();
    const pipe = new SafePipe(domSanitizer);

    pipe.transform(unsafeCode, 'style');

    expect(domSanitizer.bypassSecurityTrustStyle).toHaveBeenCalledWith(unsafeCode);
  });

  it('should safe URL', () => {
    const unsafeCode = 'http://foo.bar';
    const domSanitizer = new MockDomSanitizer();
    const pipe = new SafePipe(domSanitizer);

    pipe.transform(unsafeCode, 'url');

    expect(domSanitizer.bypassSecurityTrustUrl).toHaveBeenCalledWith(unsafeCode);
  });

});
