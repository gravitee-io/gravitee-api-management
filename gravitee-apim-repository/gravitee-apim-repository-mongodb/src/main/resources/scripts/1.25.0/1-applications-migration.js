
print('Applications migration');
db.applications.find().forEach(
    function (application) {
        print("    update the application");

        if (application.type && !("metadata" in application)) {
            print("    set the SIMPLE application type for: " + application._id);
            db.applications.updateOne(
                { _id: application._id },
                {
                    $set: { "metadata.type" : application.type },
                    $unset: { type: "" }
                });
        }

        if (application.clientId) {
            print("    put the clientId of the application into metadata: " + application._id);
            db.applications.updateOne(
                { _id: application._id },
                {
                    $set: { "metadata.client_id" : application.clientId },
                    $unset: { clientId: "" }
                });
        }

        db.applications.updateOne(
            { _id: application._id },
            { $set: { type: 'SIMPLE' }});
    }
);
