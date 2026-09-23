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
import * as fs from 'fs';
import { isBlank } from './string';
import { CircleCIEnvironment } from '../pipelines';

export interface GraviteeioVersion {
  full: string;
  version: {
    full: string;
    major: string;
    minor: string;
    patch: string;
  };
  qualifier: {
    full: string;
    name: string;
    version: string;
  };
}

export function parse(graviteeioVersion: string): GraviteeioVersion {
  const [versionFull, qualifierFull] = graviteeioVersion.split('-');

  const [major, minor, patch] = versionFull.split('.');
  const [qualifierName, qualifierVersion] = qualifierFull?.split('.') ?? ['', ''];

  return {
    full: graviteeioVersion,
    version: {
      full: versionFull ?? '',
      major: major ?? '',
      minor: minor ?? '',
      patch: patch ?? '',
    },
    qualifier: {
      full: qualifierFull ?? '',
      name: qualifierName ?? '',
      version: qualifierVersion ?? '',
    },
  };
}

export function computeApimVersion(environment: CircleCIEnvironment): string {
  if (!fs.existsSync(environment.apimVersionPath)) {
    throw new Error('computeApiVersion - No file at specified path: ' + environment.apimVersionPath);
  }
  const pomXml = fs.readFileSync(environment.apimVersionPath, 'utf8');
  const { revision, sha1, changelist } = parsePomXml(pomXml);
  return `${revision}${sha1}${changelist}`;
}

function parsePomXml(pomXml: string) {
  const revisionMatch = pomXml.match(/<revision>(.*?)<\/revision>/);
  const sha1Match = pomXml.match(/<sha1>(.*?)<\/sha1>/);
  const changelistMatch = pomXml.match(/<changelist>(.*?)<\/changelist>/);

  return {
    revision: revisionMatch && revisionMatch.length > 0 ? revisionMatch[1] : '',
    sha1: sha1Match && sha1Match.length > 0 ? sha1Match[1] : '',
    changelist: changelistMatch && changelistMatch.length > 0 ? changelistMatch[1] : '',
  };
}

/** A server keeps talking to four client lines: its own and the three before it. */
const BRIDGE_CLIENT_LINES = 4;

/** Newest first, comparing numbers rather than strings, so `4.10` outranks `4.9`. Missing components count as 0. */
const newestFirst = (left: string, right: string): number => {
  const [a, b] = [left.split('.').map(Number), right.split('.').map(Number)];
  for (let index = 0; index < Math.max(a.length, b.length); index++) {
    if ((b[index] ?? 0) !== (a[index] ?? 0)) {
      return (b[index] ?? 0) - (a[index] ?? 0);
    }
  }
  return 0;
};

/** The lines that have shipped their first release, most recent first; the four newest are the supported ones. */
const releasedLines = (tags: string[]): string[] => {
  const lines = tags.filter((tag) => /^\d+\.\d+\.0$/.test(tag)).map((tag) => tag.split('.').slice(0, 2).join('.'));
  return [...new Set(lines)].sort(newestFirst);
};

/**
 * Every image a bridge compatibility run tests the server against.
 *
 * Four client lines — its own and the three before it — and two clients per line: the line's first
 * release, and its last. What "last" means depends on whether the line is still supported: a
 * supported one keeps moving, and its tip lives on our registry as `<line>.x-latest`; a retired one
 * stopped, and its last is the public `graviteeio@<line>` tag. A line a freeze has just cut has
 * neither — only its branch tip. Its own line is named after the branch that carries it: `<line>.x-latest`
 * once that support branch exists, `master-latest` until then. Read from the branches rather than from
 * the branch the pipeline runs on, so a pull request targeting `4.12.x` tests against 4.12's images and
 * not master's.
 *
 * The list used to be written out by hand, once per branch, which made it two files to edit at every
 * code freeze and five more at every retirement — and the linter reflows the array as soon as its
 * length changes, so no script could edit it twice. Derived, it follows a release on its own.
 *
 * One window is knowingly left open: a line counts as released the moment its `<line>.0` tag is on
 * origin, and pushing that tag is what *starts* the release that publishes the images. That gap is
 * around thirty minutes, and bridge compatibility runs are scheduled — 02:00 and 03:00 UTC on a
 * Monday — so landing inside it would take a release running at that hour. Closing it would mean
 * asking the registry at config-generation time, on a host we do not control, from the job that
 * decides whether anything runs at all.
 *
 * @param version the version in the tree, as `computeApimVersion` reads it
 * @param supportBranches the support branches the repository carries
 * @param releasedTags every tag the repository carries, which `index.ts` reads for this action only
 */
export function bridgeClientTags(version: string, supportBranches: string[] | undefined, releasedTags: string[] | undefined): string[] {
  if (releasedTags === undefined || supportBranches === undefined) {
    throw new Error('bridgeClientTags - the branches and tags are missing; index.ts reads them for this action only');
  }

  const { major, minor } = parse(version).version;
  if (Number(minor) < BRIDGE_CLIENT_LINES - 1) {
    throw new Error(
      `bridgeClientTags - ${major}.${minor} has fewer than three previous minors in its own major, and which versions of the previous major it should keep talking to is not something a version number answers. List them here.`,
    );
  }

  const released = releasedLines(releasedTags);
  // Not a matrix worth running: with nothing released, every line looks freshly cut and the matrix
  // becomes four branch tips — including for retired lines, whose `-latest` image is long gone. It
  // reads as valid and fails at `docker pull`, so the emptiness stops here instead.
  if (released.length === 0) {
    throw new Error(
      `bridgeClientTags - no released line among ${releasedTags.length} tags; the matrix would name images that do not exist`,
    );
  }
  const supported = released.slice(0, BRIDGE_CLIENT_LINES);

  return Array.from({ length: BRIDGE_CLIENT_LINES }, (_, index) => `${major}.${Number(minor) - index}`).flatMap((line, index) => {
    // A line's images are published under the branch that carries it, and until it is cut that
    // branch is master.
    if (index === 0 && !supportBranches.includes(`${line}.x`)) {
      return ['master-latest'];
    }
    // Between a freeze and the first release of the line it cut, that line has a branch and nothing
    // else: no first release to test against, and a `graviteeio@` tag that does not exist yet.
    if (!released.includes(line)) {
      return [`${line}.x-latest`];
    }
    const last = supported.includes(line) ? `${line}.x-latest` : `graviteeio@${line}`;
    return [last, `graviteeio@${line}.0`];
  });
}

export function validateGraviteeioVersion(graviteeioVersion: string) {
  if (isBlank(graviteeioVersion)) {
    throw new Error('Graviteeio version is not defined - Please export CI_GRAVITEEIO_VERSION environment variable');
  }
}
