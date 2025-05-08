import { Component, OnInit, ViewChild } from '@angular/core';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { User } from '../../../models/user.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { PageEvent } from '@angular/material/paginator';

@Component({
  selector: 'app-user-management',
  templateUrl: './user-management.component.html',
  styleUrls: ['./user-management.component.scss']
})
export class UserManagementComponent implements OnInit {
  displayedColumns: string[] = ['id', 'username', 'email', 'fullName', 'role', 'subscribed', 'createdAt', 'actions'];
  dataSource: MatTableDataSource<User> = new MatTableDataSource<User>([]);
  users: User[] = [];
  isLoading = true;
  error: string | null = null;
  totalUsers = 0;
  
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  constructor(
    private http: HttpClient,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(page: number = 0, size: number = 10): void {
    this.isLoading = true;
    this.error = null;
    
    this.http.get<any>(`${environment.apiUrl}/admin/users?page=${page}&size=${size}`)
      .subscribe({
        next: (response) => {
          this.users = response.users;
          this.totalUsers = response.totalItems;
          this.dataSource = new MatTableDataSource(this.users);
          this.dataSource.sort = this.sort;
          this.isLoading = false;
        },
        error: (error) => {
          console.error('Error loading users:', error);
          this.error = 'Failed to load users. Please try again later.';
          this.isLoading = false;
        }
      });
  }

  onPageChange(event: PageEvent): void {
    this.loadUsers(event.pageIndex, event.pageSize);
  }

  applyFilter(event: Event): void {
    const filterValue = (event.target as HTMLInputElement).value;
    this.dataSource.filter = filterValue.trim().toLowerCase();

    if (this.dataSource.paginator) {
      this.dataSource.paginator.firstPage();
    }
  }

  toggleUserRole(userId: number, currentRole: string): void {
    const newRole = currentRole === 'ADMIN' ? 'USER' : 'ADMIN';
    
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '450px',
      data: {
        title: 'Change User Role',
        message: `Are you sure you want to change this user's role to ${newRole}?`,
        confirmText: 'Change Role',
        cancelText: 'Cancel'
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.http.put(`${environment.apiUrl}/admin/users/${userId}/role`, { role: newRole })
          .subscribe({
            next: () => {
              // Update local data
              const userIndex = this.users.findIndex(user => user.id === userId);
              if (userIndex !== -1) {
                this.users[userIndex].role = newRole;
                this.dataSource._updateChangeSubscription();
              }
              
              this.snackBar.open(`User role changed to ${newRole} successfully`, 'Close', {
                duration: 3000
              });
            },
            error: (error) => {
              console.error('Error updating user role:', error);
              this.snackBar.open('Failed to update user role. Please try again.', 'Close', {
                duration: 5000
              });
            }
          });
      }
    });
  }

  toggleSubscription(userId: number, isCurrentlySubscribed: boolean): void {
    const action = isCurrentlySubscribed ? 'cancel' : 'activate';
    
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '450px',
      data: {
        title: `${isCurrentlySubscribed ? 'Cancel' : 'Activate'} Subscription`,
        message: `Are you sure you want to ${action} this user's subscription?`,
        confirmText: `${isCurrentlySubscribed ? 'Cancel' : 'Activate'} Subscription`,
        cancelText: 'Go Back'
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.http.put(`${environment.apiUrl}/admin/users/${userId}/subscription`, { subscribed: !isCurrentlySubscribed })
          .subscribe({
            next: (response: any) => {
              // Update local data
              const userIndex = this.users.findIndex(user => user.id === userId);
              if (userIndex !== -1) {
                this.users[userIndex].subscribed = !isCurrentlySubscribed;
                if (response.subscriptionStartDate) {
                  this.users[userIndex].subscriptionStartDate = response.subscriptionStartDate;
                }
                if (response.subscriptionEndDate) {
                  this.users[userIndex].subscriptionEndDate = response.subscriptionEndDate;
                }
                this.dataSource._updateChangeSubscription();
              }
              
              this.snackBar.open(`Subscription ${action}d successfully`, 'Close', {
                duration: 3000
              });
            },
            error: (error) => {
              console.error(`Error ${action}ing subscription:`, error);
              this.snackBar.open(`Failed to ${action} subscription. Please try again.`, 'Close', {
                duration: 5000
              });
            }
          });
      }
    });
  }

  deleteUser(userId: number, username: string): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '450px',
      data: {
        title: 'Delete User',
        message: `Are you sure you want to delete user "${username}"? This action cannot be undone and will remove all their content.`,
        confirmText: 'Delete Permanently',
        cancelText: 'Cancel',
        dangerous: true
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.http.delete(`${environment.apiUrl}/admin/users/${userId}`)
          .subscribe({
            next: () => {
              // Remove from local data
              this.users = this.users.filter(user => user.id !== userId);
              this.dataSource.data = this.users;
              
              this.snackBar.open('User deleted successfully', 'Close', {
                duration: 3000
              });
            },
            error: (error) => {
              console.error('Error deleting user:', error);
              this.snackBar.open('Failed to delete user. Please try again.', 'Close', {
                duration: 5000
              });
            }
          });
      }
    });
  }

  viewUserActivity(userId: number): void {
    // This would navigate to a user activity dashboard
    // To be implemented in a future feature
    this.snackBar.open('User activity view not implemented yet', 'Close', {
      duration: 3000
    });
  }

  formatDate(date: string): string {
    return new Date(date).toLocaleDateString();
  }
}
