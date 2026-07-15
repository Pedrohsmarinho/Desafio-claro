export enum StatusPedido {
  EM_PROCESSAMENTO = 'EM_PROCESSAMENTO',
  PAUSADO = 'PAUSADO',
  CANCELADO = 'CANCELADO',
}

export interface Pedido {
  id: number | string;
  displayName: string;
  itens: number;
  peso: number; // gramas (fonte da verdade)
  pesoKg: number;
  status: StatusPedido;
  /** true quando o pedido foi criado via fallback de LocalStorage (API indisponível no momento do cadastro). */
  origemLocal?: boolean;
}

export interface PedidoCreateRequest {
  displayName: string;
  itens: number;
  peso: number; // gramas, já convertido a partir do valor em kg digitado no formulário
}

/** Espelha o formato de Page<T> retornado por GET /api/pedidos/busca. */
export interface PaginaPedidos {
  content: Pedido[];
  totalElements: number;
  totalPages: number;
  number: number; // página atual (0-based)
  size: number;
}

export interface FiltroPedidos {
  status?: StatusPedido | null;
  busca?: string | null;
  page: number;
  size: number;
  sort?: string | null; // "campo,direcao", ex: "displayName,asc"
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

/**
 * Espelha a maquina de estados do backend (StatusPedido#podeTransicionarPara)
 * para habilitar/desabilitar acoes na listagem e para validar pedidos criados
 * localmente (fallback offline), sem depender de round-trip com a API.
 */
export function podeTransicionar(atual: StatusPedido, novo: StatusPedido): boolean {
  return TRANSICOES_PERMITIDAS[atual].includes(novo);
}
