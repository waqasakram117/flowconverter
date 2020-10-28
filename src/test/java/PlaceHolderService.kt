import kotlinx.coroutines.flow.Flow

import retrofit2.http.GET


interface PlaceHolderService {
    @GET("/todos/")
    fun getTodos(): Flow<ApiResult<String>>
}