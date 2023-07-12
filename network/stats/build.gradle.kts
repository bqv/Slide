import com.expediagroup.graphql.plugin.gradle.config.GraphQLSerializer
import com.expediagroup.graphql.plugin.gradle.tasks.GraphQLGenerateClientTask
import com.expediagroup.graphql.plugin.gradle.tasks.GraphQLIntrospectSchemaTask

plugins {
    id(libs.plugins.java.library.get().pluginId)
    id(libs.plugins.kotlin.jvm.get().pluginId)
    id(libs.plugins.graphql.get().pluginId)
    id(libs.plugins.kotlin.serialization.get().pluginId)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    // GraphQL
    implementation(libs.bundles.graphql)
}

val graphqlIntrospectSchema
        by tasks.getting(GraphQLIntrospectSchemaTask::class) {
            endpoint.set("https://the-federation.info/v1/graphql")
        }

val graphqlGenerateClient
        by tasks.getting(GraphQLGenerateClientTask::class) {
            packageName.set("info.the_federation.graphql.generated")
            schemaFile.set(graphqlIntrospectSchema.outputFile)
            serializer.set(GraphQLSerializer.KOTLINX)

            dependsOn("graphqlIntrospectSchema")
        }
