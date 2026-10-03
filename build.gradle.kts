import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

plugins {
	`java-library`
	application
	`maven-publish`
	signing
	id("org.gradle.test-retry") version "1.6.6"
	id("org.graalvm.buildtools.native") version "1.1.14"
	id("io.github.ben-manes.versions") version "0.64.0"
	eclipse
}

repositories {
	mavenCentral()
	mavenLocal()
	maven("https://central.sonatype.com/repository/maven-snapshots/")
}

rootProject.extra.set("junitJupiterVersion", "6.1.3")
val desc = "Calimero, a free KNX network library"

group = "io.calimero"
version = "3.0-SNAPSHOT"

java {
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(21))
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

tasks.named<JavaCompile>("compileTestJava") {
	options.compilerArgs.add("-Xlint:-try")
}

application {
	mainModule.set("io.calimero.core")
	mainClass.set("io.calimero.Settings")
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
	options.encoding = "UTF-8"
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

testing {
	suites {
		val test = named<JvmTestSuite>("test") {
			useJUnitJupiter("${rootProject.extra.get("junitJupiterVersion")}")

			targets {
				all {
					testTask.configure {
						options {
							val options = this as JUnitPlatformOptions
							options.excludeTags("ft12", "slow")
						}
						testLogging {
//							exceptionFormat = TestExceptionFormat.FULL
//							showStandardStreams = true
						}
						retry {
							maxRetries = 2
							maxFailures = 20
						}
					}
				}
			}
		}
	}
}

tasks.named<JavaExec>("run") {
	// Work around https://github.com/graalvm/native-build-tools/issues/743
	outputs.upToDateWhen { false }
}

graalvmNative {
//	toolchainDetection = true // only works reliably if a single JDK is installed, which is GraalVM
	agent {
//		enabled = true
		defaultMode = "standard"
	}
	binaries {
		named("main") {
//			verbose = true
			buildArgs.addAll(
				listOf(
					"--initialize-at-build-time",
					"--no-fallback",
					"--exact-reachability-metadata",
					"-H:+ReportExceptionStackTraces",
				)
			)
		}
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
			val releasesRepoUrl = uri("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/")
			val snapshotsRepoUrl = uri("https://central.sonatype.com/repository/maven-snapshots/")
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
