// convert type to roles in Membership collection
print("\n\nConvert type to roles in Membership collection");
db.memberships.find().forEach(
  function (membership) {
    print(' - membership: ' + membership._id.userId + "/" + membership._id.referenceId + "/" + membership._id.referenceType);
    print('   type: ' + membership.type);
    var roles = [membership.type];
    print('   roles: ' + roles);
    // if membership is about groups, we have to modify the mongo id
    if (membership._id.referenceType === "API_GROUP" || membership._id.referenceType === "APPLICATION_GROUP") {
      print('   referenceType: ' + membership._id.referenceType);
      var oldId = membership._id;
      membership._id = {
        userId: membership._id.userId,
        referenceId: membership._id.referenceId,
        referenceType: "GROUP"
      };
      membership.roles = roles;
      print('   ...... insert new group');
      db.memberships.insert(membership);
      print('   ...... remove old group: ');
      db.memberships.remove({_id: oldId});
    } else if (!membership.roles) {
      print('   ...... update');
      db.memberships.updateOne(
        {_id: membership._id},
        {
          $set:
            {
              roles: roles,
            }
        },
        {upsert: true}
      );
    }
    print('   ...... done. \n');
  });

//update api groups
print("\n\nMove group -> groups for APIs");
db.apis.find().forEach(
  function(api) {
    if (api.group) {
      print(" - api: " + api._id);
      print("   name: " + api.name);
      print("   group: " + api.group);
      var groups = [api.group];
      print("   groups: " + groups);
      print('   ...... update');
      db.apis.updateOne(
        {_id: api._id},
        {
          $set:
            {groups: groups}
        },
        {upsert: true}
      );
      print('   ...... done. \n');
    }
  });

//update api groups
print("\n\nMove group -> groups for Applications");
db.applications.find().forEach(
  function(application) {
    if (application.group) {
      print(" - application: " + application._id);
      print("   name: " + application.name);
      print("   group: " + application.group);
      var groups = [application.group];
      print("   groups: " + groups);
      print('   ...... update');
      db.applications.updateOne(
        {_id: application._id},
        {
          $set:
            {groups: groups}
        },
        {upsert: true}
      );
      print('   ...... done. \n');
    }
  });

