# BlockedCache

[![Maven Central](https://img.shields.io/maven-central/v/com.paoapps.blockedcache/blocked-cache)](https://central.sonatype.com/artifact/com.paoapps.blockedcache/blocked-cache)
[![Kotlin](https://img.shields.io/badge/kotlin-2.1.21-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Platform](https://img.shields.io/badge/platform-Android%20%7C%20iOS%20%7C%20JVM%20%7C%20Linux-lightgrey.svg)](https://kotlinlang.org/docs/multiplatform.html)

A powerful, network-aware caching library for Kotlin Multiplatform that provides intelligent data fetching, automatic refresh, and offline support across Android, iOS, JVM, and Linux platforms.

## ✨ Features

- **🚀 Smart Caching**: Automatic data refresh based on time intervals and custom triggers
- **📱 Multiplatform**: Full support for Android, iOS, JVM, and Linux
- **🌐 Network Aware**: Intelligent handling of network connectivity and offline scenarios
- **🔒 Thread Safe**: Prevents duplicate network requests with mutex-based locking
- **⚡ Reactive**: Built on Kotlin Flows for reactive data streaming
- **🔄 Auto Refresh**: Configurable refresh intervals and manual refresh triggers
- **💾 Persistent**: Optional data persistence with customizable storage
- **📊 Rich States**: Comprehensive cache states (Loading, Success, Error, Offline, Empty)

## 📦 Installation

### Gradle (Kotlin DSL)

Add the dependency to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.paoapps.blockedcache:blocked-cache:0.0.8")
}
```

### Gradle (Groovy)

```gradle
dependencies {
    implementation 'com.paoapps.blockedcache:blocked-cache:0.0.8'
}
```

### Platform-Specific Setup

#### Android
No additional setup required. The library automatically handles Android-specific implementations.

#### iOS
Add the following to your `Podfile` if using CocoaPods:

```ruby
pod 'BlockedCache', :git => 'https://github.com/Paoapps/BlockedCache.git', :tag => '0.0.8'
```

Or use Swift Package Manager by adding this repository as a dependency.

#### JVM
No additional setup required. Works out of the box with standard JVM applications.

#### Linux
No additional setup required. Compatible with Linux distributions.

## 🚀 Quick Start

### Basic Usage

```kotlin
import com.paoapps.blockedcache.*
import kotlinx.coroutines.flow.*
import kotlin.time.Duration.Companion.minutes

// Create a data source (could be database, shared preferences, etc.)
val dataFlow = MutableStateFlow(BlockedCacheData<String>(null, null))

// Create the cache
val cache = BlockedCache<String>(
    refreshTime = 5.minutes,
    expireTime = 1.hours,
    dataFlow = dataFlow,
    name = "userProfile"
)

// Fetch data with automatic caching
val resultFlow = cache.getData(
    fetcher = {
        // Simulate network call
        val userData = api.fetchUserProfile()
        FetcherResult.Data(userData)
    },
    updateData = { newData ->
        dataFlow.value = newData
    }
)

// Collect the results
resultFlow.collect { result ->
    when (result) {
        is CacheResult.Loading -> showLoading(result.staleData)
        is CacheResult.Success -> showData(result.data)
        is CacheResult.Error -> showError(result.failure, result.staleData)
        is CacheResult.Offline -> showOfflineMessage(result.staleData)
        is CacheResult.Empty -> showEmptyState()
    }
}
```

### Advanced Usage with Custom Triggers

```kotlin
import com.paoapps.blockedcache.*
import kotlinx.coroutines.flow.*

// Custom refresh trigger (e.g., pull-to-refresh)
val refreshTrigger = MutableSharedFlow<Unit>()

val cache = BlockedCache<UserData>(
    refreshTime = 10.minutes,
    expireTime = 2.hours,
    trigger = refreshTrigger,
    dataFlow = userDataFlow,
    name = "userData"
)

// Manual refresh
fun refreshData() {
    cache.refresh(forceRefresh = true)
}

// Trigger refresh from UI
refreshTrigger.emit(Unit)
```

### Network-Aware Caching

```kotlin
import com.paoapps.blockedcache.utils.network.*

// Update network status (implement platform-specific monitoring)
NetworkStatusMonitor.networkStatus.value = NetworkStatus.AVAILABLE
// or
NetworkStatusMonitor.networkStatus.value = NetworkStatus.UNAVAILABLE

val cache = BlockedCache<Data>(
    refreshTime = 5.minutes,
    dataFlow = dataFlow,
    networkStatusFlow = NetworkStatusMonitor.networkStatus
)
```

## 📚 API Reference

### Core Classes

#### `BlockedCache<T>`

The main cache class that manages data fetching, caching, and refresh logic.

**Parameters:**
- `refreshTime`: Time after which data is considered stale
- `expireTime`: Optional time after which data is considered expired
- `trigger`: Flow of events that trigger cache refresh
- `dataFlow`: Flow of cached data
- `networkStatusFlow`: Flow representing network connectivity
- `nowProvider`: Provider for current time (useful for testing)
- `name`: Identifier for debugging
- `isDebugEnabled`: Enable debug logging

**Key Methods:**
- `getData()`: Fetch data with caching logic
- `refresh()`: Manually trigger a refresh

#### `CacheResult<T>`

Sealed class representing cache operation results:

```kotlin
sealed class CacheResult<out T> {
    object Empty : CacheResult<Nothing>()
    data class Offline<T>(val staleData: T? = null, val creationTimeStaleData: Long? = null) : CacheResult<T>()
    data class Loading<T>(val staleData: T? = null, val creationTimeStaleData: Long? = null) : CacheResult<T>()
    data class Success<T>(val data: T) : CacheResult<T>()
    data class Error<T>(val failure: FetcherResult.Error, val staleData: T? = null, val creationTimeStaleData: Long? = null) : CacheResult<T>()
}
```

#### `FetcherResult<T>`

Represents the result of a data fetching operation:

```kotlin
sealed class FetcherResult<out T : Any> {
    data class Data<T : Any>(val value: T, val origin: String? = null) : FetcherResult<T>()
    sealed class Error(val code: Int? = null) : FetcherResult<Nothing>() {
        data class Exception(val error: Throwable, val code: Int? = null) : Error()
        data class Message(val message: String, val code: Int? = null) : Error()
    }
}
```

#### `BlockedCacheData<T>`

Container for cached data with metadata:

```kotlin
@Serializable
data class BlockedCacheData<T>(
    val data: T? = null,
    val creationTime: Long? = null
)
```

## 🔧 Configuration

### Custom Data Storage

Implement your own data persistence:

```kotlin
// Example with DataStore (Android) or similar
val dataStoreFlow = dataStore.data.map { preferences ->
    val json = preferences[USER_DATA_KEY] ?: return@map BlockedCacheData(null, null)
    val data = Json.decodeFromString<UserData>(json)
    val timestamp = preferences[USER_TIMESTAMP_KEY]
    BlockedCacheData(data, timestamp)
}

val cache = BlockedCache(
    refreshTime = 15.minutes,
    dataFlow = dataStoreFlow,
    // ... other parameters
)
```

### Testing

Use custom `NowProvider` for testing:

```kotlin
class TestNowProvider : NowProvider {
    var currentTime = 1000L
    override fun now(): Long = currentTime
}

val testProvider = TestNowProvider()
val cache = BlockedCache(
    refreshTime = 5.minutes,
    dataFlow = testDataFlow,
    nowProvider = testProvider
)

// Control time in tests
testProvider.currentTime = 2000L
```

## 🎯 Use Cases

- **User Profiles**: Cache user data with automatic refresh
- **Feed Data**: Cache social media feeds, news articles
- **Configuration**: Cache app configuration and feature flags
- **Search Results**: Cache search queries and results
- **API Responses**: Cache REST API responses with TTL
- **Offline Support**: Provide stale data when offline

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guide](CONTRIBUTING.md) for details.

### Development Setup

1. Clone the repository:
```bash
git clone https://github.com/Paoapps/BlockedCache.git
cd BlockedCache
```

2. Open in your IDE (Android Studio, IntelliJ IDEA, or VS Code with Kotlin plugin)

3. Run tests:
```bash
./gradlew test
```

4. Build all platforms:
```bash
./gradlew build
```

### Publishing

This library is published to Maven Central automatically on every release.

To publish a new version:

1. Update the version in `library/build.gradle.kts`
2. Create a new tag with the version number
3. Push the tag to GitHub

The release workflow will automatically publish the library to Maven Central.

## 📄 License

```
MIT License

Copyright (c) 2024 PaoApps

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## 🙏 Acknowledgments

- Built with [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html)
- Inspired by modern caching patterns and reactive programming
- Thanks to the Kotlin community for excellent tooling and documentation

## 📞 Support

- 📧 **Email**: lammert@paoapps.com
- 🐛 **Issues**: [GitHub Issues](https://github.com/Paoapps/BlockedCache/issues)
- 💬 **Discussions**: [GitHub Discussions](https://github.com/Paoapps/BlockedCache/discussions)

---

<p align="center">
  <b>⭐ Star this repo if you find it useful!</b>
</p>
