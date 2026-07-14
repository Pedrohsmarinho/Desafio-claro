import { jwtExpirado } from './jwt.util';

function criarTokenFake(exp: number): string {
  const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const payload = btoa(JSON.stringify({ sub: 'usuario@teste.com', exp }));
  return `${header}.${payload}.assinatura-fake`;
}

describe('jwtExpirado', () => {
  it('retorna false para um token com expiracao no futuro', () => {
    const expFuturo = Math.floor(Date.now() / 1000) + 3600;
    expect(jwtExpirado(criarTokenFake(expFuturo))).toBeFalse();
  });

  it('retorna true para um token com expiracao no passado', () => {
    const expPassado = Math.floor(Date.now() / 1000) - 3600;
    expect(jwtExpirado(criarTokenFake(expPassado))).toBeTrue();
  });

  it('retorna true para um token invalido (nao tem 3 partes)', () => {
    expect(jwtExpirado('token-invalido')).toBeTrue();
  });
});
