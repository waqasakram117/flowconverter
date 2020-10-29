package com.waqasakram.retrofit2.adapter

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.lang.reflect.Type

@ExperimentalCoroutinesApi
class FlowAdapter<T>(private val responseType: Type, private val dispatcher: CoroutineDispatcher): CallAdapter<T, Flow<T>> {
    override fun responseType(): Type =  responseType

    override fun adapt(call: Call<T>): Flow<T> = callbackFlow {

        withContext(dispatcher){
            call.enqueue(object : Callback<T> {
                override fun onResponse(call: Call<T>, response: Response<T>) {
                    if (response.isSuccessful){
                        sendBlocking(response.body())
                        close()
                    }else{
                        val error = response.errorBody()?.string() ?: "Unknown Error"
                        close(IOException("Error code: ${response.code()} \n $error"))
                    }
                }
                override fun onFailure(call: Call<T>, t: Throwable) {
                    close(t)
                }
            })
        }

        awaitClose()
    }

}
