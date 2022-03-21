
print('Archived users migration');
db.users.find({"status" : "ARCHIVED", "sourceId": {$regex : /^(?!deleted-).*/}}).forEach(
  function (user) {
    print("    update user: ", user._id, "    {source: ", user.source, ",sourceId: ", user.sourceId,"}");
    db.users.updateOne(
      { _id: user._id },
      { $set: { sourceId: 'deleted-' + user.sourceId }}
    );
  }
);