#!/usr/bin/env zx

// Cuts a hotfix branch from a released tag and sets the version that branch will release.
//
// The released version comes from the poms — <revision><sha1><changelist> — not from the
// --version passed to the release, which only feeds the commit message, the tag and the UI
// build.json files. A branch cut from a tag therefore sits at the released version itself, with
// an empty changelist: releasing from it untouched would republish that exact version. The
// qualifier goes into <sha1>, the slot alpha and milestone releases already use, and -SNAPSHOT
// comes back until the release clears it.
//
// Only the first hotfix is prepared here. The release bumps the branch it runs on, and on a
// qualified version it bumps the qualifier: once 4.12.17-hotfix.1 has shipped, the branch is
// already at 4.12.17-hotfix.2-SNAPSHOT. So a branch that exists is checked out as it stands —
// re-running the preparation on it would set the qualifier back to .1 and release backwards.

import { extractVersion } from '../helpers/version-helper.mjs';

const releasedVersion = await extractVersion();

if (!/^\d+\.\d+\.\d+$/.test(releasedVersion)) {
  console.log(chalk.red(`'${releasedVersion}' is not a released version. Expected X.Y.Z — the tag to branch from.`));
  process.exit(1);
}

const branch = `hotfix/${releasedVersion}`;
const supportBranch = `${releasedVersion.split('.').slice(0, 2).join('.')}.x`;

// Every path below is relative to the repository root, and this script runs from release/.
cd((await $`git rev-parse --show-toplevel`).stdout.trim());

// git switch carries uncommitted changes onto the branch it moves to, and the commit below takes
// everything tracked and modified. Work in progress would ship inside the hotfix without a word.
const dirty = (await $`git status --porcelain`).stdout.trim();
if (dirty) {
  console.log(chalk.red('The working tree is not clean. Commit or stash before preparing a hotfix:\n'));
  console.log(dirty);
  process.exit(1);
}

async function exists(ref) {
  try {
    await $`git rev-parse --verify --quiet ${ref}`;
    return true;
  } catch {
    return false;
  }
}

// The version the branch will release, read where the release itself reads it: the poms.
function versionFromPom() {
  const pom = fs.readFileSync('pom.xml', 'utf8');
  const revision = /<revision>([^<]*)<\/revision>/.exec(pom)?.[1] ?? '';
  const qualifier = /<sha1>([^<]*)<\/sha1>/.exec(pom)?.[1] ?? '';
  return `${revision}${qualifier}`;
}

function printNextSteps() {
  const version = versionFromPom();
  console.log(chalk.green(`\n${branch} is ready, at ${version}.\n`));
  console.log(`  1. Add the fix, as one commit.`);
  console.log(`  2. yarn full_release --version=${version}`);
  console.log(`  3. Cherry-pick the fix into ${supportBranch}, so the next patch carries it too.`);
}

const alreadyPrepared = `${branch} already exists — the release that shipped the previous hotfix left it prepared.`;

if (await exists(`refs/heads/${branch}`)) {
  console.log(chalk.blue(alreadyPrepared));
  await $`git switch ${branch}`;
  printNextSteps();
  process.exit(0);
}

if (await exists(`refs/remotes/origin/${branch}`)) {
  console.log(chalk.blue(`${alreadyPrepared} Checking out origin's.`));
  await $`git switch -c ${branch} --no-track origin/${branch}`;
  printNextSteps();
  process.exit(0);
}

if (!(await exists(`refs/tags/${releasedVersion}`))) {
  console.log(chalk.red(`Tag ${releasedVersion} not found. Fetch the tags first: git fetch --tags`));
  process.exit(1);
}

const qualifier = '-hotfix.1';
const version = `${releasedVersion}${qualifier}`;

console.log(chalk.blue(`Cutting ${branch} from tag ${releasedVersion}, to release ${version}\n`));

await $`git switch -c ${branch} --no-track ${releasedVersion}`;

// Edited in Node rather than with sed: the release jobs run on Linux and can use GNU sed, this
// runs on whatever the developer has, and BSD sed takes different arguments.
//
// A file absent from an old tag is skipped — the tree has changed since — unless the version
// depends on it, and then absence means this is not a tag of this repository. A file that is
// present but does not match is always an error: the shape moved, and a half-prepared branch would
// release the wrong version without saying so.
function edit(path, replacements, { required = false } = {}) {
  if (!fs.existsSync(path)) {
    if (required) {
      console.log(chalk.red(`${path} is missing. This is not a tag of this repository.`));
      process.exit(1);
    }
    console.log(chalk.yellow(`  skipped ${path}, not in this tag`));
    return;
  }
  let content = fs.readFileSync(path, 'utf8');
  for (const [pattern, replacement] of replacements) {
    if (!pattern.test(content)) {
      console.log(chalk.red(`Nothing to replace in ${path} for ${pattern}. Aborting rather than half-preparing the branch.`));
      process.exit(1);
    }
    content = content.replace(pattern, replacement);
  }
  fs.writeFileSync(path, content);
  console.log(`  ${path}`);
}

const versionTriplet = [
  // <sha1 /> is self-closing when the qualifier is empty, which a plain <sha1>.*</sha1> pattern
  // never matches — and a released tag always holds that empty form.
  [/<sha1( *\/>|>[^<]*<\/sha1>)/, `<sha1>${qualifier}</sha1>`],
  [/<changelist>.*<\/changelist>/, `<changelist>-SNAPSHOT</changelist>`],
];

edit('pom.xml', versionTriplet, { required: true });

// The distribution only carries a triplet of its own since it left the product reactor, on master.
// On a support branch it is still a module and inherits the root's version, so there is nothing
// to set there.
const distributionPom = 'gravitee-apim-distribution/pom.xml';
if (fs.existsSync(distributionPom) && /<revision>/.test(fs.readFileSync(distributionPom, 'utf8'))) {
  edit(distributionPom, versionTriplet);
} else {
  console.log(chalk.yellow(`  skipped ${distributionPom}, it inherits the root version on this branch`));
}

for (const buildJson of [
  'gravitee-apim-console-webui/build.json',
  'gravitee-apim-portal-webui/build.json',
  'gravitee-apim-portal-webui-next/build.json',
  'gravitee-gamma/gravitee-gamma-control-plane-webui/build.json',
]) {
  edit(buildJson, [[/"version": ".*"/, `"version": "${version}-SNAPSHOT"`]]);
}

edit(
  'helm/Chart.yaml',
  [
    [/^version:.*$/m, `version: ${version}`],
    [/^appVersion:.*$/m, `appVersion: ${version}`],
  ],
  { required: true },
);

await $`git add --update`;
await $`git commit -m ${`chore: prepare ${version}`}`;

printNextSteps();
