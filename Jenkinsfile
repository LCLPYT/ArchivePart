// This file should only be used by the authors personal jenkins.
pipeline {
    agent { docker {image 'lclpmaven:latest' }}
    stages {
        stage('build') {
            steps {
                sh 'mvn --version'
                sh 'mvn package --file ArchivePart/pom.xml'
            }
        }
        stage('deploy') {
            steps {
                sh 'cp /.m2/settings.xml .m2/'
                sh 'mvn deploy --file ArchivePart/pom.xml'
            }
        }
    }
}
