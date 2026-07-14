import { Routes } from '@angular/router';
import { LoginComponent } from './features/login/login.component';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { PedidoListComponent } from './features/pedidos/pedido-list/pedido-list.component';
import { PedidoFormComponent } from './features/pedidos/pedido-form/pedido-form.component';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'dashboard', component: DashboardComponent, canActivate: [authGuard] },
  { path: 'pedidos', component: PedidoListComponent, canActivate: [authGuard] },
  { path: 'pedidos/novo', component: PedidoFormComponent, canActivate: [authGuard] },
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: '**', redirectTo: 'login' },
];
