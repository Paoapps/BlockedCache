# BlockedCache

[![Maven Central](https://img.shields.io/maven-central/v/com.paoapps.blockedcache/blocked-cache)](https://central.sonatype.com/artifact/com.paoapps.blockedcache/blocked-cache)

Kotlin Multiplatform Mobile framework for optimal code sharing between iOS and Android.

## Installation

```kotlin
dependencies {
    implementation("com.paoapps.blockedcache:blocked-cache:0.0.7-SNAPSHOT")
}
```

## Publishing

This library is published to Maven Central automatically on every release.

To publish a new version:

1. Update the version in `gradle.properties`
2. Create a new tag with the version number
3. Push the tag to GitHub

The release workflow will automatically publish the library to Maven Central.

### GitHub Actions Setup

To enable automatic publishing, you need to configure the following secrets in your GitHub repository:

1. Go to **Settings** → **Security** → **Secrets and variables** → **Actions**
2. Add the following repository secrets:

| Secret Name | Description | Source |
|-------------|-------------|---------|
| `MAVEN_CENTRAL_USERNAME` | Maven Central username | Generated from Central Portal → Account → User Token |
| `MAVEN_CENTRAL_PASSWORD` | Maven Central password | Generated from Central Portal → Account → User Token |
| `SIGNING_KEY_ID` | Last 8 characters of your GPG key ID | From `gpg --list-keys` output |
| `SIGNING_PASSWORD` | Your GPG key passphrase | Set when generating the GPG key |
| `GPG_KEY_CONTENTS` | Full contents of your exported GPG key | From the `key.gpg` file exported with `gpg --export-secret-keys` |

### Workflows

- **`.github/workflows/gradle.yml`**: Runs CI builds on push/PR to test all platforms
- **`.github/workflows/publish.yml`**: Publishes to Maven Central on release creation

## License

MIT
