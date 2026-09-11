/**
 * Get the version provided as argument or exit the process
 * @returns {Promise<string>} the version to release
 */
export async function extractVersion() {
  if (argv.version) {
    return argv.version;
  } else {
    console.log(chalk.red("You have to provide the version to release with '--version=VERSION'"));
    process.exit();
  }
}

/**
 * @param {string} releasingVersion, for example: '3.15.11'
 * @returns {{version: string, branch: string, trimmed: string}} the version to release, the branch to release and the
 * trimmed version (looking like {'version': '3.15.11', 'branch': '3.15.x', 'trimmed': '3.15'})
 */
export function computeVersion(releasingVersion) {
  return {
    version: releasingVersion,
    branch: branch(releasingVersion),
    trimmed: trimmed(releasingVersion),
    pattern: pattern(releasingVersion),
  };
}

/**
 * Returns the support line a version belongs to — always, X.Y.0 included: the release tags and
 * bumps the branch it runs on, and the version after 4.13.0 is 4.13.1, not what master should
 * become. Which branch a release runs on is getTargetBranch's answer, and for a hotfix the two
 * differ.
 * @param releasingVersion
 * @returns {string}
 */
function branch(releasingVersion) {
  const split = releasingVersion.split('.');
  return `${split[0]}.${split[1]}.x`;
}

function trimmed(releasingVersion) {
  const split = releasingVersion.split('.');
  return `${split[0]}.${split[1]}`;
}

function pattern(releasingVersion) {
  return branch(releasingVersion).replace('x', '*');
}

/**
 * The version a pom will publish: its revision and its qualifier, without the changelist that the
 * release clears. Kept free of any I/O so it can be tested on its own.
 * @param {string} pomXml the content of a root pom.xml
 * @returns {string}
 */
export function versionFromPom(pomXml) {
  const revision = /<revision>([^<]*)<\/revision>/.exec(pomXml)?.[1];
  if (!revision) {
    throw new Error('No <revision> in this pom.xml — it is not an APIM root pom.');
  }
  // <sha1 /> is self-closing when the qualifier is empty, and neither form should be mistaken for
  // a missing element: a final release legitimately has no qualifier.
  const qualifier = /<sha1>([^<]*)<\/sha1>/.exec(pomXml)?.[1] ?? '';
  return `${revision}${qualifier}`;
}

/**
 * Stops the release when --version and the branch's poms disagree.
 *
 * They are two independent inputs and nothing downstream reconciles them: the artefacts carry what
 * the poms say, the git tag carries what --version says. A mismatch therefore ships one version
 * under the name of another, without failing anywhere. This is the only place they are compared.
 * @param {string} version the version passed to the command
 * @param {string} branch the branch the release will run on
 */
export const ROOT_POM = 'pom.xml';
export const DISTRIBUTION_POM = 'gravitee-apim-distribution/pom.xml';

/**
 * Refuses a branch the repository does not have.
 *
 * A missing branch and a missing file are the same 404 on the contents API, and the pom loop below
 * reads that silence as "this pom inherits the root version" — so a line not cut yet, or simply
 * mistyped, used to pass the check while it printed something untrue. Asked once, before any pom.
 * @param {string} branch
 */
async function assertBranchExists(branch) {
  const url = `https://api.github.com/repos/gravitee-io/gravitee-api-management/branches/${branch}`;
  const response = await fetch(url, { headers: { Accept: 'application/vnd.github+json' } });

  if (response.status === 404) {
    console.log(chalk.red(`'${branch}' does not exist.`));
    console.log(`A release runs on the branch its version implies; --branch releases from another one.`);
    process.exit(1);
  }
  if (!response.ok) {
    console.log(chalk.red(`Cannot check whether '${branch}' exists: ${response.status} ${response.statusText}`));
    console.log(`Checked ${url}`);
    process.exit(1);
  }
}

async function readFromBranch(path, branch) {
  // The contents API rather than raw.githubusercontent.com: the latter is served with a five-minute
  // cache, long enough for a release started right after a version bump to read the previous pom and
  // refuse a correct release. This is cached for one minute, and `Accept: raw` returns the file
  // itself rather than a base64 envelope, so the same parsing applies.
  const url = `https://api.github.com/repos/gravitee-io/gravitee-api-management/contents/${path}?ref=${branch}`;
  const response = await fetch(url, { headers: { Accept: 'application/vnd.github.raw' } });
  if (response.status === 404) {
    // The branch exists — that was checked first — so the file is genuinely absent. Every branch a
    // release runs from carries both poms; measured on master and on 4.8.x through 4.12.x.
    console.log(chalk.red(`'${branch}' has no ${path}.`));
    process.exit(1);
  }
  if (!response.ok) {
    console.log(chalk.red(`Cannot read ${path} on '${branch}': ${response.status} ${response.statusText}`));
    console.log(`Checked ${url}`);
    process.exit(1);
  }
  return response.text();
}

/**
 * Refuses a version that the poms of `branch` do not publish.
 *
 * They are two independent inputs and nothing downstream reconciles them: the artefacts carry what
 * the poms say, the git tag carries what --version says. A mismatch therefore ships one version
 * under the name of another, without failing anywhere.
 *
 * Which poms to check is the caller's decision, because it depends on what is being released. A
 * release of both reactors has to find the version in both — the product's published name comes
 * from the distribution's triplet, not the root's — while a core release moves the root alone and
 * leaves the distribution where it is on purpose.
 * @param {string} version the version passed to the command
 * @param {string} branch the branch the release will run on
 * @param {string[]} poms the poms that have to carry it
 */
export async function assertVersionMatchesPoms(version, branch, poms) {
  await assertBranchExists(branch);

  const published = {};
  for (const pom of poms) {
    const content = await readFromBranch(pom, branch);
    // The distribution only carries a triplet of its own where the reactor was cut; elsewhere it
    // inherits the root's, and there is nothing separate to check.
    if (!/<revision>/.test(content)) {
      console.log(chalk.yellow(`Skipped ${pom}: it inherits the root version on '${branch}'.`));
      continue;
    }
    published[pom] = versionFromPom(content);
  }

  // Every pom skipped means nothing was compared, and the command would go on having announced a
  // check it never made.
  if (Object.keys(published).length === 0) {
    console.log(chalk.red(`None of ${poms.join(', ')} carries a version on '${branch}'.`));
    console.log(`Nothing could be compared, so nothing is confirmed. Release from a branch that carries one.`);
    process.exit(1);
  }

  const wrong = Object.entries(published).filter(([, v]) => v !== version);
  if (wrong.length === 0) {
    return;
  }

  for (const [pom, v] of wrong) {
    console.log(chalk.red(`'${branch}' publishes ${v} from ${pom}, not ${version}.`));
  }
  const distinct = new Set(Object.values(published));
  if (distinct.size > 1) {
    console.log(`The two reactors have drifted apart. Both have to carry the version being released, so set them together.`);
  } else {
    console.log(`The published version comes from the poms; --version only names the tag. Fix the poms, or release ${[...distinct][0]}.`);
  }
  console.log(chalk.yellow(`If you have just pushed a version bump, wait a minute: this reads GitHub through a one-minute cache.`));
  process.exit(1);
}

/**
 * Refuses a version whose core tag already exists.
 *
 * The tag is the trigger, so an existing one means the release already ran. Pushing it again fails
 * halfway through, after the branch has been committed to and reopened on the next version — a state
 * that has to be unwound by hand. Cheaper to refuse before anything is written.
 * @param {string} version the version passed to the command
 */
export async function assertCoreTagIsFree(version) {
  return assertTagIsFree(`core_${version}`);
}

/**
 * Refuses a tag that already exists.
 *
 * The tag is the trigger, so an existing one means the release already ran. Pushing it again fails
 * halfway through, after the branch has been committed to and reopened on the next version — a state
 * that has to be unwound by hand. Cheaper to refuse before anything is written.
 *
 * The core lane prefixes its tags; the distribution takes the bare version, so this takes the whole
 * tag rather than building it.
 * @param {string} tag the tag the release will push
 */
export async function assertTagIsFree(tag) {
  const url = `https://api.github.com/repos/gravitee-io/gravitee-api-management/git/ref/tags/${tag}`;
  const response = await fetch(url, { headers: { Accept: 'application/vnd.github+json' } });

  if (response.ok) {
    console.log(chalk.red(`${tag} already exists: that version has already been released.`));
    console.log(`Release the next version, or delete the tag if it was pushed by mistake.`);
    process.exit(1);
  }
  if (response.status !== 404) {
    console.log(chalk.red(`Cannot check whether ${tag} exists: ${response.status} ${response.statusText}`));
    console.log(`Checked ${url}`);
    process.exit(1);
  }
}

export const HELM_CHART = 'helm/Chart.yaml';

/** The first line-anchored occurrence — `version:` appears again under each dependency. */
const chartField = (yaml, field) => new RegExp(`^${field}:\\s*(\\S+)`, 'm').exec(yaml)?.[1];

/**
 * Stops the release when the Helm chart does not already carry the version being released.
 *
 * The chart holds the version to come rather than a SNAPSHOT, so a release only ships the right
 * chart if someone remembered to clear the qualifier after the last pre-release. That used to be a
 * prompt asking whether the alphas had been removed — a reminder nobody could answer from memory,
 * and which published a chart of the wrong version when they answered yes anyway.
 * @param {string} version the version passed to the command
 * @param {string} branch the branch the release will run on
 */
export async function assertChartMatchesVersion(version, branch) {
  const chart = await readFromBranch(HELM_CHART, branch);
  const found = { version: chartField(chart, 'version'), appVersion: chartField(chart, 'appVersion') };

  const wrong = Object.entries(found).filter(([, value]) => value !== version);
  if (wrong.length === 0) {
    return;
  }

  for (const [field, value] of wrong) {
    console.log(chalk.red(`${HELM_CHART} on '${branch}' carries ${field}: ${value ?? '(none)'}, not ${version}.`));
  }
  console.log(`The chart ships with the release, so it has to name it. Fix it on '${branch}' and start again.`);
  process.exit(1);
}
