import { HttpErrorResponse } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { LIMITE_MAXIMO_PEDIDOS, Pedido, StatusPedido } from '../models/pedido.model';
import { PedidoService } from './pedido.service';

const FALLBACK_KEY = 'pedidos_fallback_local';
const apiUrl = `${environment.apiUrl}/pedidos`;

describe('PedidoService', () => {
  let service: PedidoService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    localStorage.removeItem(FALLBACK_KEY);

    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(PedidoService);
    httpMock = TestBed.inject(HttpTestingController);

    // descarta o GET inicial disparado pelo construtor do service
    httpMock.match(apiUrl).forEach((req) => req.flush([]));
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.removeItem(FALLBACK_KEY);
  });

  it('cria pedido normalmente quando a API responde com sucesso', (done) => {
    service.criar({ displayName: 'Cliente Teste', itens: 1, peso: 1000 }).subscribe((pedido) => {
      expect(pedido.id).toBe(1);
      expect(pedido.origemLocal).toBeFalsy();
      done();
    });

    const req = httpMock.expectOne(apiUrl);
    expect(req.request.method).toBe('POST');
    req.flush({ id: 1, displayName: 'Cliente Teste', itens: 1, peso: 1000, pesoKg: 1, status: 'EM_PROCESSAMENTO' });

    httpMock.match(apiUrl).forEach((r) => r.flush([]));
  });

  it('faz fallback para LocalStorage quando a API esta indisponivel (status 0)', (done) => {
    service.criar({ displayName: 'Cliente Offline', itens: 2, peso: 500 }).subscribe((pedido) => {
      expect(pedido.origemLocal).toBeTrue();
      expect(String(pedido.id)).toContain('local-');
      expect(pedido.status).toBe(StatusPedido.EM_PROCESSAMENTO);

      const salvos = JSON.parse(localStorage.getItem(FALLBACK_KEY) ?? '[]') as Pedido[];
      expect(salvos.length).toBe(1);
      done();
    });

    const req = httpMock.expectOne(apiUrl);
    req.error(new ProgressEvent('network error'), { status: 0, statusText: 'Unknown Error' });
    httpMock.match(apiUrl).forEach((r) => r.flush([]));
  });

  it('propaga erro de negocio (422) da API sem cair no fallback local', (done) => {
    service.criar({ displayName: 'Cliente Limite', itens: 1, peso: 100 }).subscribe({
      next: () => fail('nao deveria ter sucesso'),
      error: (err: HttpErrorResponse) => {
        expect(err.status).toBe(422);
        expect(localStorage.getItem(FALLBACK_KEY)).toBeNull();
        done();
      },
    });

    const req = httpMock.expectOne(apiUrl);
    req.flush(
      { message: 'Limite maximo de 5 pedidos cadastrados foi atingido' },
      { status: 422, statusText: 'Unprocessable Entity' },
    );
  });

  it('nao permite fallback local acima do limite maximo de pedidos', (done) => {
    // simula 5 pedidos ja conhecidos antes de tentar criar offline
    const cheios: Pedido[] = Array.from({ length: LIMITE_MAXIMO_PEDIDOS }, (_, i) => ({
      id: i + 1,
      displayName: `Pedido ${i + 1}`,
      itens: 1,
      peso: 100,
      pesoKg: 0.1,
      status: StatusPedido.EM_PROCESSAMENTO,
    }));

    // simula o cache interno (pedidosSubject) ja tendo visto 5 pedidos antes de ficar offline
    (service as unknown as { pedidosSubject: { next: (v: Pedido[]) => void } }).pedidosSubject.next(cheios);

    service.criar({ displayName: 'Cliente Extra', itens: 1, peso: 100 }).subscribe({
      next: () => fail('nao deveria ter sucesso'),
      error: (err: Error) => {
        expect(err.message).toContain('Limite maximo');
        done();
      },
    });

    const req = httpMock.expectOne(apiUrl);
    req.error(new ProgressEvent('network error'), { status: 0, statusText: 'Unknown Error' });
  });

  it('altera status de pedido local respeitando a maquina de transicao', (done) => {
    const pedidoLocal: Pedido = {
      id: 'local-123',
      displayName: 'Cliente Local',
      itens: 1,
      peso: 100,
      pesoKg: 0.1,
      status: StatusPedido.EM_PROCESSAMENTO,
      origemLocal: true,
    };
    localStorage.setItem(FALLBACK_KEY, JSON.stringify([pedidoLocal]));

    service.alterarStatus(pedidoLocal, StatusPedido.CANCELADO).subscribe((atualizado) => {
      expect(atualizado.status).toBe(StatusPedido.CANCELADO);
      done();
    });

    httpMock.match(apiUrl).forEach((r) => r.flush([]));
  });

  it('rejeita transicao invalida para pedido local', (done) => {
    const pedidoLocal: Pedido = {
      id: 'local-456',
      displayName: 'Cliente Local',
      itens: 1,
      peso: 100,
      pesoKg: 0.1,
      status: StatusPedido.CANCELADO,
      origemLocal: true,
    };
    localStorage.setItem(FALLBACK_KEY, JSON.stringify([pedidoLocal]));

    service.alterarStatus(pedidoLocal, StatusPedido.PAUSADO).subscribe({
      next: () => fail('nao deveria ter sucesso'),
      error: (err: Error) => {
        expect(err.message).toContain('Transicao invalida');
        done();
      },
    });
  });
});
