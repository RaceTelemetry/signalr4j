plugins {
    `java-library`
    `maven-publish`
//    signing
//    id("net.researchgate.release") version "2.8.1"
}

java {
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
}

dependencies {
    api("com.fasterxml.jackson.core:jackson-databind:2.13.3")
    api("org.java-websocket:Java-WebSocket:1.5.3")
    api("org.slf4j:slf4j-api:1.7.36")

    val lombokAnnotations = "org.projectlombok:lombok:1.18.24"
    compileOnly(lombokAnnotations)
    annotationProcessor(lombokAnnotations)
    testCompileOnly(lombokAnnotations)
    testAnnotationProcessor(lombokAnnotations)

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.hamcrest:hamcrest-library:2.2")
    testImplementation("org.slf4j:slf4j-simple:1.7.36")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<JavaCompile> {
    options.encoding = Charsets.UTF_8.name()
}

configure<PublishingExtension> {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

//
//if (project.findProperty("ossrhUsername") != null && project.findProperty("ossrhPassword") != null) {
//    uploadArchives {
//        repositories {
//            mavenDeployer {
//                beforeDeployment { MavenDeployment deployment -> signing.signPom(deployment) }
//
//                repository(url: "https://oss.sonatype.org/service/local/staging/deploy/maven2/") {
//                    authentication(userName: project.findProperty("ossrhUsername"), password: project.findProperty("ossrhPassword"))
//                }
//
//                snapshotRepository(url: "https://oss.sonatype.org/content/repositories/snapshots/") {
//                    authentication(userName: project.findProperty("ossrhUsername"), password: project.findProperty("ossrhPassword"))
//                }
//
//                pom.project {
//                    name 'signalr4j'
//                    packaging 'jar'
//                    description 'A java client library for accessing SignalR WebSocket endpoints.'
//                    url 'https://github.com/signalr4j/signalr4j'
//
//                    scm {
//                        connection 'scm:git:https://github.com/signalr4j/signalr4j.git'
//                        developerConnection 'scm:git:https://github.com/signalr4j/signalr4j.git'
//                        url 'https://github.com/signalr4j/signalr4j'
//                    }
//
//                    licenses {
//                        license {
//                            name 'Apache 2.0 Licence'
//                            url 'https://www.apache.org/licenses/LICENSE-2.0'
//                        }
//                    }
//
//                    developers {
//                        developer {
//                            id 'CCob'
//                            name 'Ceri Coburn'
//                            email 'ceri.coburn@gmail.com'
//                        }
//                    }
//
//                }
//            }
//        }
//    }
//
//    signing {
//        sign configurations.archives
//    }
//
//    release {
//        failOnCommitNeeded = true
//        failOnPublishNeeded = true
//        failOnSnapshotDependencies = true
//        failOnUnversionedFiles = true
//        failOnUpdateNeeded = true
//        revertOnFail = true
//        preCommitText = ''
//        preTagCommitMessage = '[Gradle Release Plugin] - pre tag commit: '
//        tagCommitMessage = '[Gradle Release Plugin] - creating tag : '
//        newVersionCommitMessage = '[Gradle Release Plugin] - new version commit: '
//        tagTemplate = '${version}'
//        // May decide to add additional custom tasks here
//        buildTasks = ['uploadArchives']
//        scmAdapters = [
//                net.researchgate.release.GitAdapter
//        ]
//    }
//}


