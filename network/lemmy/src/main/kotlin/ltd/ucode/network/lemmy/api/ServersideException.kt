package ltd.ucode.network.lemmy.api

data class ServersideException(override val path: String,
                               override val statusCode: Int,
                               @Transient override val errorBody: String? = null
) : Exception("HTTP $statusCode"), NetworkException {
    override fun rethrow(): Nothing {
        throw this
    }

    override fun upcast(): Exception {
        return this
    }
}
