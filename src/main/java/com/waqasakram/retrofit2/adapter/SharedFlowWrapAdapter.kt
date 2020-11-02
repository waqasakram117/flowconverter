package com.waqasakram.retrofit2.adapter

import com.waqasakram.retrofit2.ApiResult
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.lang.reflect.Type
import java.net.ConnectException

/**
 * This adapter wraps [Call] into [SharedFlow]<[ApiResult]<[T]>>
 * @param responseType is final inner [T] type of Api call e.g [String], User, Movie etc
 * @param dispatcher is [CoroutineDispatcher] normally it is default [Dispatchers.IO]
 */

@ExperimentalCoroutinesApi
class SharedFlowWrapAdapter<T>(private val responseType: Type, private val dispatcher: CoroutineDispatcher)
    : CallAdapter<T, SharedFlow<ApiResult<T>>> {

    /**
     * @return type of final inner return type of api here is type of [T] e.g [String], User, Movie etc
     */
    override fun responseType(): Type = responseType

    /**
     * It launches separate coroutine for each call and emit must three results [ApiResult.StartRequest],
     * [ApiResult.Success] or [ApiResult.Error] and [ApiResult.EndRequest]
     * @return [SharedFlow] of [ApiResult] contains return of [T]
     */
    override fun adapt(call: Call<T>): SharedFlow<ApiResult<T>> {
        val flow = MutableSharedFlow<ApiResult<T>>(replay = 1, extraBufferCapacity = 1)
        fun sendError(error: ApiResult.Error) {
            flow.tryEmit(error)
            flow.tryEmit(ApiResult.EndRequest)
        }
        CoroutineScope(dispatcher).launch {
            flow.tryEmit(ApiResult.StartRequest)
            call.enqueue(object : Callback<T> {
                override fun onResponse(call: Call<T>, response: Response<T>) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body != null) {
                            flow.tryEmit(ApiResult.Success(body))
                        } else {
                            flow.tryEmit(ApiResult.EmptyResult)
                        }
                        flow.tryEmit(ApiResult.EndRequest)
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
        }
        return flow.asSharedFlow()
    }

}

/**
 * [SharedFlowWrapAdapter.adapt] has guarantee it will always sent three states in each request [ApiResult.StartRequest],
 * [ApiResult.Success] or [ApiResult.Error] and [ApiResult.EndRequest] so this extension function will terminate [SharedFlow]
 * after each api call.
 * Note: Without operator fusion [Flow.take] [Flow.takeWhile] etc SharedFlow will always remain open
 *
 */
suspend inline fun <T> SharedFlow<ApiResult<T>>.collectResult(crossinline block: suspend (ApiResult<T>) -> Unit) {
    take(3).collect(block)
}

/**
 * [SharedFlowWrapAdapter.adapt] has guarantee it will always sent three states in each request [ApiResult.StartRequest],
 * [ApiResult.Success] or [ApiResult.Error] and [ApiResult.EndRequest] so this extension function will terminate [SharedFlow]
 * after [ApiResult.StartRequest],[ApiResult.Success] or [ApiResult.Error]
 * Note: Without operator fusion [Flow.take] [Flow.takeWhile] etc SharedFlow will always remain open
 */
suspend inline fun <T> SharedFlow<ApiResult<T>>.collectUnitEnd(crossinline block: suspend (ApiResult<T>) -> Unit) {
    takeWhile { it !is ApiResult.EndRequest }.collect(block)
}