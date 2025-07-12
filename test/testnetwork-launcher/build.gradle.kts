plugins {
	application
}

application {
	mainClass.set("io.calimero.testnetwork.TestNetwork")
}

tasks.named<JavaExec>("run") {
	// for attaching to debugger, start with -Ddebug=true
	if (System.getProperty("debug", "false") == "true") {
		jvmArgs("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=8000")
	}
	systemProperties(System.getProperties() as Map<String, *>)
	args("server-config.xml")
	standardInput = System.`in`
}

group = "io.calimero"
version = "3.0-M1"

repositories {
	mavenLocal()
	mavenCentral()
	maven("https://central.sonatype.com/repository/maven-snapshots/")
}

dependencies {
	implementation("${group}:calimero-testnetwork:${version}")

	runtimeOnly("org.slf4j:slf4j-jdk-platform-logging:2.0.17")
	runtimeOnly("org.slf4j:slf4j-simple:2.0.17")
}
