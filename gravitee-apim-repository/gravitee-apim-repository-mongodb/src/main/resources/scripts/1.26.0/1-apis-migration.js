
print('APIs migration');
db.apis.find().forEach(
    function (api) {
        print("    update the api", api._id);
        db.apis.updateOne(
            { _id: api._id },
            { $set: { apiLifecycleState: 'PUBLISHED' }});
    }
);