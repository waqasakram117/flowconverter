package com.waqasakram.retrofit2.factory

import com.waqasakram.retrofit2.ApiResult
import com.waqasakram.retrofit2.adapter.FlowAdapter
import com.waqasakram.retrofit2.adapter.WrappedAdapter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import retrofit2.*
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

@ExperimentalCoroutinesApi
class FlowConverterFactory(private val dispatcher: CoroutineDispatcher = Dispatchers.IO): CallAdapter.Factory() {

    override fun get(returnType: Type, annotations: Array<Annotation>, retrofit: Retrofit):
            CallAdapter<*, *>? {

        if (getRawType(returnType) ==  Flow::class.java){//use only Flow
            val responseType = getParameterUpperBound(0, (returnType as ParameterizedType))
            if (responseType !is ParameterizedType){
                return FlowAdapter<Any>(responseType,dispatcher)
            }else if (getRawType(responseType)  == ApiResult::class.java){//use Flow with com.waqasakram.retrofit2.ApiResult
                val response = getParameterUpperBound(0, responseType)
                return WrappedAdapter<Any>(response,dispatcher)
            }
        }
        return null
    }
}
