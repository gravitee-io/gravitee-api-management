print('In audits collection replace PORTAL with ENVIRONMENT or ORGANIZATION regarding audit type');
const organizationFilter = { $or: [
        { event: /^IDENTITY_PROVIDER/ },
        { event: /^ROLE/ },
        { event: /^USER/ },
        { $and:[
                {event: /^MEMBERSHIP/},
                {patch: /"organization"/i}
            ]}
    ]};
const environmentFilter = { $and: [
        { event: { $not: /^IDENTITY_PROVIDER/ }},
        { event: { $not: /^ROLE/ }},
        { event: { $not: /^USER/ }},
        { $or:[
                {event: { $not: /^MEMBERSHIP/}},
                {patch: { $not: /"organization"/i}}
            ]}
    ]};
try {
    const orgUpdateResult = db.audits.updateMany({
        $and: [
            { referenceType:'PORTAL' },
            organizationFilter
        ]}, { $set: { referenceType: "ORGANIZATION" } });
    if (orgUpdateResult.matchedCount && orgUpdateResult.modifiedCount) {
        print(`Successfully modified ${orgUpdateResult.modifiedCount} lines to ORGANIZATION.`)
    } else {
        print(`No audit has been modified to ORGANIZATION.`)
    }
} catch(e) {
    print(`Error while updating audits to ORGANIZATION.\nError: ${e}`);
}
try {
    const envUpdateResult = db.audits.updateMany({
        $and: [
            { referenceType:'PORTAL' },
            environmentFilter
        ]}, { $set: { referenceType: "ENVIRONMENT" } })
    if (envUpdateResult.matchedCount && envUpdateResult.modifiedCount) {
        print(`Successfully modified ${envUpdateResult.modifiedCount} lines to ENVIRONMENT.`)
    } else {
        print(`No audit has been modified to ENVIRONMENT.`)
    }
} catch(e) {
    print(`Error while updating audits to ENVIRONMENT.\nError: ${e}`);
}
