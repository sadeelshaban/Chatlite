plugins {
    application
    java
    id("org.openjfx.javafxplugin") version "0.1.0"
}
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
javafx {
    version = "21"
    modules = listOf("javafx.controls", "javafx.fxml", "javafx.graphics")
}
application { mainClass.set("com.ChatLite.client.ChatLiteClientApp") }
dependencies {
    implementation("org.slf4j:slf4j-api:2.0.16")
    implementation("org.slf4j:slf4j-simple:2.0.16")
}