print('Use DEFAULT in memberhips with null referenceId');

try {
    let refUpdateResult = db.memberships.updateMany({ referenceId: null, referenceType: "ENVIRONMENT" }, { $set: { referenceId: "DEFAULT" }});
    if (refUpdateResult.matchedCount && refUpdateResult.modifiedCount) {
        print(`Successfully modified ${refUpdateResult.modifiedCount} memberships referenceId to DEFAULT.`)
    } else {
        print(`No memberships referenceId has been modified to DEFAULT.`)
    }
} catch(e) {
    print(`Error while updating memberships referenceId to DEFAULT.\nError: ${e}`);
}
