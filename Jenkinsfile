pipeline {
    agent any

    environment {
        DOCKER_IMAGE = "ahmedwolf/spring-test3"
        DOCKER_TAG = "latest"
        K8S_NAMESPACE = "devops"
        APP_NAME = "student-management"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout([$class: 'GitSCM',
                          branches: [[name: '*/main']],
                          userRemoteConfigs: [[url: 'https://github.com/Ahmeddhib/StudentsManagement-DevOps.git']],
                          extensions: [[$class: 'CloneOption', shallow: true, depth: 1, noTags: false, timeout: 10]]
                ])
            }
        }

        stage('Build Maven Project') {
            steps {
                sh 'mvn clean package -DskipTests -B'
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    // Utiliser l'environnement Docker de Minikube
                    sh '''
                        eval $(minikube docker-env)
                        docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} .
                        docker tag ${DOCKER_IMAGE}:${DOCKER_TAG} student-management:latest
                    '''
                }
            }
        }

        stage('Push to DockerHub') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'dockerhub', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                        sh '''
                            echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin
                            docker push ${DOCKER_IMAGE}:${DOCKER_TAG}
                        '''
                    }
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                script {
                    // 1. Mettre à jour l'image dans le déploiement Kubernetes
                    sh '''
                        kubectl set image deployment/spring-deployment \
                        spring-app=student-management:latest \
                        -n ${K8S_NAMESPACE}
                    '''
                    
                    // 2. Vérifier le rollout
                    sh '''
                        kubectl rollout status deployment/spring-deployment \
                        -n ${K8S_NAMESPACE} --timeout=300s
                    '''
                    
                    // 3. Vérifier les pods
                    sh '''
                        kubectl get pods -n ${K8S_NAMESPACE}
                        kubectl get svc -n ${K8S_NAMESPACE}
                    '''
                }
            }
        }

        stage('Test Deployment') {
            steps {
                script {
                    // Obtenir l'URL et tester
                    sh '''
                        APP_URL=$(minikube service spring-service -n ${K8S_NAMESPACE} --url)
                        echo "Application URL: $APP_URL"
                        curl -s $APP_URL/student/actuator/health | grep -q "UP" && echo "✅ Application is UP!" || echo "❌ Application is DOWN!"
                    '''
                }
            }
        }
    }

    post {
        always {
            echo "Pipeline terminée ✅"
            script {
                // Nettoyage
                sh 'docker logout'
            }
        }
        success {
            echo "✅ Déploiement réussi sur Kubernetes!"
            slackSend(color: 'good', message: "Build ${BUILD_NUMBER} déployé avec succès!")
        }
        failure {
            echo "❌ Échec du déploiement!"
            slackSend(color: 'danger', message: "Build ${BUILD_NUMBER} a échoué!")
            emailext (
                subject: "Échec du pipeline: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                body: "Le build ${env.BUILD_URL} a échoué.",
                to: 'votre-email@example.com'
            )
        }
    }
}
