package com.waqasakram.retrofit2.adapter

import com.waqasakram.retrofit2.ApiResult
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.*
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.lang.reflect.Type
import java.net.ConnectException
import kotlin.coroutines.CoroutineContext

/**
 *This adapter wraps [Call] into [Flow]<[T]>
 * @param responseType is final inner [T] type of Api call e.g [String], User, Movie etc
 * @param dispatcher is [CoroutineDispatcher] normally it is default [Dispatchers.IO]
 */
@ExperimentalCoroutinesApi
class SharedFlowAdapter<T>(private val responseType: Type, private val dispatcher: CoroutineDispatcher) : CallAdapter<T, SharedFlow<T?>> {

    /**
     * @return type of final inner return type of api here is type of [T] e.g [String], User, Movie etc
     */
    override fun responseType(): Type = responseType

    /**
     * It launches separate coroutine for each call and emit result [T] or close [Flow] with [IOException] and
     * launch on [dispatcher] via [Flow.flowOn]
     * @return [Flow] contains [T]
     */
    override fun adapt(call: Call<T>): SharedFlow<T?> {
        return callbackFlow<T?> {
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
        }.catch {
                   this.emit(null)
                }.shareIn(CoroutineScope(dispatcher), SharingStarted.Lazily, 1)
    }
}

suspend inline fun<T> SharedFlow<T?>.takeResult(crossinline block:suspend (T) -> Unit){
   takeWhile{
       it != null
   }.collect {
       block(it!!)
   }
}
