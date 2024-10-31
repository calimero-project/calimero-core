import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

plugins {
	`java-library`
	application
	`maven-publish`
	signing
	id("org.gradle.test-retry") version "1.6.0"
	id("com.github.ben-manes.versions") version "0.51.0"
	eclipse
}

repositories {
	mavenLocal()
	mavenCentral()
	maven("https://oss.sonatype.org/content/repositories/snapshots")
}

val junitJupiterVersion = "5.11.3"
val desc = "Calimero, a free KNX network library"

group = "com.github.calimero"
version = "2.6-rc1"

java {
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(17))
	}
	withSourcesJar()
	withJavadocJar()
}

tasks.withType<Jar>().configureEach {
	from(projectDir) {
		include("LICENSE.txt")
		into("META-INF")
	}
	if (name == "sourcesJar") {
		from(projectDir) {
			include("README.md")
		}
	}
}

tasks.withType<JavaCompile>().configureEach {
	options.encoding = "UTF-8"
	options.compilerArgs.addAll(listOf(
		"-Xlint:all,-serial",
		"--limit-modules", "java.base,java.xml"
	))
}

tasks.named<JavaCompile>("compileJava") {
	options.javaModuleVersion = project.version.toString()
}

application {
	mainClass.set("tuwien.auto.calimero.Settings")
}

sourceSets {
	main {
		java.srcDirs("src")
		resources.srcDirs("resources")
	}
	test {
		java {
			srcDirs("test")
			exclude("resources/", "**/.gradle")
		}
		resources.srcDirs("test/resources")
	}
}

tasks.withType<Javadoc>().configureEach {
	(options as CoreJavadocOptions).addStringOption("Xdoclint:-missing", "-quiet")
}

tasks.named<Jar>("jar") {
	manifest {
		val gitHash = providers.exec {
			commandLine("git", "-C", "$projectDir", "rev-parse", "--verify", "--short", "HEAD")
		}.standardOutput.asText.map { it.trim() }
		val buildDate = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")
			.withZone(ZoneId.of("UTC"))
			.format(Instant.now())

		attributes(
			"Main-Class" to application.mainClass.get(),
			"Implementation-Version" to project.version,
			"Revision" to gitHash.get(),
			"Build-Date" to buildDate
		)
	}
}

tasks.named<Test>("test") {
	useJUnitPlatform {
		excludeTags("ft12", "slow")
		testLogging {
//			exceptionFormat = TestExceptionFormat.FULL
//			showStandardStreams = true
		}
	}
	retry {
		maxRetries = 2
		maxFailures = 20
	}
}

publishing {
	publications {
		create<MavenPublication>("mavenJava") {
			artifactId = rootProject.name
			from(components["java"])
			pom {
				name.set("Calimero core library")
				description.set("Calimero, a free KNX network library")
				url.set("https://github.com/calimero-project/calimero-core")
				inceptionYear.set("2006")
				licenses {
					license {
						name.set("GNU General Public License, version 2, with the Classpath Exception")
						url.set("LICENSE")
					}
				}
				developers {
					developer {
						name.set("Boris Malinowsky")
						email.set("b.malinowsky@gmail.com")
					}
				}
				scm {
					connection.set("scm:git:git://github.com/calimero-project/calimero-core.git")
					url.set("https://github.com/calimero-project/calimero-core.git")
				}
			}
		}
	}
	repositories {
		maven {
			name = "maven"
			val releasesRepoUrl = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2")
			val snapshotsRepoUrl = uri("https://oss.sonatype.org/content/repositories/snapshots")
			url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
			credentials(PasswordCredentials::class)
		}
	}
}

signing {
	if (project.hasProperty("signing.keyId")) {
		sign(publishing.publications["mavenJava"])
	}
}

plugins.withType<JavaPlugin>().configureEach {
	eclipse {
		// Eclipse's view of projects treats circular dependencies as errors by default
		jdt.file.withProperties { set("org.eclipse.jdt.core.circularClasspath", "warning") }
	}
}

dependencies {
	api("org.slf4j:slf4j-api:2.0.12")

	testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
	// Eclipse treats circular dependencies as errors by default, see eclipseJdt task above
//	testRuntimeOnly("com.github.calimero:calimero-rxtx:$version")
}
