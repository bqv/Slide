package ltd.ucode.network.lemmy.api

interface NetworkException {
    val path: String
    val statusCode: Int
    val errorBody: String?

    fun rethrow(): Nothing
    fun upcast(): Exception
}
