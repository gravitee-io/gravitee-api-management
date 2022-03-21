print('Migrate excluded_groups of Pages to new ACL format');
// Override this variable if you use prefix
const prefix = '';

const pages = db.getCollection(`${prefix}pages`);

pages.find({ excluded_groups: { $exists: true, $not: { $size: 0 } } }).forEach((page) => {
    page.visibility = 'PRIVATE';
    page.excludedAccessControls = true;
    page.accessControls = page.excluded_groups.map((referenceId) => ({ referenceId, referenceType: 'GROUP' }));
    delete page.excluded_groups;
    pages.replaceOne({ _id: page._id }, page);
  }
);
