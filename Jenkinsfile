pipeline {
    agent any

    stages {
        stage('Checkout Git') {
            steps {
                git branch: 'main',
                    url: 'https://github.com/Ahmeddhib/StudentsManagement-DevOps.git'
            }
        }

        stage('Build Spring Boot') {
            steps {
                sh 'mvn clean package'
            }
        }

        stage('Build Docker Image') {
            steps {
                sh 'docker build -t students-spring:latest .'
            }
        }

        stage('Run Container') {
            steps {
                sh '''
                    docker stop students-app || true
                    docker rm students-app || true
                    docker run -d -p 8080:8080 --name students-app students-spring:latest
                '''
            }
        }
    }

    post {
        success {
            echo '✅ Application déployée avec succès!'
            echo '📌 Accédez à: http://localhost:8080'
        }
        failure {
            echo '❌ Échec du déploiement'
        }
    }
}