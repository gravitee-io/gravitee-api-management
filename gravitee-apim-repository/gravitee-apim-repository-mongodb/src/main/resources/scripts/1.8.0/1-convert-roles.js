var now = new Date();

// update memberships type
print("\n\nUpdate existing memberships to add scope");
db.memberships.find().forEach(
  function (membership) {
    var referenceType = membership._id.referenceType;
    if (   !membership.type.startsWith("1:")
      && !membership.type.startsWith("2:")
      && !membership.type.startsWith("3:")
      && !membership.type.startsWith("4:")) {
      var roleScope;
      if (referenceType === "API" || referenceType === "API_GROUP") {
        roleScope = "3:"
      } else if (referenceType === "APPLICATION" || referenceType === "APPLICATION_GROUP") {
        roleScope = "4:"
      }
      print("    - id[" + membership._id.userId + ":" + membership._id.referenceId + ":" + membership._id.referenceType + "]");
      print("        " + membership.type + " -> " + roleScope + membership.type);
      db.memberships.updateOne(
        {_id: membership._id},
        {
          $set:
            {type: roleScope + membership.type}
        },
        {upsert: true}
      );
    }
  }
);

// create default roles for scope MANAGEMENT and PORTAL
print("\n\nCreate default memberships");
//get all userIds with MANAGEMENT role
var migratedUsers = db.memberships.
  find({"_id.referenceId": "DEFAULT", "_id.referenceType": "MANAGEMENT"}, {"_id": 1}).
  toArray().
  map( function(id) {
    return id._id.userId;
  });
db.users.find().forEach(
  function(user) {
    if (migratedUsers.indexOf(user._id) === -1) {
      print("    - user = " + user._id);
      var mgmt_role = "USER";
      var portal_role = "USER";
      if (user.roles) {
        var roles = user.roles;
        if (roles && roles.length > 0) {
          mgmt_role = roles[0];
          portal_role = roles[0];
          if (mgmt_role === "API_CONSUMER") {
            mgmt_role = "USER";
            portal_role = "USER";
          }
        }
      }
      if (user._id === "admin") {
        mgmt_role = "ADMIN";
        portal_role = "ADMIN";
      } else if (user._id === "api1") {
        mgmt_role = "API_PUBLISHER";
        portal_role = "USER";
      }
      print("    - mgmt_role = " + mgmt_role);
      print("    - portal_role = " + portal_role);
      db.memberships.insert([
        {
          _id: {
            userId: user._id,
            referenceId: "DEFAULT",
            referenceType: "MANAGEMENT"
          },
          _class: "io.gravitee.repository.mongodb.management.internal.model.MembershipMongo",
          type: "1:" + mgmt_role,
          createdAt: now,
          updatedAt: now
        },
        {
          _id: {
            userId: user._id,
            referenceId: "DEFAULT",
            referenceType: "PORTAL"
          },
          _class: "io.gravitee.repository.mongodb.management.internal.model.MembershipMongo",
          type: "2:" + portal_role,
          createdAt: now,
          updatedAt: now
        }
      ]);
    }
  }
);

print("\n\n remove old roles for all users");
db.users.update(
  {},
  {$unset: {roles: ""}},
  {multi: true}
);