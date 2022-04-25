// Override this variable if you use prefix
const prefix = "";

print(`Add 'environmentId' and 'organizationId' columns in 'audits' table`);

const audits = db.getCollection(`${prefix}audits`);
const environments = db.getCollection(`${prefix}environments`);
const applications = db.getCollection(`${prefix}applications`);
const apis = db.getCollection(`${prefix}apis`);

function handleApiAudit(audit) {
    const envId = getEnvIdByApiId(audit.referenceId);
    const orgId = getOrgIdByEnvId(envId);

    audit.organizationId = orgId || "DEFAULT";
    audit.environmentId = envId || "DEFAULT";
    audits.replaceOne({ _id: audit._id }, audit);
}

function handleApplicationAudit(audit) {
    const envId = getEnvIdByAppId(audit.referenceId);
    const orgId = getOrgIdByEnvId(envId);

    audit.organizationId = orgId || "DEFAULT";
    audit.environmentId = envId || "DEFAULT";
    audits.replaceOne({ _id: audit._id }, audit);
}

function handleEnvironmentAudit(audit) {
    const envId = audit.referenceId;
    const orgId = getOrgIdByEnvId(envId);

    audit.organizationId = orgId || "DEFAULT";
    audit.environmentId = envId || "DEFAULT";
    audits.replaceOne({ _id: audit._id }, audit);
}

function handleOrganizationAudit(audit) {
    const orgId = getOrgIdByEnvId(audit.referenceId);

    audit.organizationId = orgId || "DEFAULT";
    audits.replaceOne({ _id: audit._id }, audit);
}

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

audits.find({ organizationId: null }).forEach((audit) => {
    switch (audit.referenceType) {
        case "API":
            handleApiAudit(audit);
            break;
        case "APPLICATION":
            handleApplicationAudit(audit);
            break;
        case "ENVIRONMENT":
            handleEnvironmentAudit(audit);
            break;
        case "ORGANIZATION":
            handleOrganizationAudit(audit);
            break;
    }
});
print(`Create new indexes in 'audits' table`);

audits.createIndex({ organizationId: 1 });
audits.createIndex({ organizationId: 1, environmentId: 1 });
audits.reIndex();
