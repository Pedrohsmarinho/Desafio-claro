// Protocolo separado do host:port para que trocar de http para https (ex.:
// atras de um proxy/TLS termination) nao exija reescrever a URL inteira, so
// o valor de "protocol" abaixo.
const protocol = 'http';
const host = 'localhost:8080';

export const environment = {
  production: false,
  protocol,
  host,
  apiUrl: `${protocol}://${host}/api`,
  actuatorUrl: `${protocol}://${host}/actuator`,
};
