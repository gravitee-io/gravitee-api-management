// Override this variable if you use prefix
const prefix = "";

print(`Add 'environmentId' and 'organizationId' columns in 'audits' table`);

const audits = db.getCollection(`${prefix}audits`);
const environments = db.getCollection(`${prefix}environments`);
const organizations = db.getCollection(`${prefix}organizations`);
const applications = db.getCollection(`${prefix}applications`);
const apis = db.getCollection(`${prefix}apis`);

const environmentIdByApiIdMap = {};
function getEnvIdByApiId(apiId) {
    let envId = environmentIdByApiIdMap[apiId];
    if (!envId) {
        const api = apis.findOne({ _id: apiId });
        envId = api ? api.environmentId || null : null;
        environmentIdByApiIdMap[apiId] = envId;
    }
    return envId;
}

const environmentIdByAppIdMap = {};
function getEnvIdByAppId(appId) {
    let envId = environmentIdByAppIdMap[appId];
    if (!envId) {
        const app = applications.findOne({ _id: appId });
        envId = app ? app.environmentId || null : null;
        environmentIdByAppIdMap[appId] = envId;
    }
    return envId;
}

const organizationIdByEnvironmentIdMap = {};
function getOrgIdByEnvId(envId) {
    let orgId = organizationIdByEnvironmentIdMap[envId];
    if (!orgId) {
        const env = environments.findOne({ _id: envId });
        orgId = env ? env.organizationId || null : null;
        organizationIdByEnvironmentIdMap[envId] = orgId;
    }
    return orgId;
}

function handleApiAudit(audit) {
    const envId = getEnvIdByApiId(audit.referenceId);
    const orgId = getOrgIdByEnvId(envId);

    const organizationId = orgId || "DEFAULT";
    const environmentId = envId || "DEFAULT";
    return {
        updateOne: {
            filter: { _id: audit._id },
            update: { $set: { organizationId, environmentId } },
        },
    };
}

function handleApplicationAudit(audit) {
    const envId = getEnvIdByAppId(audit.referenceId);
    const orgId = getOrgIdByEnvId(envId);

    const organizationId = orgId || "DEFAULT";
    const environmentId = envId || "DEFAULT";
    return {
        updateOne: {
            filter: { _id: audit._id },
            update: { $set: { organizationId, environmentId } },
        },
    };
}

function handleEnvironmentAudit(audit) {
    const envId = audit.referenceId;
    const orgId = getOrgIdByEnvId(envId);

    const organizationId = orgId || "DEFAULT";
    const environmentId = envId || "DEFAULT";
    return {
        updateOne: {
            filter: { _id: audit._id },
            update: { $set: { organizationId, environmentId } },
        },
    };
}

function handleOrganizationAudit(audit) {
    const orgId = getOrgIdByEnvId(audit.referenceId);

    const organizationId = orgId || "DEFAULT";
    return {
        updateOne: {
            filter: { _id: audit._id },
            update: { $set: { organizationId } },
        },
    };
}

function updateForMultipleOrgEnv() {
    print(`Run update for multiple one org & env`);
    let bulkUpdate = [];
    let bulkUpdateLimit = 1000;
    audits.find({ organizationId: null }).forEach((audit) => {
        if (!audit.referenceId) {
            audit.referenceId = "DEFAULT";
        }
        switch (audit.referenceType) {
            case "API":
                bulkUpdate.push(handleApiAudit(audit));
                break;
            case "APPLICATION":
                bulkUpdate.push(handleApplicationAudit(audit));
                break;
            case "ENVIRONMENT":
                bulkUpdate.push(handleEnvironmentAudit(audit));
                break;
            case "ORGANIZATION":
                bulkUpdate.push(handleOrganizationAudit(audit));
                break;
        }

        // Write update each bulkUpdateLimit
        if (bulkUpdate.length >= bulkUpdateLimit) {
            audits.bulkWrite(bulkUpdate);
            print(`Update ${bulkUpdate.length} audit`);
            bulkUpdate = [];
        }
    });

    // Write last bulkUpdate
    if (bulkUpdate.length > 0) {
        audits.bulkWrite(bulkUpdate);
        print(`Update ${bulkUpdate.length} audit`);
    }
}

function updateForOnlyOneOrgEnv() {
    const envLength = environments.count();
    const orgLength = organizations.count();

    if (envLength === 1 && orgLength === 1) {
        print(`Run update for only one org & env`);
        const env = environments.findOne();
        const org = organizations.findOne();

        audits.updateMany({}, { $set: { organizationId: env._id, environmentId: org._id } });
        return true;
    }
    return false;
}

const hasExecutedUpdateForOnlyOneOrgEnv = updateForOnlyOneOrgEnv();
if (!hasExecutedUpdateForOnlyOneOrgEnv) {
    updateForMultipleOrgEnv();
}

print(`Create new indexes in 'audits' table`);

audits.createIndex({ organizationId: 1 }, { name: "o1" });
audits.createIndex({ organizationId: 1, environmentId: 1 }, { name: "o1e1" });
audits.reIndex();
