# Kotlin Multiplatform Blocked Cache Library

## Overview
Kotlin Multiplatform Blocked Cache Library is a versatile caching solution designed for Kotlin Multiplatform projects. It provides robust mechanisms for data caching with features like automatic refresh, expiry handling, and network status awareness.

## Features
- **Multiplatform Compatibility**: Works seamlessly across multiple platforms.
- **Automatic Cache Refresh**: Configurable refresh times for automatic data updates.
- **Data Expiry**: Optional expiry times for cached data.
- **Network Status Awareness**: Adjusts caching behavior based on network availability.
- **Flexible Fetching Strategy**: Customizable data fetching logic.
- **Thread-Safe Operations**: Ensures safe concurrent access to cached data.

## Concept

Blocked Cache is designed to share api resources between multiple classes, such as view models,
and stream data updates to them. It uses a source of truth to get cached data and update it. Any
update to that data will be streamed to all subscribers.

By using a refresh time, the cache will update itself after a certain amount of time when the Flow
is subscribed to. While it is loading, the cache will return the last cached data.

## Installation
Include the library in your project's build file:

```kotlin
dependencies {
    implementation("com.paoapps.blockedcache:blocked-cache:0.0.7-SNAPSHOT")
}
```

## Publishing Setup

To publish this library to Maven Central, you'll need to configure your credentials securely. **Never commit credentials to version control.**

### Central Portal User Token (New Method - Required)
Sonatype has migrated to the Central Portal. You now need a **User Token** instead of username/password:

1. Go to [Central Portal](https://central.sonatype.com/)
2. Sign in with your Sonatype account
3. Navigate to "Account" → "User Token"
4. Generate a new User Token
5. Use the token values below

### Option 1: Local Properties (Recommended)
Create or update `local.properties` in the project root (this file is gitignored):

```properties
# Central Portal User Token (get from https://central.sonatype.com/account)
ossrhUsername=your-user-token-username
ossrhPassword=your-user-token-password

# GPG Signing
signing.keyId=your-gpg-key-id
signing.password=your-gpg-password
signing.secretKeyRingFile=/path/to/secring.gpg
```

### Option 2: Environment Variables
Set the following environment variables:

```bash
export OSSRH_USERNAME=your-user-token-username
export OSSRH_PASSWORD=your-user-token-password
export SIGNING_KEY_ID=your-gpg-key-id
export SIGNING_PASSWORD=your-gpg-password
export SIGNING_SECRET_KEY_RING_FILE=/path/to/secring.gpg
```

### Option 3: Command Line
Pass credentials as Gradle properties:

```bash
./gradlew publish \
  -PossrhUsername=your-token-username \
  -PossrhPassword=your-token-password \
  -Psigning.keyId=your-key-id \
  -Psigning.password=your-key-password \
  -Psigning.secretKeyRingFile=/path/to/secring.gpg
```

### GPG Key Setup
1. Generate a GPG key: `gpg --gen-key`
2. Export the secret key: `gpg --export-secret-keys -o secring.gpg`
3. Store `secring.gpg` securely (not in version control)

## Usage
### Basic Usage
To create a simple cache for your data type:

```kotlin
// Source of truth Flow of data, could be from a database or file
val myDataFlow = MutableStateFlow(BlockedCacheData<MyDataType>())

val myCache = BlockedCache<MyDataType>(
    refreshTime = 60000,  // Refresh every 60 seconds
    dataFlow = myDataFlow
)

val myStream: BlockedCacheStream<MyDataType> = BlockedCacheStreamBuilder
    .from(
        cache = myCache,
        fetcher = BlockedCacheStreamBuilder.Fetcher.of { api.fetchData() }
    )
    .updateData { myDataFlow.value = it }
    .build()

// Flow with share access to the cache and api
val dataFlow1: Flow<CacheResult<T>> = myStream.stream()
val dataFlow2: Flow<CacheResult<T>> = myStream.stream(forceRefresh = true)
```

## API Reference
- `BlockedCache`: Core class for caching mechanism.
- `BlockedCacheData`: Data model for cache entries.
- `BlockedCacheStream`: Streamlines fetching and updating cache.
- `FetcherResult`: Represents results of a fetch operation.
- More details available in the [API documentation](#).

## Alternatives


- [Store5](https://github.com/MobileNativeFoundation/Store) - A Kotlin Multiplatform library for
reactive data store based on Kotlin Flows powered by Kotlin Coroutines. While Store5 uses a similar
approach and provides more flexibility for data caching, it does not provide automatic or data based
conditional refreshing.

## Contributing
Contributions are welcome! If you'd like to contribute, please:
1. Fork the repository.
2. Create a new branch for your feature.
3. Add your changes and write tests.
4. Submit a pull request.

## License
This library is licensed under the [MIT License](LICENSE).
