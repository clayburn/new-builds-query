plugins {
    id("com.gradle.enterprise") version "3.17"
    id("com.gradle.common-custom-user-data-gradle-plugin") version "1.13"
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "new-builds-query"

gradleEnterprise {
    buildScan {
        val acceptTOSProp = "acceptGradleTOS"
        if (extra.properties.sets(acceptTOSProp)) {
            termsOfServiceUrl = "https://gradle.com/terms-of-service"
            termsOfServiceAgree = "yes"
        }
        publishAlways()

        obfuscation {
            ipAddresses { addresses -> addresses.map { _ -> "0.0.0.0" } }
        }
    }
}

fun Map<String, Any>.sets(key: String) : Boolean {
    val value = this.getOrDefault(key, "false").toString()
    return value.isBlank() || value.toBoolean()
}