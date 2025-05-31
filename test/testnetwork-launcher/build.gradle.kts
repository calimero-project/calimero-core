plugins {
	application
}

application {
	mainClass.set("tuwien.auto.calimero.TestNetwork")
}

tasks.named<JavaExec>("run") {
	// for attaching to debugger, start with -Ddebug=true
	if (System.getProperty("debug", "false") == "true") {
		jvmArgs("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=8000")
	}
	systemProperties(System.getProperties() as Map<String?, *>)
	args("server-config.xml")
	standardInput = System.`in`
}

group = "com.github.calimero"
version = "2.6"

repositories {
	mavenLocal()
	mavenCentral()
	maven("https://oss.sonatype.org/content/repositories/snapshots")
}

dependencies {
	implementation("${group}:calimero-testnetwork:${version}")
}
