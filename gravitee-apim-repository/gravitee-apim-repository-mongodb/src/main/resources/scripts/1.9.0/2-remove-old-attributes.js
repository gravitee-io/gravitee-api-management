// remove group type
print("\n\n remove type attribute for all groups");
db.groups.update(
  {},
  {$unset: {type: ""}},
  {multi: true}
);
print("Done!");

print("\n\nRemove old type in Membership collection");
db.memberships.update(
  {},
  {$unset: {type: ""}},
  {multi: true}
);
print("Done!");

print("\n\nRemove old group in API collection");
db.apis.update(
  {},
  {$unset: {group: ""}},
  {multi: true}
);
print("Done!");

print("\n\nRemove old group in Application collection");
db.applications.update(
  {},
  {$unset: {group: ""}},
  {multi: true}
);
print("Done!");
