/**
 * Asks a yes/no question, and stops the command on anything but yes.
 *
 * Testing for 'n' instead lets the empty answer through — the one a stray Enter produces — and these
 * prompts guard a publication. Exiting is the caller's only outcome: a refusal must not fall through
 * to the next line.
 * @param {string} prompt asked as-is, with the (y/n) appended
 * @param {string} [refusal] printed when the answer is not yes
 */
export async function confirm(prompt, refusal = '🚦 Nothing was triggered.') {
  const answer = await question(chalk.blue(`${prompt} (y/n)\n`));
  if (!['y', 'yes'].includes(answer.trim().toLowerCase())) {
    console.log(chalk.yellow(refusal));
    process.exit(1);
  }
}

/**
 * States, in the same terms everywhere, whether this run publishes.
 *
 * The absence of a marker used to be the only sign that a run was real, which is the weakest signal
 * there is: nothing to notice, and nothing to read twice.
 * @param {boolean} dryRun
 */
export function announceMode(dryRun) {
  if (dryRun) {
    console.log(chalk.yellow(`🧪 DRY RUN — the pipeline runs with its pushes disarmed. Nothing is published.\n`));
  } else {
    console.log(chalk.red(`⚠️  REAL RELEASE — this publishes. Pass --dry-run to rehearse instead.\n`));
  }
}

const DRY_RUN = 'dry-run';

/** Same flag to a hurried hand, a different key to minimist. */
const looksLikeDryRun = (flag) => ['dryrun', 'dry'].includes(flag.toLowerCase().replace(/[-_]/g, ''));

/**
 * Whether this run is a rehearsal.
 *
 * The default is a real release, deliberately: a release that quietly publishes nothing is as bad a
 * trap as one that publishes by surprise. What is closed here is the typo — a misspelled flag lands
 * under its own key, leaving this to answer `false`, which is the answer that publishes. Refused
 * rather than ignored, so a slip cannot look like a deliberate omission.
 * @returns {boolean}
 */
export function isDryRun() {
  const misspelled = Object.keys(argv).filter((flag) => flag !== DRY_RUN && looksLikeDryRun(flag));
  if (misspelled.length > 0) {
    console.log(chalk.red(`Unknown option --${misspelled[0]}.`));
    console.log(`Did you mean --${DRY_RUN}? Refusing rather than reading it as a real release.`);
    process.exit(1);
  }

  return !!argv[DRY_RUN];
}

const HOTFIX_QUALIFIER = /-hotfix\.\d+$/;

/**
 * Whether a version is released from a branch cut off a tag rather than from its support line.
 * @param {string} version
 * @returns {boolean}
 */
export function isHotfixVersion(version) {
  return HOTFIX_QUALIFIER.test(version);
}

/**
 * Branch the release runs on: the one --branch names, otherwise the one the version implies.
 *
 * That is not always versions.branch. A hotfix runs on the branch cut from the tag it fixes, while
 * versions.branch stays the support line the version belongs to — which is what the documentation
 * changelog is filed under, hotfix or not.
 * @param {{version: string, branch: string}} versions result of computeVersion()
 * @returns {string}
 */
export function getTargetBranch(versions) {
  const hotfixBranch = HOTFIX_QUALIFIER.test(versions.version) ? `hotfix/${versions.version.replace(HOTFIX_QUALIFIER, '')}` : undefined;

  if (hotfixBranch && argv.branch && argv.branch !== hotfixBranch) {
    console.log(chalk.red(`${versions.version} is released from ${hotfixBranch}, not from ${argv.branch}.`));
    process.exit(1);
  }

  return argv.branch ?? hotfixBranch ?? versions.branch;
}
