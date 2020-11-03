package com.waqasakram.retrofit2.adapter

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

/**
 *This adapter wraps [Call] into [Flow]<[T]>
 * @param responseType is final inner [T] type of Api call e.g [String], User, Movie etc
 * @param dispatcher is [CoroutineDispatcher] normally it is default [Dispatchers.IO]
 */
@ExperimentalCoroutinesApi
class FlowAdapter<T>(private val responseType: Type, private val dispatcher: CoroutineDispatcher) : CallAdapter<T, Flow<T>> {
    /**
     * @return type of final inner return type of api here is type of [T] e.g [String], User, Movie etc
     */
    override fun responseType(): Type = responseType

    /**
     * It launches separate coroutine for each call and emit result [T] or close [Flow] with [IOException] and
     * launch on [dispatcher] via [Flow.flowOn]
     * @return [Flow] contains [T]
     */
    override fun adapt(call: Call<T>) = callbackFlow<T> {
        call.enqueue(object : Callback<T> {
            override fun onResponse(call: Call<T>, response: Response<T>) {
                if (response.isSuccessful) {
                    val result = response.body()
                    if (result != null) {
                        sendBlocking(result)
                        close()
                    } else {
                        val error = "Result null returned"
                        close(IOException("Error code: ${response.code()} \n $error"))
                    }
                } else {
                    val error = response.errorBody()?.string() ?: "Unknown Error"
                    close(IOException("Error code: ${response.code()} \n $error"))
                }
            }

            override fun onFailure(call: Call<T>, t: Throwable) {
                close(t)
            }
        })
        awaitClose()
    }.flowOn(dispatcher)

}
