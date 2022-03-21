db.groups.find().forEach( function(group) {
  db.groups.updateOne(
    { _id: group._id },
    {
      $set: { "systemInvitation" : true }
    });
});