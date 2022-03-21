print('Update default theme logo in parameters');
// Override this variable if you use prefix
const prefix = '';

const parameters = db.getCollection(`${prefix}parameters`);

parameters.find({ '_id.key': 'theme.logo', 'value': 'themes/assets/GRAVITEE_LOGO1-01.png' }).forEach(parameter => {
  parameter.value = 'themes/assets/gravitee-logo.svg';
  parameters.replaceOne({ _id: parameter._id }, parameter);
  print('Default theme logo has been updated');
});
