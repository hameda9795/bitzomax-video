import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AdminLayoutComponent } from './admin-layout/admin-layout.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { UserManagementComponent } from './user-management/user-management.component';
import { VideoManagementComponent } from './video-management/video-management.component';
import { VideoUploadComponent } from './video-upload/video-upload.component';

const routes: Routes = [
  {
    path: '',
    component: AdminLayoutComponent,
    children: [
      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full'
      },
      {
        path: 'dashboard',
        component: DashboardComponent
      },
      {
        path: 'users',
        component: UserManagementComponent
      },
      {
        path: 'videos',
        component: VideoManagementComponent
      },
      {
        path: 'videos/upload',
        component: VideoUploadComponent
      },
      {
        path: 'videos/edit/:id',
        component: VideoUploadComponent
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class AdminRoutingModule { }
