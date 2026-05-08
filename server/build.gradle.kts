plugins {
    application
    java
}
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
application {
    mainClass.set("com.ChatLite.server.MainServer")
}
dependencies {
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("org.slf4j:slf4j-api:2.0.16")
    implementation("org.slf4j:slf4j-simple:2.0.16")
}
tasks.register("runWithGui") {

    // Start MainServer first
    dependsOn("run")

    doLast {
        println("Launching AdminConsoleGUI...")

        javaexec {
            mainClass.set("com.ChatLite.server.AdminConsoleGUI")
            classpath = sourceSets.main.get().runtimeClasspath
        }
    }
}
