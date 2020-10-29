package com.waqasakram.retrofit2.adapter
import com.waqasakram.retrofit2.ApiResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CoroutineDispatcher
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
import java.net.ConnectException

@ExperimentalCoroutinesApi
class WrappedAdapter<T>(private val responseType: Type, private val dispatcher: CoroutineDispatcher) : CallAdapter<T, Flow<ApiResult<T>>>{

    override fun responseType(): Type = responseType

    override fun adapt(call: Call<T>): Flow<ApiResult<T>> = callbackFlow {

        fun sendError(error: ApiResult.Error){
            sendBlocking(error)
            sendBlocking(ApiResult.EndRequest)
            close(error.error)
        }
        withContext(dispatcher){
            sendBlocking(ApiResult.StartRequest)
            call.enqueue(object : Callback<T> {
                override fun onResponse(call: Call<T>, response: Response<T>) {

                    if (response.isSuccessful){
                        val body = response.body()
                        if (body != null){
                            sendBlocking(ApiResult.Success<T>(body))
                        }else{
                            sendBlocking(ApiResult.EmptyResult)
                        }
                        sendBlocking(ApiResult.EndRequest)
                        close()
                    }else{

                        val exception = IOException(response.errorBody()?.string() ?: "UnKnown Error with error code: ${response.code()}")
                        val error = categoriesErrors(response.code(), exception)
                        sendError(error)
                    }

                }
                override fun onFailure(call: Call<T>, throwable: Throwable) {
                    when(throwable){
                        is IOException, is ConnectException ->
                            sendError(ApiResult.Error.ClientError.InternetConnection(throwable))
                        else -> sendError(ApiResult.Error.UnHandled(throwable))
                    }
                }
            })
        }

        awaitClose()
    }


    fun categoriesErrors(code:Int, throwable:Throwable): ApiResult.Error = when(code){

        in 500..600 ->{
            when(code){
                500 -> ApiResult.Error.ServerError.Internal
                501 -> ApiResult.Error.ServerError.RequestNotImplemented
                502 -> ApiResult.Error.ServerError.BadGateway
                503 -> ApiResult.Error.ServerError.ServiceUnavailable
                else -> ApiResult.Error.UnHandled(throwable)
            }
        }
        in 400..500 ->{
            when(code){
                400 -> ApiResult.Error.ClientError.BadRequest(throwable)
                401 -> ApiResult.Error.ClientError.UnAuthorized(throwable)
                403 -> ApiResult.Error.ClientError.Forbidden(throwable)
                404 -> ApiResult.Error.ClientError.NotFound(throwable)
                else -> ApiResult.Error.UnHandled(throwable)
            }
        }
        else -> ApiResult.Error.UnHandled(throwable)
    }


}
