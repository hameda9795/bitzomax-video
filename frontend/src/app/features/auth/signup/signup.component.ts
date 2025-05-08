import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-signup',
  templateUrl: './signup.component.html',
  styles: [`
    .signup-container {
      padding: 2rem;
      max-width: 600px;
      margin: 0 auto;
    }
    
    .form-field {
      width: 100%;
      margin-bottom: 1rem;
    }
    
    .form-row {
      display: flex;
      gap: 1rem;
    }
    
    .form-row .form-field {
      flex: 1;
    }
    
    .neo-card {
      border: 4px solid #000;
      box-shadow: 8px 8px 0 #000;
      padding: 2rem;
      background-color: #fff;
    }
    
    .neo-header {
      margin-bottom: 2rem;
    }
    
    .neo-header h1 {
      font-weight: bold;
      font-size: 2rem;
    }
  `]
})
export class SignupComponent implements OnInit {
  signupForm: FormGroup;
  isSubmitting = false;
  errorMessage: string | null = null;
  hidePassword = true;
  hideConfirmPassword = true;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.signupForm = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3), Validators.pattern('^[a-zA-Z0-9._-]*$')]],
      email: ['', [Validators.required, Validators.email]],
      fullName: ['', [Validators.required]], // Added fullName field
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', Validators.required]
    }, { 
      validators: this.passwordMatchValidator 
    });
  }

  ngOnInit(): void {
    if (this.authService.isAuthenticated()) {
      this.router.navigate(['/']);
    }
  }

  passwordMatchValidator(form: FormGroup) {
    const password = form.get('password')?.value;
    const confirmPassword = form.get('confirmPassword')?.value;
    
    if (password !== confirmPassword) {
      form.get('confirmPassword')?.setErrors({ passwordMismatch: true });
      return { passwordMismatch: true };
    } else {
      form.get('confirmPassword')?.setErrors(null);
      return null;
    }
  }

  onSubmit(): void {
    if (this.signupForm.invalid || this.isSubmitting) {
      // Mark all fields as touched to show validation errors
      Object.keys(this.signupForm.controls).forEach(key => {
        const control = this.signupForm.get(key);
        control?.markAsTouched();
      });
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = null;
    
    const signupData = {
      username: this.signupForm.value.username,
      email: this.signupForm.value.email,
      fullName: this.signupForm.value.fullName, // Added fullName field
      password: this.signupForm.value.password
    };

    this.authService.signup(signupData).subscribe({
      next: () => {
        // After successful signup, login the user
        const loginData = {
          usernameOrEmail: signupData.username,
          password: signupData.password
        };

        this.authService.login(loginData).subscribe({
          next: () => {
            this.router.navigate(['/']);
          },
          error: (error) => {
            // If auto-login fails, redirect to login page
            this.router.navigate(['/auth/login']);
          }
        });
      },
      error: (error) => {
        this.errorMessage = error.error?.message || 'Registration failed. Please try again.';
        this.isSubmitting = false;
      }
    });
  }

  togglePasswordVisibility(): void {
    this.hidePassword = !this.hidePassword;
  }

  toggleConfirmPasswordVisibility(): void {
    this.hideConfirmPassword = !this.hideConfirmPassword;
  }
}