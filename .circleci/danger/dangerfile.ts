import { danger, schedule } from "danger";

const { git, github } = danger;

const checks = [
    {
        name: "liquibase",
        location: "gravitee-apim-repository/gravitee-apim-repository-jdbc/src/main/resources/liquibase/changelogs",
        why: "Changing this file might cause some start up issues because of a failing checksum validation during liquibase execution.",
    },
];

schedule(check);

async function check() {
    for (const check of checks) {
        await postMessages(getMessagesForCheck(check));

        const { hasAddPrimaryKey, file } = await checkLiquibaseAddPrimaryKey(check);
        if (hasAddPrimaryKey) {
            await postMessages([
                {
                    why: "addPrimaryKey detected in liquibase changelog 💥. Please create the primary key directly with the create table statement.",
                    file,
                },
            ]);
        }
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

function getMessagesForCheck(check) {
    return getModifiedFiles(check).map((file) => toMessage(check, file));
}

function toMessage(check, file) {
    return {
        why: check.why,
        file,
    };
}

function getModifiedFiles(check) {
    return git.modified_files.filter((file) => file.startsWith(check.location));
}

async function checkLiquibaseAddPrimaryKey(check) {
    const files = getModifiedFiles(check);
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
    return { hasAddPrimaryKey, file: fileToChange };
}
