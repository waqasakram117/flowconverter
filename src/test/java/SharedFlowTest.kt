import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.waqasakram.retrofit2.ApiResult
import com.waqasakram.retrofit2.adapter.collectResult
import com.waqasakram.retrofit2.factory.FlowConverterFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

class SharedFlowTest {

    lateinit var server: MockWebServer

    lateinit var service: PlaceHolderService

    @Before
    fun initServer() {
        server = MockWebServer()
        service = Retrofit.Builder()
                .baseUrl(server.url("/"))
                .addCallAdapterFactory(FlowConverterFactory(dispatcher = newSingleThreadContext("testing")))
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(PlaceHolderService::class.java)
    }

    @Test
    fun testSharedFlowWithApiResult() {

        server.enqueue(
                MockResponse()
                        .setResponseCode(200)
                        .setBodyDelay(1, TimeUnit.SECONDS)
                        .setBody(response))

        runBlocking {
            val call = service.getSharedFlowApiResultStringTodos()
            var result = 0
            call.collectResult {
                if (result == 0) assert(it is ApiResult.StartRequest)
                else if (result == 1) assert(it is ApiResult.Success)
                if (result == 2) assert(it is ApiResult.EndRequest)
                result++
            }

        }

        assert(true)
    }

    @Test
    fun testSharedFlowWithApiResultWithCollect() {

        server.enqueue(
                MockResponse()
                        .setResponseCode(200)
                        .setBodyDelay(1, TimeUnit.SECONDS)
                        .setBody(response))

        runBlocking {
            val call = service.getSharedFlowApiResultStringTodos()
            var result = 0

            val job = launch {
                call.collect {
                    if (result == 0) assert(it is ApiResult.StartRequest)
                    else if (result == 1) assert(it is ApiResult.Success)
                    if (result == 2) assert(it is ApiResult.EndRequest)
                    result++
                }
                //program will never reach here because sharedFlow always collect
                assert(false)
            }
            //3_000 enough wait for collecting result without it this test always in running mode because of SharedFlow
            delay(3000)
            job.cancelAndJoin()
        }

    }


    @Test
    fun testSharedFlowWithTodoApiResult() {

        val listType = object : TypeToken<List<TodoResult?>?>() {}.type
        val list = Gson().fromJson<List<TodoResult>>(response, listType)
        server.enqueue(
                MockResponse()
                        .setResponseCode(200)
                        .setBodyDelay(1, TimeUnit.SECONDS)
                        .setBody(response))

        runBlocking {
            val call = service.getSharedFlowApiResultTodos()
            var result = 0
            call.collectResult {
                if (result == 0) assert(it is ApiResult.StartRequest)
                else if (result == 1) {
                    assert(it is ApiResult.Success)
                    assert((it as ApiResult.Success).result.size == list.size)
                    assert((it as ApiResult.Success).result.first() == list.first())
                }
                if (result == 2) assert(it is ApiResult.EndRequest)
                result++
            }

        }

        assert(true)
    }

    @Test
    fun testPlainSharedFlow(){
        server.enqueue(
                MockResponse()
                        .setResponseCode(200)
                        .setBodyDelay(1, TimeUnit.SECONDS)
                        .setBody(response))

        runBlocking {
            val call = service.getSharedFlowStringTodos()
            assert(call.first() == response)
        }

}


    @Test
    fun testPlainSharedFlowWithError(){

        server.enqueue(
                MockResponse()
                        .setResponseCode(400)
                        .setBodyDelay(1, TimeUnit.SECONDS)
                        .setBody(response))

        runBlocking {
             val call = service.getSharedFlowStringTodos()
                print(call.first())

        }

}


    @After
    fun shutdown() {
        server.shutdown()
    }
}