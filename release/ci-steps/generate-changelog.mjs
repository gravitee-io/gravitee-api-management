import { computeVersion, extractVersion } from '../helpers/version-helper.mjs';
import { getJiraIssuesOfVersion, getJiraVersion } from '../helpers/jira-helper.mjs';
import { getChangelogFor } from '../helpers/changelog-helper.mjs';

console.log(chalk.magenta(`#############################################`));
console.log(chalk.magenta(`# 📰 Open APIM docs PR for new Release Note #`));
console.log(chalk.magenta(`#############################################`));

const releasingVersion = await extractVersion();
const versions = computeVersion(releasingVersion);
const dateOptions = { year: 'numeric', month: 'long', day: 'numeric' };

const docRepository = 'gravitee-platform-docs';
const docRepositoryURL = `https://github.com/gravitee-io/${docRepository}`;
const docApimChangelogFolder = 'docs/apim/releases-and-changelogs/changelogs/';
const docApimChangelogFile = `${docApimChangelogFolder}apim-${versions.branch}-changelog.md`;
const localTmpFolder = '.tmp';

const gitBranch = `release-apim-${releasingVersion}`;

echo(chalk.blue(`# Create local tmp folder: ${localTmpFolder}`));
await $`mkdir -p ${localTmpFolder}`;
cd(localTmpFolder);
await $`rm -rf ${docRepository}`;

echo(chalk.blue(`# Clone ${docRepository} repository`));
await $`git clone --depth 1  ${docRepositoryURL} --single-branch --branch=main`;
cd(docRepository);

const version = await getJiraVersion(releasingVersion);
if (version === undefined) {
  echo(chalk.blue(`No Jira release found for: ${releasingVersion}, nothing to do.`));
  process.exit(0);
}
let issues = await getJiraIssuesOfVersion(version.id);

let changelogPatchTemplate = `
## Gravitee API Management ${releasingVersion} - ${new Date().toLocaleDateString('en-US', dateOptions)}

<details>

<summary>Bug fixes</summary>

`;

const gatewayIssues = issues.filter((issue) => issue.components.some((cmp) => cmp.name === 'Gateway'));
issues = issues.filter((issue) => !gatewayIssues.includes(issue));
if (gatewayIssues.length > 0) {
  changelogPatchTemplate += `**Gateway**

${getChangelogFor(gatewayIssues)}

`;
}

const managementAPIIssues = issues.filter((issue) => issue.components.some((cmp) => cmp.name === 'Management API'));
issues = issues.filter((issue) => !managementAPIIssues.includes(issue));
if (managementAPIIssues.length > 0) {
  changelogPatchTemplate += `**Management API**

${getChangelogFor(managementAPIIssues)}

`;
}

const consoleIssues = issues.filter((issue) => issue.components.some((cmp) => cmp.name === 'Console'));
issues = issues.filter((issue) => !consoleIssues.includes(issue));
if (consoleIssues.length > 0) {
  changelogPatchTemplate += `**Console**

${getChangelogFor(consoleIssues)}

`;
}

const portalIssues = issues.filter((issue) => issue.components.some((cmp) => cmp.name === 'Portal'));
issues = issues.filter((issue) => !portalIssues.includes(issue));
if (portalIssues.length > 0) {
  changelogPatchTemplate += `**Portal**

${getChangelogFor(portalIssues)}

`;
}

const helmChartIssues = issues.filter((issue) => issue.components.some((cmp) => cmp.name === 'Helm Chart'));
if (helmChartIssues.length > 0) {
  changelogPatchTemplate += `**Helm Chart**
    
${getChangelogFor(helmChartIssues)}

`;
}

const otherIssues = issues.filter((issue) => !helmChartIssues.includes(issue));
if (otherIssues.length > 0) {
  changelogPatchTemplate += `**Other**

${getChangelogFor(otherIssues)}

`;
}

changelogPatchTemplate += `</details>`;

echo(changelogPatchTemplate);

// write after anchor
const changelogFileContent = fs.readFileSync(docApimChangelogFile, 'utf8');
const changelogFileContentWithPatch = changelogFileContent.replace(
  `# APIM ${versions.branch}`,
  `$&
 ${changelogPatchTemplate}`,
);
echo(chalk.blue(`# Write changelog to ${docApimChangelogFile}`));
fs.writeFileSync(`${docApimChangelogFile}`, changelogFileContentWithPatch);

echo(chalk.blue(`# Commit and push changelog to ${gitBranch}`));
try {
  await $`gh pr close --delete-branch ${gitBranch}`;
} catch (e) {
  // Best effort to have no open PR before creating a new one
}
await $`git checkout -b ${gitBranch}`;
await $`git add ./${docApimChangelogFile}`;
await $`git commit -m "chore: add changelog for ${releasingVersion}"`;
await $`git push --set-upstream origin ${gitBranch}`;

const prBody = `
# APIM ${releasingVersion} has been released

📝 Please review and merge this pull request to add the changelog to the documentation.
`;
echo(chalk.blue('# Create PR on Github Doc repository'));
echo(prBody);
process.env.PR_BODY = prBody;

const releaseNotesPrUrl =
  await $`gh pr create --title "[APIM] Changelog for version ${releasingVersion}" --body "$PR_BODY" --base main --head ${gitBranch}`;
$`echo ${releaseNotesPrUrl.stdout} > /tmp/releaseNotesPrUrl.txt`;
