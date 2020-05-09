import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
	kotlin("jvm") version "1.3.71"
	id("com.github.johnrengelman.shadow") version "5.2.0"
	id("java-library-distribution")
}

group = "com.afengar.blockchain.hlf"
version = "1.0.0"
java.sourceCompatibility = JavaVersion.VERSION_1_8

repositories {
	mavenLocal()
	mavenCentral()
	maven {
        setUrl("https://hyperledger.jfrog.io/hyperledger/fabric-maven")
    }
    maven {
        setUrl("https://jitpack.io")
    }
}

dependencies {
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	implementation("org.hyperledger.fabric-chaincode-java:fabric-chaincode-shim:2.+")

}

tasks {
    "shadowJar"(ShadowJar::class) {
        baseName = "chaincode"
        version = null
        classifier = null
        manifest {
            attributes(mapOf("Main-Class" to "com.afengar.blockchain.hlf.counterchaincode.CounterContract"))
        }
    }
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
		jvmTarget = "1.8"
	}
}
