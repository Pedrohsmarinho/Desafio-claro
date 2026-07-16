/**
 * Extrai a mensagem de erro de uma resposta HTTP (formato ErrorResponse do
 * backend, com "error.message") ou de um Error genérico (fallback local, ex.
 * limite/transição inválida detectados no próprio frontend), com um texto
 * padrão caso nenhum dos dois exista.
 */
export function extrairMensagemErro(err: unknown, mensagemPadrao: string): string {
  const httpError = err as { error?: { message?: string }; message?: string };
  return httpError?.error?.message ?? httpError?.message ?? mensagemPadrao;
}
