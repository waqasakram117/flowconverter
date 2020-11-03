package com.waqasakram.retrofit2.adapter

import com.waqasakram.retrofit2.ApiResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.lang.reflect.Type
import java.net.ConnectException

/**
 *This adapter wraps [Call] into [Flow]<[ApiResult]<[T]>>
 * @param responseType is final inner [T] type of Api call e.g [String], User, Movie etc
 * @param dispatcher is [CoroutineDispatcher] normally it is default [Dispatchers.IO]
 */
@ExperimentalCoroutinesApi
class FlowWrapAdapter<T>(private val responseType: Type, private val dispatcher: CoroutineDispatcher) : CallAdapter<T, Flow<ApiResult<T>>> {
    /**
     * @return type of final inner return type of api here is type of [T] e.g [String], User, Movie etc
     */
    override fun responseType(): Type = responseType

    /**
     * It creates  flow for each call and emit must three results [ApiResult.StartRequest],
     * [ApiResult.Success] or [ApiResult.Error] and [ApiResult.EndRequest] or close [Flow] with [IOException] and
     * launch on [dispatcher] via [Flow.flowOn]
     * @return [Flow] of [ApiResult] contains return of [T]
     */
    override fun adapt(call: Call<T>) = callbackFlow<ApiResult<T>> {

        fun sendError(error: ApiResult.Error) {
            sendBlocking(error)
            sendBlocking(ApiResult.EndRequest)
            close()// success close because exception already wrapped in sendBlocking
        }
        sendBlocking(ApiResult.StartRequest)
        call.enqueue(object : Callback<T> {
            override fun onResponse(call: Call<T>, response: Response<T>) {

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        sendBlocking(ApiResult.Success<T>(body))
                    } else {
                        sendBlocking(ApiResult.EmptyResult)
                    }
                    sendBlocking(ApiResult.EndRequest)
                    close()
                } else {
                    val exception = IOException(response.errorBody()?.string()
                            ?: "UnKnown Error with error code: ${response.code()}")
                    val error = response.code().categoriesErrors(exception)
                    sendError(error)
                }

            }

            override fun onFailure(call: Call<T>, throwable: Throwable) {
                when (throwable) {
                    is IOException, is ConnectException ->
                        sendError(ApiResult.Error.ClientError.InternetConnection(throwable))
                    else -> sendError(ApiResult.Error.UnHandled(throwable))
                }
            }
        })
        awaitClose()
    }.flowOn(dispatcher)

}

/**
 * Extension utility categories Http error into [ApiResult.Error.ClientError] and [ApiResult.Error.ServerError] with [Throwable]
 * It will extend in future
 * @param throwable exception
 */

fun Int.categoriesErrors(throwable: Throwable): ApiResult.Error = when (this) {

    in 500..600 -> {
        when (this) {
            500 -> ApiResult.Error.ServerError.Internal
            501 -> ApiResult.Error.ServerError.RequestNotImplemented
            502 -> ApiResult.Error.ServerError.BadGateway
            503 -> ApiResult.Error.ServerError.ServiceUnavailable
            else -> ApiResult.Error.UnHandled(throwable)
        }
    }
    in 400..500 -> {
        when (this) {
            400 -> ApiResult.Error.ClientError.BadRequest(throwable)
            401 -> ApiResult.Error.ClientError.UnAuthorized(throwable)
            403 -> ApiResult.Error.ClientError.Forbidden(throwable)
            404 -> ApiResult.Error.ClientError.NotFound(throwable)
            else -> ApiResult.Error.UnHandled(throwable)
        }
    }
    else -> ApiResult.Error.UnHandled(throwable)
}