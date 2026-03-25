# Android Native App Scaffold

This folder contains an Android MVVM scaffold that connects to the existing backend.

## Principles
- Reuse existing backend and MySQL database
- No local mobile database
- API-only data access through `/api` endpoints
- ViewModel handles UI state and error mapping

## Current Included Files
- API layer (Retrofit):
  - `data/remote/ApiService.kt`
  - `data/remote/NetworkModule.kt`
- Repository layer:
  - `data/repository/AuthRepository.kt`
- Model layer:
  - `model/AuthModels.kt`
  - `model/ApiResult.kt`
- ViewModel layer:
  - `viewmodel/AuthViewModel.kt`
- View layer:
  - `ui/auth/LoginScreen.kt`

## Emulator URL
Use `http://10.0.2.2:4000/api/` to access local backend from Android emulator.

## Next Steps
1. Add Register screen using existing `/auth/register`
2. Add Dashboard screen using `/dashboard`
3. Add Accounts and Transactions screens using `/accounts` and `/transactions`
4. Add Transfer + OTP screens using `/transfers/initiate`, `/otp/send`, `/otp/verify`
5. Add Statements and Bills screens
