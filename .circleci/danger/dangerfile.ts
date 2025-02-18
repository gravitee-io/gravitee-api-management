import { danger, schedule } from "danger";

const { git, github } = danger;

type Check = {
    name: string;
    location: string;
    why: string;
    checkFn: Function;
};
type Message = {
    why: string;
    file: string;
};

const checks: Check[] = [
    {
        name: "liquibase",
        location: "gravitee-apim-repository/gravitee-apim-repository-jdbc/src/main/resources/liquibase/changelogs",
        why: "Changing this file might cause some start up issues because of a failing checksum validation during liquibase execution.",
        checkFn: async (check: Check) => {
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
];

schedule(check);

async function check() {
    for (const check of checks) {
        await check.checkFn(check);
    }
}

async function postMessages(messages: Message[]) {
    for (const message of messages) {
        await addPrComment(message);
    }
}

async function addPrComment(message: Message) {
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

function getModifiedFiles(path: string) {
    return git.modified_files.filter((file) => file.startsWith(path));
}

function checkNoLiquibaseFileUpdated(check: Check) {
    return getModifiedFiles(check.location).map((file) => ({
        why: check.why,
        file,
    }));
}

async function checkLiquibaseAddPrimaryKey(check: Check) {
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
