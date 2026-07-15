import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { MatPaginator } from '@angular/material/paginator';
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

  it('mantém o tamanho de página escolhido pelo usuário (não reseta para o valor padrão)', () => {
    // regressao: [pageSize] no template estava fixo em "pageSizeOptions[0]"
    // (uma expressao estatica, sempre 5) em vez de vinculado ao campo real do
    // componente - qualquer mudanca de pagina/filtro reescrevia o tamanho de
    // pagina do paginador de volta para 5, mesmo que o usuario tivesse
    // escolhido 10 ou 25.
    component.onPageChange({ pageIndex: 0, pageSize: 25, length: 50 });

    const req = httpMock.expectOne((r) => r.url === buscaUrl && r.params.get('size') === '25');
    req.flush({ ...paginaCompleta, size: 25, totalElements: 50 });

    expect(component.pageSize).toBe(25);

    // uma pagina seguinte com o MESMO tamanho de pagina (nao deve voltar a 5)
    component.onPageChange({ pageIndex: 1, pageSize: 25, length: 50 });
    const req2 = httpMock.expectOne((r) => r.url === buscaUrl && r.params.get('page') === '1');
    expect(req2.request.params.get('size')).toBe('25');
    req2.flush({ ...paginaCompleta, size: 25, totalElements: 50, number: 1 });
  });

  it('reflete pageIndex/pageSize do componente no mat-paginator real da tela (não uma expressão estática)', () => {
    component.onPageChange({ pageIndex: 2, pageSize: 25, length: 100 });
    httpMock.expectOne((r) => r.url === buscaUrl && r.params.get('page') === '2')
      .flush({ ...paginaCompleta, size: 25, totalElements: 100, number: 2 });
    fixture.detectChanges();

    const paginator = fixture.debugElement.query((el) => el.componentInstance instanceof MatPaginator)
      .componentInstance as MatPaginator;

    expect(paginator.pageSize).toBe(25);
    expect(paginator.pageIndex).toBe(2);
  });

  it('permite voltar para a página anterior corretamente', () => {
    // regressao: [pageIndex] nao estava vinculado no template, entao o
    // MatPaginator gerenciava seu proprio indice interno, dessincronizado
    // do indice real usado nas requisicoes - "voltar pagina" nao funcionava.
    component.onPageChange({ pageIndex: 2, pageSize: 10, length: 50 });
    httpMock.expectOne((r) => r.url === buscaUrl && r.params.get('page') === '2')
      .flush({ ...paginaCompleta, size: 10, totalElements: 50, number: 2 });
    expect(component.pageIndex).toBe(2);

    component.onPageChange({ pageIndex: 1, pageSize: 10, length: 50 });
    const reqVoltar = httpMock.expectOne((r) => r.url === buscaUrl && r.params.get('page') === '1');
    reqVoltar.flush({ ...paginaCompleta, size: 10, totalElements: 50, number: 1 });

    expect(component.pageIndex).toBe(1);
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
});
