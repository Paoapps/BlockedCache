package com.paoapps.blockedcache

/**
 * Represents the result of a fetching operation, encapsulating both successful and error outcomes.
 * This sealed class allows for a typed distinction between success data and different forms of errors.
 *
 * @param Network The type of data expected on a successful fetch.
 */
sealed class FetcherResult<out Network : Any> {

    /**
     * Represents a successful fetch operation, containing the fetched data.
     *
     * @property value The data fetched from the network or other sources.
     * @property origin Optional string to describe the origin of the data (e.g., cache, network).
     */
    data class Data<Network : Any>(val value: Network, val origin: String? = null) : FetcherResult<Network>()

    /**
     * Represents an error outcome of a fetch operation.
     */
    sealed class Error(open val code: Int? = null) : FetcherResult<Nothing>() {

        val errorMessage: String?
            get() = when (this) {
                is Exception -> error.message
                is Message -> message
            }

        /**
         * Represents an error with an exception.
         *
         * @property error The throwable exception that caused the error.
         */
        data class Exception(val error: Throwable, override val code: Int? = null) : Error()

        /**
         * Represents an error with a specific message.
         *
         * @property message The error message.
         */
        data class Message(val message: String, override val code: Int? = null) : Error()
    }
}

/**
 * Extension property to get the throwable of a [FetcherResult.Error].
 * Returns the throwable if the error is an Exception type, or null if it's a Message type.
 */
internal val FetcherResult.Error.throwable: Throwable?
    get() = when (this) {
        is FetcherResult.Error.Exception -> error
        is FetcherResult.Error.Message -> null
    }

/**
 * Transforms the successful data of the fetch result using the provided transformation function.
 *
 * @param T The original type of the successful data.
 * @param R The new type after the transformation.
 * @param transform A transformation function to apply to the successful data.
 * @return A new [FetcherResult] with the transformed data if successful, or the original error.
 */
inline fun <T: Any, R: Any> FetcherResult<T>.map(transform: (T) -> R): FetcherResult<R> {
    return when (this) {
        is FetcherResult.Data -> {
            FetcherResult.Data(transform(value), origin)
        }
        is FetcherResult.Error -> {
            map()
        }
    }
}

/**
 * Transforms the successful data of the fetch result to another [FetcherResult] using the provided transformation function.
 *
 * @param T The original type of the successful data.
 * @param R The type of the new [FetcherResult].
 * @param transform A transformation function that returns a new [FetcherResult].
 * @return A new [FetcherResult] based on the transformation of the successful data, or the original error.
 */
inline fun <T: Any, R: Any> FetcherResult<T>.flatMap(transform: (T) -> FetcherResult<R>): FetcherResult<R> {
    return when (this) {
        is FetcherResult.Data -> {
            transform(value)
        }
        is FetcherResult.Error -> {
            map()
        }
    }
}

/**
 * Checks if the successful data of the fetch result is empty.
 *
 * @param T The type of the successful data.
 * @return True if the data is a list and is empty, false otherwise or if it's an error.
 */
fun <T: Any> FetcherResult<T>.isEmpty(): Boolean {
    return when (this) {
        is FetcherResult.Data -> {
            value is List<*> && value.isEmpty()
        }
        is FetcherResult.Error -> {
            false
        }
    }
}

/**
 * Maps an [FetcherResult.Error] to another [FetcherResult.Error].
 * Essentially, this function can be used to transform one error type to another.
 *
 * @return A new instance of [FetcherResult.Error] with the same error details.
 */
fun FetcherResult.Error.map(): FetcherResult.Error = when(this) {
    is FetcherResult.Error.Exception -> FetcherResult.Error.Exception(error, code)
    is FetcherResult.Error.Message -> FetcherResult.Error.Message(message, code)
}
