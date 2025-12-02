[23:01, 02/12/2025] Nesrine Romdhani: pipeline {
    agent any

    environment {
        DOCKER_IMAGE = "nesrineromd/projet_devos"
    }

    stages {
        stage('Checkout') {
            steps {
                // Clone rapide, shallow clone pour gagner du temps
                checkout([$class: 'GitSCM',
                          branches: [[name: '*/main']],
                          userRemoteConfigs: [[url: 'https://github.com/nesrrine/projet_devos.git']],
                          extensions: [[$class: 'CloneOption', shallow: true, depth: 1, noTags: false, reference: '', timeout: 10]]
                ])
            }
        }

        stage('Clean & Build') {
            steps {
                // Build Maven
                sh 'mvn clean install -DskipTests -B'
            }
        …
[23:05, 02/12/2025] Nesrine Romdhani: hthhaaaaaaaaaa shihh pipeline {
    agent any

    environment {
        DOCKER_IMAGE = "nesrineromd/projet_devos"
        DOCKER_TAG = "latest"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout([$class: 'GitSCM',
                          branches: [[name: '*/main']],
                          userRemoteConfigs: [[url: 'https://github.com/nesrrine/projet_devos.git']],
                          extensions: [[$class: 'CloneOption', shallow: true, depth: 1, noTags: false, timeout: 10]]
                ])
            }
        }

        stage('Check Docker Image') {
            steps {
                script {
                    def imageExists = sh(script: "docker image inspect ${DOCKER_IMAGE}:${DOCKER_TAG} > /dev/null 2>&1 || echo 'no'", returnStdout: true).trim()
                    if(imageExists == "no") {
                        env.BUILD_MAVEN = "true"
                    } else {
                        env.BUILD_MAVEN = "false"
                    }
                    echo "Build Maven needed? ${env.BUILD_MAVEN}"
                }
            }
        }

        stage('Build Maven Project') {
            when {
                expression { env.BUILD_MAVEN == "true" }
            }
            steps {
                sh 'mvn clean install -DskipTests -B'
            }
        }

        stage('Build Docker Image') {
            when {
                expression { env.BUILD_MAVEN == "true" }
            }
            steps {
                script {
                    sh "docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} ."
                }
            }
        }

        stage('Push Docker Image') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'dockerhub', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                        sh '''
                            echo \$DOCKER_PASS | docker login -u \$DOCKER_USER --password-stdin
                            docker push ${DOCKER_IMAGE}:${DOCKER_TAG}
                        '''
                    }
                }
            }
        }
    }

    post {
        always {
            echo "Pipeline terminée ✅"
        }
    }
}
