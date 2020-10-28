
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import retrofit2.*
import java.io.IOException
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.net.ConnectException

@ExperimentalCoroutinesApi
class FlowConverterFactory(private val dispatcher: CoroutineDispatcher = Dispatchers.IO): CallAdapter.Factory() {

    override fun get(returnType: Type, annotations: Array<Annotation>, retrofit: Retrofit):
            CallAdapter<*, *>? {

        if (getRawType(returnType) ==  Flow::class.java){//use only Flow
            val responseType = getParameterUpperBound(0, (returnType as ParameterizedType))
            if (responseType !is ParameterizedType){
                return FlowAdapter<Any>(responseType,dispatcher)
            }else if (getRawType(responseType)  == ApiResult::class.java){//use Flow with ApiResult
                val response = getParameterUpperBound(0, responseType)
                return WrappedAdapter<Any>(response,dispatcher)
            }
        }
        return null
    }
}

@ExperimentalCoroutinesApi
class WrappedAdapter<T>(private val responseType: Type, private val dispatcher: CoroutineDispatcher) : CallAdapter<T, Flow<ApiResult<T>>>{

    override fun responseType(): Type = responseType

    override fun adapt(call: Call<T>): Flow<ApiResult<T>> = callbackFlow {

        fun sendError(error:ApiResult.Error){
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


    fun categoriesErrors(code:Int, throwable:Throwable):ApiResult.Error = when(code){

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

@ExperimentalCoroutinesApi
class FlowAdapter<T>(private val responseType: Type, private val dispatcher: CoroutineDispatcher):CallAdapter<T, Flow<T>>{
    override fun responseType(): Type =  responseType

    override fun adapt(call: Call<T>): Flow<T> = callbackFlow {

        withContext(dispatcher){
            call.enqueue(object :Callback<T>{
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