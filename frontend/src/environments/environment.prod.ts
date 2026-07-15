// O app roda no navegador do usuário, fora da rede interna do Docker Compose,
// por isso a URL da API continua sendo o host/porta publicados (localhost:8080)
// mesmo em produção/Docker, e não o nome do serviço interno do compose.
//
// Protocolo separado do host:port (ver comentário em environment.ts): trocar
// para https em um deploy real exige mudar só a constante "protocol".
const protocol = 'http';
const host = 'localhost:8080';

export const environment = {
  production: true,
  protocol,
  host,
  apiUrl: `${protocol}://${host}/api`,
  actuatorUrl: `${protocol}://${host}/actuator`,
};
