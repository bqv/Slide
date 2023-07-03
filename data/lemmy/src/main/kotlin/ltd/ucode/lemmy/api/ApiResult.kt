package ltd.ucode.lemmy.api

sealed interface ApiResult<T> {
    val instance: String

    class Success<T>(override val instance: String, val data: T) : ApiResult<T> {}
    class Failed<T>(override val instance: String, val exception: NetworkException) : ApiResult<T> {
        fun <R> cast(): Failed<R> = Failed(instance, exception)
    }

    val successOrNull: T?
        get() {
            return when (this) {
                is Failed -> { null }
                is Success -> { this.data }
            }
        }

    val success: T
        get() {
            return when (this) {
                is Failed -> { this.exception.rethrow() }
                is Success -> { this.data }
            }
        }

    suspend fun onSuccess(block: suspend (T) -> Unit): ApiResult<T> {
        when (this) {
            is Failed -> { }
            is Success -> { block(this.data) }
        }
        return this
    }

    fun <R> mapSuccess(block: Success<T>.() -> R): ApiResult<R> {
        return when (this) {
            is Failed -> { this.cast() }
            is Success -> { Success(instance, block(this)) }
        }
    }

    suspend fun <R> bindSuccess(block: suspend Success<T>.() -> ApiResult<R>): ApiResult<R> {
        return when (this) {
            is Failed -> { this.cast() }
            is Success -> { block(this) }
        }
    }

    fun mapFailure(block: Failed<T>.() -> Unit): ApiResult<T> {
        when (this) {
            is Failed -> { block(this) }
            is Success -> { }
        }
        return this
    }

    suspend fun onFailure(block: suspend Failed<T>.() -> Unit): ApiResult<T> {
        when (this) {
            is Failed -> { block(this) }
            is Success -> { }
        }
        return this
    }

    suspend fun or(block: suspend ApiResult<T>.() -> ApiResult<T>): ApiResult<T> {
        return when (this) {
            is Failed -> { block(this) }
            is Success -> { this }
        }
    }
}
