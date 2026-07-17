export enum StatusPedido {
  EM_PROCESSAMENTO = 'EM_PROCESSAMENTO',
  PAUSADO = 'PAUSADO',
  CANCELADO = 'CANCELADO',
}

export interface Pedido {
  id: number | string;
  displayName: string;
  itens: number;
  peso: number;
  pesoKg: number;
  status: StatusPedido;
  origemLocal?: boolean;
}

export interface PedidoCreateRequest {
  displayName: string;
  itens: number;
  peso: number;
}

export interface PaginaPedidos {
  content: Pedido[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface FiltroPedidos {
  status?: StatusPedido | null;
  busca?: string | null;
  page: number;
  size: number;
  sort?: string | null;
}

export interface DashboardMetricas {
  totalPedidos: number;
  porStatus: Record<StatusPedido, number>;
  limiteMaximo: number;
}

export const STATUS_LABELS: Record<StatusPedido, string> = {
  [StatusPedido.EM_PROCESSAMENTO]: 'Em processamento',
  [StatusPedido.PAUSADO]: 'Pausado',
  [StatusPedido.CANCELADO]: 'Cancelado',
};

export const LIMITE_MAXIMO_PEDIDOS = 5;

const TRANSICOES_PERMITIDAS: Record<StatusPedido, StatusPedido[]> = {
  [StatusPedido.EM_PROCESSAMENTO]: [StatusPedido.PAUSADO, StatusPedido.CANCELADO],
  [StatusPedido.PAUSADO]: [StatusPedido.CANCELADO, StatusPedido.EM_PROCESSAMENTO],
  [StatusPedido.CANCELADO]: [StatusPedido.EM_PROCESSAMENTO],
};

export function podeTransicionar(atual: StatusPedido, novo: StatusPedido): boolean {
  return TRANSICOES_PERMITIDAS[atual].includes(novo);
}
