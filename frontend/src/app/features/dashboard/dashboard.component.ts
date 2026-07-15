import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Chart, ChartConfiguration, registerables } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';
import { Subscription, timer } from 'rxjs';
import {
  DashboardMetricas,
  LIMITE_MAXIMO_PEDIDOS,
  Pedido,
  STATUS_LABELS,
  StatusPedido,
} from '../../core/models/pedido.model';
import { DashboardService } from '../../core/services/dashboard.service';
import { PedidoService } from '../../core/services/pedido.service';

Chart.register(...registerables);

const COR_EM_PROCESSAMENTO = '#1d6fa5';
const COR_PAUSADO = '#b9781a';
const COR_CANCELADO = '#8f2323';
const COR_MARCA = '#e4002b';
const COR_VAGA = '#e6e6e6';

const STATUS_ORDENADOS = [StatusPedido.EM_PROCESSAMENTO, StatusPedido.PAUSADO, StatusPedido.CANCELADO];

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, MatCardModule, MatButtonModule, MatIconModule, BaseChartDirective],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent implements OnInit, OnDestroy {
  /** Intervalo do polling que mantem os cards/graficos atualizados mesmo com mudancas feitas em outra aba/sessao. */
  private static readonly INTERVALO_POLLING_MS = 20_000;

  private subscription?: Subscription;
  private pollingSubscription?: Subscription;

  limiteMaximo = LIMITE_MAXIMO_PEDIDOS;
  totalPedidos = 0;
  totalEmProcessamento = 0;
  pesoTotalKg = 0;
  itensTotais = 0;
  carregando = true;

  barChartData: ChartConfiguration<'bar'>['data'] = {
    labels: [],
    datasets: [{ data: [], label: 'Pedidos por status', backgroundColor: [COR_EM_PROCESSAMENTO, COR_PAUSADO, COR_CANCELADO] }],
  };
  barChartOptions: ChartConfiguration<'bar'>['options'] = {
    responsive: true,
    plugins: { legend: { display: false } },
    scales: { y: { beginAtZero: true, ticks: { stepSize: 1 } } },
  };

  pieChartData: ChartConfiguration<'pie'>['data'] = {
    labels: ['Pedidos cadastrados', 'Vagas disponíveis'],
    datasets: [{ data: [0, LIMITE_MAXIMO_PEDIDOS], backgroundColor: [COR_MARCA, COR_VAGA] }],
  };
  pieChartOptions: ChartConfiguration<'pie'>['options'] = {
    responsive: true,
    plugins: { legend: { position: 'bottom' } },
  };

  constructor(
    private pedidoService: PedidoService,
    private dashboardService: DashboardService,
  ) {}

  ngOnInit(): void {
    // Cards de resumo: Observable compartilhado, reflete imediatamente
    // qualquer acao feita nesta aba (criar/excluir/mudar status).
    this.subscription = this.pedidoService.pedidos$.subscribe((pedidos) => {
      this.atualizarCardsResumo(pedidos);
      this.carregando = false;
    });

    // Graficos (barras "por status" e pizza "vs. limite"): mesma fonte que
    // alimenta o Grafana em espirito (uma unica contagem autoritativa no
    // backend), mas escopada ao usuario logado - GET /api/dashboard/metricas,
    // nao as metricas globais do Prometheus (ver DashboardService/README).
    this.carregarMetricas();

    // Polling: cobre mudancas feitas em outra aba/sessao (ex: outro usuario,
    // ou este mesmo usuario em outro dispositivo) sem precisar recarregar a
    // pagina manualmente.
    this.pollingSubscription = timer(DashboardComponent.INTERVALO_POLLING_MS, DashboardComponent.INTERVALO_POLLING_MS)
      .subscribe(() => {
        this.pedidoService.carregar();
        this.carregarMetricas();
      });
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
    this.pollingSubscription?.unsubscribe();
  }

  private carregarMetricas(): void {
    this.dashboardService.buscarMetricas().subscribe((metricas) => this.atualizarGraficos(metricas));
  }

  private atualizarCardsResumo(pedidos: Pedido[]): void {
    this.totalPedidos = pedidos.length;
    this.totalEmProcessamento = pedidos.filter((p) => p.status === StatusPedido.EM_PROCESSAMENTO).length;
    this.pesoTotalKg = pedidos.reduce((soma, p) => soma + p.pesoKg, 0);
    this.itensTotais = pedidos.reduce((soma, p) => soma + p.itens, 0);
  }

  private atualizarGraficos(metricas: DashboardMetricas): void {
    const contagem = STATUS_ORDENADOS.map((status) => metricas.porStatus[status] ?? 0);

    this.barChartData = {
      ...this.barChartData,
      labels: STATUS_ORDENADOS.map((status) => STATUS_LABELS[status]),
      datasets: [{ ...this.barChartData.datasets[0], data: contagem }],
    };

    const restante = Math.max(metricas.limiteMaximo - metricas.totalPedidos, 0);
    this.pieChartData = {
      ...this.pieChartData,
      datasets: [{ ...this.pieChartData.datasets[0], data: [metricas.totalPedidos, restante] }],
    };
  }
}
