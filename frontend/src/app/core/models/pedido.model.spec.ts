import { StatusPedido, podeTransicionar } from './pedido.model';

describe('podeTransicionar', () => {
  const casos: [StatusPedido, StatusPedido, boolean][] = [
    [StatusPedido.EM_PROCESSAMENTO, StatusPedido.PAUSADO, true],
    [StatusPedido.EM_PROCESSAMENTO, StatusPedido.CANCELADO, true],
    [StatusPedido.EM_PROCESSAMENTO, StatusPedido.EM_PROCESSAMENTO, false],
    [StatusPedido.PAUSADO, StatusPedido.CANCELADO, true],
    [StatusPedido.PAUSADO, StatusPedido.EM_PROCESSAMENTO, true],
    [StatusPedido.PAUSADO, StatusPedido.PAUSADO, false],
    [StatusPedido.CANCELADO, StatusPedido.EM_PROCESSAMENTO, true],
    [StatusPedido.CANCELADO, StatusPedido.PAUSADO, false],
    [StatusPedido.CANCELADO, StatusPedido.CANCELADO, false],
  ];

  casos.forEach(([origem, destino, esperado]) => {
    it(`${origem} -> ${destino} deve ser ${esperado}`, () => {
      expect(podeTransicionar(origem, destino)).toBe(esperado);
    });
  });
});
