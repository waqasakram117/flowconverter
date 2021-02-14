### Flowconverter
 Retrofit2 call adapter factory enables use of Kotlin [Flow](https://kotlinlang.org/docs/reference/coroutines/flow.html) and [SharedFlow](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-shared-flow/)
 
## Usage

```kotlin
val retrofit = Retrofit.Builder()
    .baseUrl("https://example.com/")
    .addCallAdapterFactory(FlowConverterFactory()) // Here is Flow Converter Factory
    .addConverterFactory(ScalarsConverterFactory.create())
    .addConverterFactory(GsonConverterFactory.create())
    .build()
```
After integration you can use 
```
    @GET("/todos/")
    fun getFlowStringTodos(): Flow<String>
```
But make sure you have to handle error ownself

```
service.getFlowStringTodos().catch{e->
//error captured in case of server, client or others
}.collect{
//result in string
}
```
## ApiResult Wrapper
You can use ApiResult<T> wrapper
```
    @GET("/todos/")
    fun getFlowApiResultStringTodos(): Flow<ApiResult<String>>
```
Then everything is handle and catagroies
```
service.getFlowStringTodos().collect {
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
```
You can use [SharedFlow](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-shared-flow/) as well
```
fun getFlowApiResultStringTodos(): SharedFlow<ApiResult<String>>
```

# Examples
```
fun getTodos(): Flow<String>
fun getTodos(): SharedFlow<String>
fun getTodos(): Flow<ApiResult<List<Todo>>>
fun getTodos(): SharedFlow<ApiResult<List<Todo>>>
fun getTodos(): Flow<ApiResult<String>>
fun getTodos(): SharedFlow<ApiResult<String>>
```
## Download
Download via Gradle:

```
implementation 'com.waqasakram.retrofit2:flowconverter:Latest Release'
```

For Maven
```
<dependency>
	<groupId>com.waqasakram.retrofit2</groupId>
	<artifactId>flowconverter</artifactId>
	<version>Latest Release</version>
	<type>pom</type>
</dependency>
```

## License

```
Copyright 2020 Waqas Akram

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
