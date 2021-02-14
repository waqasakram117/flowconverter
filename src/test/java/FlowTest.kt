import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.waqasakram.retrofit2.ApiResult
import com.waqasakram.retrofit2.factory.FlowConverterFactory
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

import java.util.concurrent.TimeUnit


/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */

class FlowTest {

    lateinit var server: MockWebServer

    lateinit var service: PlaceHolderService

    @Before
    fun initServer() {
        server = MockWebServer()
        service = Retrofit.Builder()
                .baseUrl(server.url("/"))
                .addCallAdapterFactory(FlowConverterFactory())
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(PlaceHolderService::class.java)
    }

    @Test
    fun testFlowWithApiResult() {

        server.enqueue(
                MockResponse()
                        .setResponseCode(200)
                        .setBodyDelay(1, TimeUnit.SECONDS)
                        .setBody(response))

        runBlocking {
            val call = service.getFlowApiResultStringTodos()
            call.withIndex().collect {
                if (it.index == 0) assert(it.value is ApiResult.StartRequest)
                else if (it.index == 1) assert(it.value is ApiResult.Success)
                if (it.index == 2) assert(it.value is ApiResult.EndRequest)
            }
        }

    }

    @Test
    fun testFlowWithApiToDoResult() {

        val listType = object : TypeToken<List<TodoResult?>?>() {}.type
        val list = Gson().fromJson<List<TodoResult>>(response, listType)
        server.enqueue(
                MockResponse()
                        .setResponseCode(200)
                        .setBodyDelay(1, TimeUnit.SECONDS)
                        .setBody(response))

        runBlocking {
            val call = service.getFlowApiResultTodos()
            call.withIndex().collect {
                if (it.index == 0) assert(it.value is ApiResult.StartRequest)
                else if (it.index == 1) {
                    assert(it.value is ApiResult.Success)
                    assert((it.value as ApiResult.Success).result.size == list.size)
                    assert((it.value as ApiResult.Success).result.first() == list.first())
                }
                if (it.index == 2) assert(it.value is ApiResult.EndRequest)
            }
        }

    }

    @Test
    fun clientErrorTestFlowWithApiResult() {

        server.enqueue(
                MockResponse()
                        .setResponseCode(401)
                        .setBodyDelay(1, TimeUnit.SECONDS)
                        .setBody(response))

        runBlocking {
            val call = service.getFlowApiResultStringTodos()
            call.withIndex().collect {
                if (it.index == 0) assert(it.value is ApiResult.StartRequest)
                else if (it.index == 1) assert(it.value is ApiResult.Error.ClientError)
                if (it.index == 2) assert(it.value is ApiResult.EndRequest)
            }
        }

    }

    @Test
    fun clientErrorTestFlowWithApiResulft() {

        server.enqueue(
                MockResponse()
                        .setResponseCode(401)
                        .setBodyDelay(1, TimeUnit.SECONDS)
                        .setBody(response))

        runBlocking {
            val call = service.getFlowApiResultStringTodos()
            call.collect {
                when (it) {
                    is ApiResult.Success -> {
                        it.result // This is result
                    }
                    is ApiResult.StartRequest -> TODO()
                    is ApiResult.EndRequest -> TODO()
                    is ApiResult.EmptyResult -> TODO()
                    is ApiResult.Error -> {
                        when (it) {
                            is ApiResult.Error.ClientError -> {
                                when (it) {
                                    is ApiResult.Error.ClientError.UnAuthorized -> TODO()
                                    is ApiResult.Error.ClientError.BadRequest -> TODO()
                                    is ApiResult.Error.ClientError.Forbidden -> TODO()
                                    is ApiResult.Error.ClientError.NotFound -> TODO()
                                    is ApiResult.Error.ClientError.InternetConnection -> TODO()
                                }
                            }
                            is ApiResult.Error.ServerError -> {
                                when (it) {
                                    ApiResult.Error.ServerError.Internal -> TODO()
                                    ApiResult.Error.ServerError.RequestNotImplemented -> TODO()
                                    ApiResult.Error.ServerError.BadGateway -> TODO()
                                    ApiResult.Error.ServerError.ServiceUnavailable -> TODO()
                                }
                            }
                            is ApiResult.Error.UnHandled -> TODO()
                        }
                    }

                }
            }
        }

    }

    @Test
    fun testPlainFlow() {

        server.enqueue(
                MockResponse()
                        .setResponseCode(200)
                        .setBodyDelay(1, TimeUnit.SECONDS)
                        .setBody(response))

        runBlocking {
            val call = service.getFlow()
            assert(call.first() == response)
        }

    }


    @After
    fun shutdown() {

        server.shutdown()
    }
}