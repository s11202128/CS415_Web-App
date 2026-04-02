package com.bof.mobile.data.repository

import com.bof.mobile.data.remote.ApiService
import com.bof.mobile.model.ApiResult
import com.bof.mobile.model.BillerItem
import com.bof.mobile.model.InitiateTransferRequest
import com.bof.mobile.model.InitiateTransferResponse
import com.bof.mobile.model.RecipientItem
import com.bof.mobile.model.TransferMoneyRequest
import com.bof.mobile.model.TransferMoneyResponse
import com.bof.mobile.model.ValidateDestinationRequest
import com.bof.mobile.model.ValidateDestinationResponse
import com.bof.mobile.model.VerifyTransferOtpRequest
import com.bof.mobile.model.VerifyTransferRequest
import com.bof.mobile.model.VerifyTransferResponse
import retrofit2.HttpException
import java.io.IOException

class TransferRepository(private val apiService: ApiService) {

    suspend fun validateDestination(
        fromAccountId: Int,
        toAccountNumber: String
    ): ApiResult<ValidateDestinationResponse> {
        return try {
            ApiResult.Success(
                apiService.validateDestination(
                    ValidateDestinationRequest(fromAccountId = fromAccountId, toAccountNumber = toAccountNumber)
                )
            )
        } catch (e: HttpException) {
            ApiResult.Error(message = "Destination validation failed: ${e.message()}", code = e.code())
        } catch (e: IOException) {
            ApiResult.Error(message = "Network unavailable. Please try again.")
        } catch (e: Exception) {
            ApiResult.Error(message = e.message ?: "Unexpected error")
        }
    }

    suspend fun initiateTransfer(
        fromAccountId: Int,
        toAccountNumber: String,
        amount: Double,
        description: String
    ): ApiResult<InitiateTransferResponse> {
        return try {
            ApiResult.Success(
                apiService.initiateTransfer(
                    InitiateTransferRequest(
                        fromAccountId = fromAccountId,
                        toAccountNumber = toAccountNumber,
                        amount = amount,
                        description = description
                    )
                )
            )
        } catch (e: HttpException) {
            ApiResult.Error(message = "Transfer initiation failed: ${e.message()}", code = e.code())
        } catch (e: IOException) {
            ApiResult.Error(message = "Network unavailable. Please try again.")
        } catch (e: Exception) {
            ApiResult.Error(message = e.message ?: "Unexpected error")
        }
    }

    suspend fun verifyTransfer(transferId: String, otp: String): ApiResult<VerifyTransferResponse> {
        return try {
            ApiResult.Success(
                apiService.verifyTransfer(VerifyTransferRequest(transferId = transferId, otp = otp))
            )
        } catch (e: HttpException) {
            ApiResult.Error(message = "OTP verification failed: ${e.message()}", code = e.code())
        } catch (e: IOException) {
            ApiResult.Error(message = "Network unavailable. Please try again.")
        } catch (e: Exception) {
            ApiResult.Error(message = e.message ?: "Unexpected error")
        }
    }

    suspend fun transfer(request: TransferMoneyRequest): ApiResult<TransferMoneyResponse> {
        return try {
            ApiResult.Success(apiService.transfer(request))
        } catch (e: HttpException) {
            ApiResult.Error(message = "Transfer failed: ${e.message()}", code = e.code())
        } catch (e: IOException) {
            ApiResult.Error(message = "Network unavailable. Please try again.")
        } catch (e: Exception) {
            ApiResult.Error(message = e.message ?: "Unexpected error")
        }
    }

    suspend fun verifyTransferOtp(request: VerifyTransferOtpRequest): ApiResult<TransferMoneyResponse> {
        return try {
            ApiResult.Success(apiService.verifyTransferOtp(request))
        } catch (e: HttpException) {
            ApiResult.Error(message = "OTP verification failed: ${e.message()}", code = e.code())
        } catch (e: IOException) {
            ApiResult.Error(message = "Network unavailable. Please try again.")
        } catch (e: Exception) {
            ApiResult.Error(message = e.message ?: "Unexpected error")
        }
    }

    suspend fun searchRecipients(query: String): ApiResult<List<RecipientItem>> {
        return try {
            ApiResult.Success(apiService.searchRecipients(query))
        } catch (e: HttpException) {
            ApiResult.Error(message = "Could not search recipients: ${e.message()}", code = e.code())
        } catch (e: IOException) {
            ApiResult.Error(message = "Network unavailable. Please try again.")
        } catch (e: Exception) {
            ApiResult.Error(message = e.message ?: "Unexpected error")
        }
    }

    suspend fun getBillers(): ApiResult<List<BillerItem>> {
        return try {
            ApiResult.Success(apiService.getBillers())
        } catch (e: HttpException) {
            ApiResult.Error(message = "Failed to load billers: ${e.message()}", code = e.code())
        } catch (e: IOException) {
            ApiResult.Error(message = "Network unavailable. Please try again.")
        } catch (e: Exception) {
            ApiResult.Error(message = e.message ?: "Unexpected error")
        }
    }
}
