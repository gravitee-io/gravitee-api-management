import { computeVersion, extractVersion } from '../helpers/version-helper.mjs';
import { getJiraIssuesOfVersion, getJiraVersion } from '../helpers/jira-helper.mjs';
import { ChangelogSections, ComponentTypes, getTicketsFor } from '../helpers/changelog-helper.mjs';

console.log(chalk.magenta(`#############################################`));
console.log(chalk.magenta(`# 📰 Open APIM docs PR for new Release Note #`));
console.log(chalk.magenta(`#############################################`));

const releasingVersion = await extractVersion();
const versions = computeVersion(releasingVersion);
const dateOptions = { year: 'numeric', month: 'long', day: 'numeric' };

const docRepository = 'gravitee-platform-docs';
const docRepositoryURL = `https://github.com/gravitee-io/${docRepository}`;
const docApimChangelogFolder = `docs/apim/${versions.trimmed}/overview/changelog/`;
const docApimChangelogFile = `${docApimChangelogFolder}apim-${versions.branch}.md`;
const localTmpFolder = '.tmp';

const gitBranch = `release-apim-${releasingVersion}`;

echo(chalk.blue(`# Create local tmp folder: ${localTmpFolder}`));
await $`mkdir -p ${localTmpFolder}`;
cd(localTmpFolder);

echo(chalk.blue(`# Clone ${docRepository} repository`));
await $`git clone --depth 1  ${docRepositoryURL} --single-branch --branch=main`;
cd(docRepository);

const version = await getJiraVersion(releasingVersion);
if (version === undefined) {
  echo(chalk.blue(`No Jira release found for: ${releasingVersion}, nothing to do.`));
  process.exit(0);
}
const issues = await getJiraIssuesOfVersion(version.id);

let changelogPatchTemplate = `
## Gravitee API Management ${releasingVersion} - ${new Date().toLocaleDateString('en-US', dateOptions)}
`;

ChangelogSections.forEach((section) => {
  let changelogSection = '';
  [...ComponentTypes, 'Other'].forEach((componentType) => {
    const ticketsForComponent = getTicketsFor(issues, componentType, section.ticketType);
    if (ticketsForComponent) {
      changelogSection += ticketsForComponent;
    }
  });
  if (changelogSection) {
    changelogPatchTemplate += `<details>

<summary>${section.title}</summary>

${changelogSection}</details>

`;
  }
});

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
