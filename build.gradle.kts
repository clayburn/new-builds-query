plugins {
    id("org.openapi.generator") version "7.3.0"
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.ajalt.clikt:clikt:4.2.2")
    implementation("io.swagger:swagger-annotations:1.6.13")
    implementation("javax.annotation:javax.annotation-api:1.3.2")
    implementation("com.fasterxml.jackson.jaxrs:jackson-jaxrs-json-provider:2.16.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.16.1")
    implementation("com.google.code.findbugs:jsr305:3.0.2")
    implementation("org.apache.httpcomponents.client5:httpclient5:5.3.1")

    implementation("org.slf4j:slf4j-api:2.0.12")
    runtimeOnly("org.slf4j:slf4j-simple:2.0.12")
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

application {
    mainClass.set("me.clayjohnson.newbuildsquery.AppKt")
}

val develocityVersion = "2023.4"
val baseApiUrl = providers.gradleProperty("apiManualUrl")
    .orElse("https://docs.gradle.com/enterprise/api-manual/ref/")

val apiSpecificationURL = baseApiUrl.map { "${it}gradle-enterprise-${develocityVersion}-api.yaml" }
val apiSpecificationFile = objects.property(File::class)
    .convention(provider {
        resources.text.fromUri(apiSpecificationURL).asFile()
    }).map { file -> file.absolutePath }

val basePackageName = "com.gradle.develocity.api"
val modelPackageName = "$basePackageName.model"
val invokerPackageName = "$basePackageName.client"
openApiGenerate {
    generatorName.set("java")
    inputSpec.set(apiSpecificationFile)
    outputDir.set(project.layout.buildDirectory.file("generated/$name").map { it.asFile.absolutePath })
    ignoreFileOverride.set(project.layout.projectDirectory.file(".openapi-generator-ignore").asFile.absolutePath)
    modelPackage.set(modelPackageName)
    apiPackage.set(basePackageName)
    invokerPackage.set(invokerPackageName)
    cleanupOutput.set(true)
    openapiNormalizer.set(mapOf("REF_AS_PARENT_IN_ALLOF" to "true"))
    // see https://github.com/OpenAPITools/openapi-generator/blob/master/docs/generators/java.md for a description of each configuration option
    configOptions.set(mapOf(
        "library" to "apache-httpclient",
        "dateLibrary" to "java8",
        "hideGenerationTimestamp" to "true",
        "openApiNullable" to "false",
        "useBeanValidation" to "false",
        "disallowAdditionalPropertiesIfNotPresent" to "false",
        "additionalModelTypeAnnotations" to  "@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)",
        "sourceFolder" to "",  // makes IDEs like IntelliJ more reliably interpret the class packages.
        "containerDefaultToNull" to "true",
        "testOutput" to project.layout.buildDirectory.dir("generated-test-sources/openapi").map { it.asFile.absolutePath }.get()
    ))
}

sourceSets {
    main {
        java {
            srcDir(tasks.openApiGenerate)
        }
    }
}

tasks.test {
    useJUnitPlatform()
}