import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { PaginaPedidos, Pedido, StatusPedido } from '../../../core/models/pedido.model';
import { PedidoListComponent } from './pedido-list.component';

describe('PedidoListComponent - listagem via API (filtro/paginação/ordenação server-side)', () => {
  let component: PedidoListComponent;
  let fixture: ComponentFixture<PedidoListComponent>;
  let httpMock: HttpTestingController;

  const pedidos: Pedido[] = [
    { id: 1, displayName: 'Pedido João Silva', itens: 1, peso: 500, pesoKg: 0.5, status: StatusPedido.EM_PROCESSAMENTO },
    { id: 2, displayName: 'Pedido Maria Souza', itens: 1, peso: 500, pesoKg: 0.5, status: StatusPedido.PAUSADO },
    { id: 3, displayName: 'Pedido Carlos Lima', itens: 1, peso: 500, pesoKg: 0.5, status: StatusPedido.CANCELADO },
  ];

  const paginaCompleta: PaginaPedidos = {
    content: pedidos,
    totalElements: 3,
    totalPages: 1,
    number: 0,
    size: 5,
  };

  const buscaUrl = `${environment.apiUrl}/pedidos/busca`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [PedidoListComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideAnimations(), provideRouter([])],
    });

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(PedidoListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    // PedidoService dispara um GET /api/pedidos no proprio construtor, e o
    // ngOnInit do componente chama carregar() de novo (para totalPedidosUsuario)
    // + buscar() (para a tabela, via /api/pedidos/busca) - por isso ha varias
    // requisicoes pendentes nesse ponto.
    httpMock.match(`${environment.apiUrl}/pedidos`).forEach((req) => req.flush(pedidos));
    httpMock.expectOne((req) => req.url === buscaUrl).flush(paginaCompleta);
    fixture.detectChanges();
  });

  afterEach(() => httpMock.verify());

  it('carrega a primeira página via API ao iniciar', () => {
    expect(component.pedidos.length).toBe(3);
    expect(component.totalElements).toBe(3);
    expect(component.totalPedidosUsuario).toBe(3);
  });

  it('dispara uma nova requisição com o status ao mudar o filtro', () => {
    component.filtroStatus = StatusPedido.PAUSADO;
    component.onFiltroStatusChange();

    const req = httpMock.expectOne(
      (r) => r.url === buscaUrl && r.params.get('status') === 'PAUSADO' && r.params.get('page') === '0',
    );
    req.flush({ ...paginaCompleta, content: [pedidos[1]], totalElements: 1 });

    expect(component.pedidos.length).toBe(1);
    expect(component.pedidos[0].displayName).toBe('Pedido Maria Souza');
  });

  it('dispara uma nova requisição com o termo de busca, com debounce', fakeAsync(() => {
    component.termoBusca = 'carlos';
    component.onBuscaChange();

    httpMock.expectNone((r) => r.url === buscaUrl && r.params.get('busca') === 'carlos');

    tick(300);

    const req = httpMock.expectOne((r) => r.url === buscaUrl && r.params.get('busca') === 'carlos');
    req.flush({ ...paginaCompleta, content: [pedidos[2]], totalElements: 1 });

    expect(component.pedidos.length).toBe(1);
    expect(component.pedidos[0].displayName).toBe('Pedido Carlos Lima');
  }));

  it('não dispara requisições extras enquanto o usuário ainda está digitando (distinctUntilChanged/debounce)', fakeAsync(() => {
    component.termoBusca = 'c';
    component.onBuscaChange();
    tick(100);
    component.termoBusca = 'ca';
    component.onBuscaChange();
    tick(100);
    component.termoBusca = 'car';
    component.onBuscaChange();
    tick(300);

    const req = httpMock.expectOne((r) => r.url === buscaUrl && r.params.get('busca') === 'car');
    req.flush({ ...paginaCompleta, content: [], totalElements: 0 });

    expect(component.totalElements).toBe(0);
  }));

  it('dispara uma nova requisição ao mudar de página', () => {
    component.onPageChange({ pageIndex: 1, pageSize: 5, length: 10 });

    const req = httpMock.expectOne((r) => r.url === buscaUrl && r.params.get('page') === '1');
    req.flush({ ...paginaCompleta, number: 1 });

    expect(component.pedidos.length).toBe(paginaCompleta.content.length);
  });

  it('dispara uma nova requisição com o campo de ordenação mapeado para o nome usado no backend', () => {
    component.onSortChange({ active: 'pesoKg', direction: 'desc' });

    const req = httpMock.expectOne((r) => r.url === buscaUrl && r.params.get('sort') === 'peso,desc');
    req.flush(paginaCompleta);

    expect(component.pedidos.length).toBe(paginaCompleta.content.length);
  });

  it('usa o total de pedidos do usuário (não filtrado) para o limite de 5, não o total da página filtrada', () => {
    component.filtroStatus = StatusPedido.CANCELADO;
    component.onFiltroStatusChange();

    httpMock.expectOne((r) => r.url === buscaUrl && r.params.get('status') === 'CANCELADO')
      .flush({ ...paginaCompleta, content: [pedidos[2]], totalElements: 1 });

    // mesmo com a busca filtrada retornando so 1 resultado, o total real do
    // usuario (3) e o que decide se o botao "Adicionar" fica desabilitado
    expect(component.totalPedidosUsuario).toBe(3);
    expect(component.podeAtingirLimite()).toBeFalse();
  });

  it('exibe erro sem quebrar a tela quando alterarStatus retorna 404 (pedido de outro usuário)', () => {
    const snackBar = TestBed.inject(MatSnackBar);
    const openSpy = spyOn(snackBar, 'open').and.callThrough();

    component.alterarStatus(pedidos[0], StatusPedido.PAUSADO);

    httpMock
      .expectOne((r) => r.url === `${environment.apiUrl}/pedidos/${pedidos[0].id}/status`)
      .flush({ message: 'Pedido nao encontrado: id=1' }, { status: 404, statusText: 'Not Found' });

    expect(openSpy).toHaveBeenCalled();
    expect(openSpy.calls.mostRecent().args[0]).toContain('Pedido nao encontrado');
    // a tela continua funcional - nao ha excecao nao tratada, o componente
    // continua respondendo (dado carregado anteriormente nao e descartado)
    expect(component.pedidos.length).toBe(3);
  });

  it('exibe erro sem quebrar a tela quando excluir retorna 404 (pedido ja excluido/de outro usuário)', () => {
    const dialog = TestBed.inject(MatDialog);
    spyOn(dialog, 'open').and.returnValue({ afterClosed: () => of(true) } as ReturnType<MatDialog['open']>);
    const snackBar = TestBed.inject(MatSnackBar);
    const openSpy = spyOn(snackBar, 'open').and.callThrough();

    component.excluir(pedidos[0]);

    httpMock
      .expectOne((r) => r.url === `${environment.apiUrl}/pedidos/${pedidos[0].id}` && r.method === 'DELETE')
      .flush({ message: 'Pedido nao encontrado: id=1' }, { status: 404, statusText: 'Not Found' });

    expect(openSpy).toHaveBeenCalled();
    expect(component.pedidos.length).toBe(3);
  });
});
