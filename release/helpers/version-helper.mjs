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
export async function assertVersionMatchesPom(version, branch) {
  // The contents API rather than raw.githubusercontent.com: the latter is served with a five-minute
  // cache, long enough for a release started right after a version bump to read the previous pom and
  // refuse a correct release. This is cached for one minute, and `Accept: raw` returns the file
  // itself rather than a base64 envelope, so the same parsing applies.
  const url = `https://api.github.com/repos/gravitee-io/gravitee-api-management/contents/pom.xml?ref=${branch}`;
  const response = await fetch(url, { headers: { Accept: 'application/vnd.github.raw' } });
  if (!response.ok) {
    console.log(chalk.red(`Cannot read pom.xml on '${branch}': ${response.status} ${response.statusText}`));
    console.log(`Checked ${url}`);
    process.exit(1);
  }

  const published = versionFromPom(await response.text());
  if (published !== version) {
    console.log(chalk.red(`'${branch}' publishes ${published}, not ${version}.`));
    console.log(`The published version comes from the poms; --version only names the tag. Fix the poms, or release ${published}.`);
    console.log(chalk.yellow(`If you have just pushed a version bump, wait a minute: this reads GitHub through a one-minute cache.`));
    process.exit(1);
  }
}
