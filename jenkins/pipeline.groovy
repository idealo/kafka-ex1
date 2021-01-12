@Library('idp@0.17.11')

String sourceRepository = 'ssh://git@code.eu.idealo.com:7999/dk/kafka-consumer-idempotency.git'

pipeline {
    agent { node { label 'java' } }

    environment {
        IDP_SKIP_STAGES = true
    }

    stages {
        stage('test and install new version') {
            steps {
                script {
                    idp_maven {
                        gitRepoUrl = sourceRepository
                        mavenGoals = "clean install"
                        jdkVersion = 'openjdk-11.0.2'
                    }
                }
            }
        }
        stage('release to artifactory') {
            steps {
                script {
                    idp_createBuildVersion {
                        gitRepoUrl = sourceRepository
                    }
                }
                script {
                    idp_buildReleasable {
                        mvnSkipTests = true
                        artifactoryTargetRepo = 'libs-commit-local'
                        jdkVersion = 'openjdk-11.0.2'
                    }
                }
            }
        }
    }

}
