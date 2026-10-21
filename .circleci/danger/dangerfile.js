import { danger, schedule } from "danger";

const { git, github } = danger;

const checks = [
    {
        name: "liquibase",
        location: "gravitee-apim-repository/gravitee-apim-repository-jdbc/src/main/resources/liquibase/changelogs",
        why: "Changing this file might cause some start up issues because of a failing checksum validation during liquibase execution.",
        checkFn: async (check) => {
            const messages = checkNoLiquibaseFileUpdated(check);
            if (messages.length > 0) {
                await postMessages(messages);
            }

            const { file } = await checkLiquibaseAddPrimaryKey(check);
            if (file != null) {
                await postMessages([
                    {
                        why: "`addPrimaryKey` detected in liquibase changelog 💥. Please create the primary key directly with the create table statement.",
                        file,
                    },
                ]);
            }
        },
    },
    {
        name: "liquibase-sql",
        location: "gravitee-apim-repository/gravitee-apim-repository-jdbc/src/main/resources/liquibase/changelogs",
        why: "Some SQL statements are not compatible with all databases. Please make sure that the SQL statements are compatible with all supported databases.",
        checkFn: async (check) => {
            const files = getModifiedFiles(check.location);
            for (const file of files) {
                const content = await git.diffForFile(file);
                if (content && content.added.includes("sql:")) {
                    await postMessages([
                        {
                            why: "SQL statement detected in liquibase changelog 💥. Please make sure that the SQL statements are compatible with all supported databases.",
                            file,
                        },
                    ]);
                }
            }
        },
    },
];

schedule(check);

async function check() {
    for (const check of checks) {
        await check.checkFn(check);
    }
}

async function postMessages(messages) {
    for (const message of messages) {
        await addPrComment(message);
    }
}

async function addPrComment(message) {
    const commit_id = github.pr.head.sha;
    const owner = github.thisPR.owner;
    const repo = github.thisPR.repo;
    const pull_number = github.thisPR.number;

    await github.api.request(`POST /repos/${owner}/${repo}/pulls/${pull_number}/comments`, {
        commit_id,
        body: message.why,
        path: message.file,
        position: 1,
    });
}

function getModifiedFiles(path) {
    return git.modified_files.filter((file) => file.startsWith(path));
}

function checkNoLiquibaseFileUpdated(check) {
    return getModifiedFiles(check.location).map((file) => ({
        why: check.why,
        file,
    }));
}

async function checkLiquibaseAddPrimaryKey(check) {
    const files = getModifiedFiles(check.location);
    let hasAddPrimaryKey = false;
    let fileToChange = null;

    for (const file of files) {
        const content = await git.diffForFile(file);
        if (content && content.added.includes("addPrimaryKey")) {
            hasAddPrimaryKey = true;
            fileToChange = file;
            break;
        }
    }
    return { file: fileToChange };
}
