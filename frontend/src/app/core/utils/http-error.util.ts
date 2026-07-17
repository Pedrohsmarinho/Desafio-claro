export function extrairMensagemErro(err: unknown, mensagemPadrao: string): string {
  const httpError = err as { error?: { message?: string }; message?: string };
  return httpError?.error?.message ?? httpError?.message ?? mensagemPadrao;
}
