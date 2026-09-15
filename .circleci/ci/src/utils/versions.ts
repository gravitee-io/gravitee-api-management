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
import * as path from 'path';
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

/**
 * The version a branch goes back to developing once `graviteeioVersion` is released.
 *
 * A final release opens the next patch; a qualified one keeps its number and increments the
 * qualifier, so `4.12.17-hotfix.1` is followed by `4.12.17-hotfix.2` rather than by a patch nobody
 * asked for.
 */
export function nextDevelopmentVersion(graviteeioVersion: string): { version: string; qualifier: string } {
  const parsed = parse(graviteeioVersion);
  const { major, minor, patch } = parsed.version;

  if (parsed.qualifier.full === '') {
    return { version: `${major}.${minor}.${Number(patch) + 1}`, qualifier: '' };
  }
  return {
    version: `${major}.${minor}.${patch}`,
    qualifier: `-${parsed.qualifier.name}.${Number(parsed.qualifier.version) + 1}`,
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

/**
 * The core version the distribution assembles, read from the pom beside the root one.
 *
 * The two reactors release apart, so this is the number the core's artefacts carry — `4.13.1` under
 * a product `4.13.4` — and anything fetching them by version needs it rather than the product's.
 * Read here, at generation time, because the tag is checked out; the release itself refuses a pin
 * it cannot assemble, through Maven, before it builds anything.
 */
export function corePin(environment: CircleCIEnvironment): string {
  const distributionPom = `${path.dirname(environment.apimVersionPath)}/gravitee-apim-distribution/pom.xml`;
  if (!fs.existsSync(distributionPom)) {
    throw new Error('corePin - No file at specified path: ' + distributionPom);
  }

  const pin = fs.readFileSync(distributionPom, 'utf8').match(/<apim\.core\.version>(.*?)<\/apim\.core\.version>/);
  if (pin === null || isBlank(pin[1])) {
    throw new Error('corePin - No <apim.core.version> in ' + distributionPom);
  }
  return pin[1];
}

/** A server keeps talking to four client lines: its own and the three before it. */
const BRIDGE_CLIENT_LINES = 4;

/** The lines that have shipped their first release, most recent first; the four newest are the supported ones. */
const releasedLines = (tags: string[]): string[] => {
  const lines = tags.filter((tag) => /^\d+\.\d+\.0$/.test(tag)).map((tag) => tag.split('.').slice(0, 2).join('.'));
  return [...new Set(lines)].sort((a, b) => {
    const [aMajor, aMinor] = a.split('.').map(Number);
    const [bMajor, bMinor] = b.split('.').map(Number);
    return bMajor - aMajor || bMinor - aMinor;
  });
};

/**
 * Every image a bridge compatibility run tests the server against.
 *
 * Four client lines — its own and the three before it — and two clients per line: the line's first
 * release, and its last. What "last" means depends on whether the line is still supported: a
 * supported one keeps moving, and its tip lives on the registry as `<line>.x-latest`; a retired one
 * stopped, and its last is the public `graviteeio@<line>` tag. A line a freeze has just cut has
 * neither — only its branch tip. Master is the exception, having no release of its own to name.
 *
 * The list used to be written out by hand, once per branch, which made it two files to edit at every
 * code freeze and five more at every retirement — and the linter reflows the array as soon as its
 * length changes, so no script could edit it twice. Derived, it follows a release on its own.
 *
 * @param environment the pipeline's environment, carrying the branch and the released tags
 */
export function bridgeClientTags(environment: CircleCIEnvironment): string[] {
  if (environment.releasedTags === undefined) {
    throw new Error('bridgeClientTags - the released tags are missing; index.ts reads them for this action only');
  }

  const { major, minor } = parse(computeApimVersion(environment)).version;
  if (Number(minor) < BRIDGE_CLIENT_LINES - 1) {
    throw new Error(
      `bridgeClientTags - ${major}.${minor} has fewer than three previous minors in its own major, and which versions of the previous major it should keep talking to is not something a version number answers. List them here.`,
    );
  }

  const released = releasedLines(environment.releasedTags);
  const supported = released.slice(0, BRIDGE_CLIENT_LINES);

  return Array.from({ length: BRIDGE_CLIENT_LINES }, (_, index) => `${major}.${Number(minor) - index}`).flatMap((line, index) => {
    if (index === 0 && environment.branch === 'master') {
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

export function validateGraviteeioVersion(graviteeioVersion: string) {
  if (isBlank(graviteeioVersion)) {
    throw new Error('Graviteeio version is not defined - Please export CI_GRAVITEEIO_VERSION environment variable');
  }
}

/** A released version with no qualifier. Prefixed tags — the core lane's — never match. */
const FINAL_VERSION = /^\d+\.\d+\.\d+$/;

/**
 * Whether `version` is the newest final release the repository has produced.
 *
 * This replaces the `--latest` flag. A distribution release is started by pushing a tag, and a
 * tag-triggered pipeline receives no parameter, so which images take `latest` cannot be answered at
 * the keyboard any more — it is read from what has already been released. That is also one fewer
 * thing to get wrong: the flag was answered from memory, and a release from an older support line
 * only had a prompt standing between it and overwriting `latest`.
 *
 * A qualified version is never the latest: an alpha must not become what `docker pull` hands to
 * someone who names no tag.
 * @param version the version being released
 * @param tags every tag the repository carries, raw
 */
export function isLatestRelease(version: string, tags: string[]): boolean {
  if (!FINAL_VERSION.test(version)) {
    return false;
  }

  const rank = (v: string) => v.split('.').map(Number);
  const [major, minor, patch] = rank(version);
  const isHigher = (candidate: string) => {
    const [otherMajor, otherMinor, otherPatch] = rank(candidate);
    if (otherMajor !== major) return otherMajor > major;
    if (otherMinor !== minor) return otherMinor > minor;
    return otherPatch > patch;
  };

  return !tags.filter((tag) => FINAL_VERSION.test(tag)).some(isHigher);
}

/**
 * The support line a version belongs to — `4.13.0-alpha.1` comes from `4.13.x`.
 *
 * A tag-triggered pipeline has no branch: CircleCI leaves `CIRCLE_BRANCH` empty, and a job that
 * reads it passes an empty string onward, which the other end takes for a directory or a cache key.
 * The release commands already derive the branch from the version this way, so the lanes a tag
 * starts do the same rather than trusting a value that is never there.
 */
export function supportLineOf(version: string): string {
  const { version: parsed } = parse(version);
  return `${parsed.major}.${parsed.minor}.x`;
}
