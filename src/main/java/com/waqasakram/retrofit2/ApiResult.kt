package com.waqasakram.retrofit2

import java.io.IOException
sealed class ApiResult<out T>{
    data class Success<T>(val result:T): ApiResult<T>()
    object StartRequest: ApiResult<Nothing>()
    object EndRequest : ApiResult<Nothing>()
    object EmptyResult: ApiResult<Nothing>()

    sealed class Error(val error: Throwable): ApiResult<Nothing>(){

        sealed class ClientError(error: Throwable): Error(error){
            class UnAuthorized(error: Throwable): ClientError(error)
            class BadRequest(error: Throwable): ClientError(error)
            class Forbidden(error: Throwable): ClientError(error)
            class NotFound(error: Throwable): ClientError(error)
            class InternetConnection(error: Throwable): ClientError(error)
        }

        //server errors will extend in future

        sealed class ServerError(error: Throwable? = null): Error(error ?: IOException("Server Error")){
            object Internal: ServerError()
            object RequestNotImplemented: ServerError()
            object BadGateway: ServerError()
            object ServiceUnavailable: ServerError()
        }
        class UnHandled(error:Throwable): Error(error)
    }

}