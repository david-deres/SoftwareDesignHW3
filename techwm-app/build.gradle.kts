plugins {
    application
//    id ("com.google.protobuf") version "0.8.18"
}

application {
    mainClass.set("il.ac.technion.cs.softwaredesign.MainKt")
}

val junitVersion: String? by extra
val hamkrestVersion: String? by extra
val guiceVersion: String? by extra
val kotlinGuiceVersion: String? by extra
val externalLibraryVersion: String? by extra


dependencies {
    implementation(project(":library"))
    implementation("org.jetbrains.kotlin", "kotlin-reflect", "1.5.0")

    implementation("com.google.inject", "guice", guiceVersion)
    implementation("dev.misfitlabs.kotlinguice4", "kotlin-guice", kotlinGuiceVersion)
    implementation("il.ac.technion.cs.softwaredesign", "primitive-storage-layer", externalLibraryVersion)
    implementation("com.google.protobuf", "protobuf-kotlin", "3.21.1")



    testImplementation("org.junit.jupiter", "junit-jupiter-api", junitVersion)
    testImplementation("org.junit.jupiter", "junit-jupiter-params", junitVersion)
    testImplementation("com.natpryce", "hamkrest", hamkrestVersion)
}

//protobuf {
//    protoc {
//        artifact = "com.google.protobuf:protoc:21.0-rc-1"
//    } // bundled compiler for proto files
//
//    generateProtoTasks {
//        ofSourceSet("main").forEach { task ->
//            task.builtins {
//                kotlin { }
//            }
//        }
//    }
//}
