export const environment = {
  production: true,
  // O app roda no navegador do usuário, fora da rede interna do Docker Compose,
  // por isso a URL da API continua sendo o host/porta publicados (localhost:8080)
  // mesmo em produção/Docker, e não o nome do serviço interno do compose.
  apiUrl: 'http://localhost:8080/api',
  actuatorUrl: 'http://localhost:8080/actuator',
};
