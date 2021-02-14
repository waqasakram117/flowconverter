package com.waqasakram.retrofit2.factory

import com.waqasakram.retrofit2.ApiResult
import com.waqasakram.retrofit2.adapter.FlowAdapter
import com.waqasakram.retrofit2.adapter.FlowWrapAdapter
import com.waqasakram.retrofit2.adapter.SharedFlowAdapter
import com.waqasakram.retrofit2.adapter.SharedFlowWrapAdapter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Retrofit
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * Factory converts incoming [Flow] calls into dedicated [CallAdapter]
 * @param dispatcher used to [Call.enqueue] default is [Dispatchers.IO]
 */
@ExperimentalCoroutinesApi
class FlowConverterFactory(private val dispatcher: CoroutineDispatcher = Dispatchers.IO) : CallAdapter.Factory() {

    override fun get(returnType: Type, annotations: Array<Annotation>, retrofit: Retrofit):
            CallAdapter<*, *>? {
        if (returnType !is ParameterizedType) return null // This is plain return type e.g String, User, Student etc
        val responseType = getParameterUpperBound(0, returnType)
        if (getRawType(returnType) == Flow::class.java) {//use only Flow
            if (responseType !is ParameterizedType) {
                return FlowAdapter<Any>(responseType, dispatcher)
            } else if (getRawType(responseType) == ApiResult::class.java) {//use Flow with [ApiResult]
                val response = getParameterUpperBound(0, responseType)
                return FlowWrapAdapter<Any>(response, dispatcher)
            }
        } else if (getRawType(returnType) == SharedFlow::class.java) {
            if (responseType !is ParameterizedType) {
                return SharedFlowAdapter<Any>(responseType, dispatcher)
            } else if (getRawType(responseType) == ApiResult::class.java) {// use SharedFlow with [ApiResult]
                val response = getParameterUpperBound(0, responseType)
                return SharedFlowWrapAdapter<Any>(response, dispatcher)
            }
        }
        return null
    }
}
