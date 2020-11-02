package com.waqasakram.retrofit2

import java.io.IOException

/**
 * Api Wrapper to encapsulate Api request, result, error and termination
 */
sealed class ApiResult<out T>{
    data class Success<T>(val result:T): ApiResult<T>()
    object StartRequest: ApiResult<Nothing>()
    object EndRequest : ApiResult<Nothing>()
    object EmptyResult: ApiResult<Nothing>()

    /**
     * Encapsulate all Errors including http and IO
     */
    sealed class Error(val error: Throwable): ApiResult<Nothing>(){

        /**
         * Client related Http and IO Errors
         */
        sealed class ClientError(error: Throwable): Error(error){
            class UnAuthorized(error: Throwable): ClientError(error)
            class BadRequest(error: Throwable): ClientError(error)
            class Forbidden(error: Throwable): ClientError(error)
            class NotFound(error: Throwable): ClientError(error)
            class InternetConnection(error: Throwable): ClientError(error)
        }

        /**
         * Server related Http errors
         * These will extend in future
         */

        sealed class ServerError(error: Throwable? = null): Error(error ?: IOException("Server Error")){
            object Internal: ServerError()
            object RequestNotImplemented: ServerError()
            object BadGateway: ServerError()
            object ServiceUnavailable: ServerError()
        }

        /**
         * Generic error wrapper mostly uses when error is neither categories in [ClientError] nor [ServerError]
         */
        class UnHandled(error:Throwable): Error(error)
    }

}