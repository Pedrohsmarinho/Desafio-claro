const protocol = 'http';
const host = 'localhost:8080';

export const environment = {
  production: false,
  protocol,
  host,
  apiUrl: `${protocol}://${host}/api`,
  actuatorUrl: `${protocol}://${host}/actuator`,
};
