/**
 * Decodifica apenas o payload (claims) de um JWT, sem validar a assinatura -
 * a validacao de verdade e sempre feita pelo backend. Usado no frontend só
 * para checar localmente se o token ja expirou (claim "exp", em segundos
 * desde epoch) e evitar mandar requisicoes com um token obviamente vencido.
 */
export function decodificarPayloadJwt(token: string): { exp?: number; sub?: string } | null {
  const partes = token.split('.');
  if (partes.length !== 3) {
    return null;
  }

  try {
    const payloadBase64 = partes[1].replace(/-/g, '+').replace(/_/g, '/');
    const payloadJson = decodeURIComponent(
      atob(payloadBase64)
        .split('')
        .map((c) => '%' + c.charCodeAt(0).toString(16).padStart(2, '0'))
        .join(''),
    );
    return JSON.parse(payloadJson);
  } catch {
    return null;
  }
}

export function jwtExpirado(token: string): boolean {
  const payload = decodificarPayloadJwt(token);
  if (!payload?.exp) {
    return true;
  }
  return Date.now() >= payload.exp * 1000;
}
