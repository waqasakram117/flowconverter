import com.waqasakram.retrofit2.ApiResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import retrofit2.http.GET

interface PlaceHolderService {

    @GET("/todos/")
    fun getFlowApiResultStringTodos(): Flow<ApiResult<String>>

    @GET("/todos/")
    fun getFlowApiResultTodos(): Flow<ApiResult<List<TodoResult>>>

    @GET("/todos/")
    fun getFlow(): Flow<String>

    @GET("/todos/")
    fun getSharedFlowApiResultStringTodos(): SharedFlow<ApiResult<String>>

    @GET("/todos/")
    fun getSharedFlowApiResultTodos(): SharedFlow<ApiResult<List<TodoResult>>>


    @GET("/todos/")
    fun getSharedFlowStringTodos(): SharedFlow<String>
}