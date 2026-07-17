const protocol = 'http';
const host = 'localhost:8080';

export const environment = {
  production: true,
  protocol,
  host,
  apiUrl: `${protocol}://${host}/api`,
  actuatorUrl: `${protocol}://${host}/actuator`,
};
