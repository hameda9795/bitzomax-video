import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { SharedModule } from '../../shared/shared.module';

import { AdminRoutingModule } from './admin-routing.module';
import { DashboardComponent } from './dashboard/dashboard.component';
import { UserManagementComponent } from './user-management/user-management.component';
import { VideoManagementComponent } from './video-management/video-management.component';
import { VideoUploadComponent } from './video-upload/video-upload.component';
import { AdminLayoutComponent } from './admin-layout/admin-layout.component';
import { NgChartsModule } from 'ng2-charts';

@NgModule({
  declarations: [
    DashboardComponent,
    UserManagementComponent,
    VideoManagementComponent,
    VideoUploadComponent,
    AdminLayoutComponent
  ],
  imports: [
    CommonModule,
    AdminRoutingModule,
    FormsModule,
    ReactiveFormsModule,
    SharedModule,
    NgChartsModule
  ]
})
export class AdminModule { }
