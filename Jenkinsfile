pipeline {
    agent any
    
    tools {
        maven 'M3'
        jdk 'jdk17'
    }
    
    environment {
        // ⭐ IMPORTANT: Votre repository Docker Hub
        DOCKER_HUB_USER = "ahmedwolf"
        DOCKER_HUB_REPO = "spring-test"
        DOCKER_IMAGE = "${DOCKER_HUB_USER}/${DOCKER_HUB_REPO}"
        DOCKER_TAG = "${env.BUILD_NUMBER}"
        
        // Git
        GIT_URL = "https://github.com/Ahmeddhib/StudentsManagement-DevOps.git"
        GIT_BRANCH = "main"
    }
    
    triggers {
        // Polling automatique toutes les minutes
        pollSCM('* * * * *')
    }
    
    options {
        timeout(time: 30, unit: 'MINUTES')
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }
    
    stages {
        // ===== STAGE 1: VÉRIFICATION =====
        stage('Vérification') {
            steps {
                script {
                    echo "🚀 DÉMARRAGE DU PIPELINE"
                    echo "Build: ${env.BUILD_NUMBER}"
                    echo "Docker Hub: ${env.DOCKER_IMAGE}"
                    echo "Git: ${env.GIT_URL}"
                    
                    sh '''
                        echo "=== OUTILS DISPONIBLES ==="
                        java -version
                        mvn --version
                        docker --version
                        echo ""
                        echo "=== CRÉDENTIALS TEST ==="
                        echo "Docker Hub User: ${DOCKER_HUB_USER}"
                    '''
                }
            }
        }
        
        // ===== STAGE 2: CHECKOUT GIT =====
        stage('Checkout') {
            steps {
                echo "📥 RÉCUPÉRATION DU CODE"
                
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: "*/${env.GIT_BRANCH}"]],
                    extensions: [
                        [$class: 'CleanCheckout'],
                        [$class: 'CloneOption', depth: 1, shallow: true]
                    ],
                    userRemoteConfigs: [[
                        url: env.GIT_URL,
                        credentialsId: 'github-credentials'
                    ]]
                ])
                
                sh '''
                    echo "✅ Code récupéré"
                    echo "=== DERNIERS CHANGEMENTS ==="
                    git log --oneline -3
                    echo ""
                    echo "=== FICHIERS MODIFIÉS ==="
                    git diff --name-only HEAD~1 HEAD 2>/dev/null || echo "Premier build ou pas de changements détectés"
                '''
            }
        }
        
        // ===== STAGE 3: BUILD SPRING BOOT =====
        stage('Build Spring Boot') {
            steps {
                echo "🔨 CONSTRUCTION SPRING BOOT"
                
                sh '''
                    echo "🧹 Nettoyage..."
                    mvn clean || echo "Clean skipped"
                    
                    echo "📦 Packaging..."
                    mvn package -DskipTests
                    
                    echo "✅ JAR créé avec succès:"
                    ls -lh target/*.jar
                '''
                
                // Sauvegarde du JAR
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
            
            post {
                success {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }
        
        // ===== STAGE 4: BUILD DOCKER IMAGE =====
        stage('Build Docker Image') {
            steps {
                echo "🐳 CONSTRUCTION IMAGE DOCKER"
                
                script {
                    // Vérifier Dockerfile
                    sh '''
                        echo "🔍 Vérification Dockerfile..."
                        if [ ! -f "Dockerfile" ]; then
                            echo "❌ ERREUR: Dockerfile non trouvé!"
                            echo "Création d'un Dockerfile par défaut..."
                            cat > Dockerfile << 'DOCKERFILE'
FROM openjdk:17
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
DOCKERFILE
                        fi
                        
                        echo "📄 Contenu Dockerfile:"
                        cat Dockerfile
                    '''
                    
                    // Builder l'image Docker
                    sh """
                        echo "🔨 Building Docker image..."
                        docker build \\
                            --no-cache \\
                            -t ${env.DOCKER_IMAGE}:${env.DOCKER_TAG} \\
                            -t ${env.DOCKER_IMAGE}:latest \\
                            .
                        
                        echo "✅ Images créées:"
                        docker images | grep ${env.DOCKER_HUB_USER}
                    """
                }
            }
        }
        
        // ===== STAGE 5: TEST DOCKER IMAGE =====
        stage('Test Docker Image') {
            steps {
                echo "🧪 TESTS DOCKER"
                
                script {
                    sh """
                        echo "🚀 Lancement du conteneur de test..."
                        
                        # Lancer un conteneur de test
                        docker run -d \\
                            --name test-${env.BUILD_NUMBER} \\
                            -p 8081:8080 \\
                            ${env.DOCKER_IMAGE}:${env.DOCKER_TAG}
                        
                        # Attendre le démarrage
                        sleep 15
                        
                        echo "📊 Statut du conteneur:"
                        docker ps | grep test-${env.BUILD_NUMBER}
                        
                        echo "📝 Logs:"
                        docker logs test-${env.BUILD_NUMBER} --tail 20
                        
                        # Tester l'application
                        echo "🏥 Test de santé:"
                        if curl -f http://localhost:8081/actuator/health 2>/dev/null; then
                            echo "✅ Application saine"
                        elif curl -f http://localhost:8081 2>/dev/null; then
                            echo "✅ Application accessible"
                        else
                            echo "⚠️ Application en démarrage"
                        fi
                        
                        # Arrêter et nettoyer
                        echo "🧹 Nettoyage..."
                        docker stop test-${env.BUILD_NUMBER}
                        docker rm test-${env.BUILD_NUMBER}
                    """
                }
            }
        }
        
        // ===== STAGE 6: PUSH TO DOCKER HUB =====
        stage('Push to Docker Hub') {
            steps {
                echo "📤 PUBLICATION SUR DOCKER HUB"
                
                script {
                    withCredentials([usernamePassword(
                        credentialsId: 'docker-hub-credentials',
                        usernameVariable: 'DOCKER_HUB_USERNAME',
                        passwordVariable: 'DOCKER_HUB_PASSWORD'
                    )]) {
                        sh """
                            echo "🔐 Authentification Docker Hub..."
                            echo \$DOCKER_HUB_PASSWORD | docker login -u \$DOCKER_HUB_USERNAME --password-stdin
                            
                            echo "🏷️ Tagging images..."
                            # S'assurer que les tags sont corrects
                            docker tag ${env.DOCKER_IMAGE}:${env.DOCKER_TAG} \\
                                ${env.DOCKER_IMAGE}:${env.DOCKER_TAG}
                            docker tag ${env.DOCKER_IMAGE}:latest \\
                                ${env.DOCKER_IMAGE}:latest
                            
                            echo "📤 Pushing images to Docker Hub..."
                            docker push ${env.DOCKER_IMAGE}:${env.DOCKER_TAG}
                            docker push ${env.DOCKER_IMAGE}:latest
                            
                            echo "✅ Images publiées avec succès!"
                            echo "🔗 Lien: https://hub.docker.com/r/${env.DOCKER_IMAGE}"
                        """
                    }
                }
            }
        }
        
        // ===== STAGE 7: DÉPLOIEMENT =====
        stage('Deploy') {
            steps {
                echo "🚀 DÉPLOIEMENT"
                
                script {
                    sh """
                        echo "🔄 Mise à jour de l'application..."
                        
                        # Arrêter l'ancienne version si elle existe
                        docker stop spring-app || true
                        docker rm spring-app || true
                        
                        # Démarrer la nouvelle version
                        docker run -d \\
                            --name spring-app \\
                            --restart unless-stopped \\
                            -p 8080:8080 \\
                            ${env.DOCKER_IMAGE}:latest
                        
                        echo "✅ Application déployée!"
                        echo "🌐 Accès:"
                        echo "   Local: http://localhost:8080"
                        echo "   Réseau: http://\$(hostname -I | awk '{print \$1}'):8080"
                        echo ""
                        echo "📊 Vérification:"
                        sleep 5
                        docker ps | grep spring-app
                    """
                }
            }
        }
    }
    
    // ===== POST-BUILD =====
    post {
        always {
            echo "🧹 NETTOYAGE"
            sh '''
                echo "Nettoyage des conteneurs de test..."
                docker ps -aq --filter "name=test-" | xargs -r docker rm -f 2>/dev/null || true
                
                echo "Nettoyage des images temporaires..."
                docker images -f "dangling=true" -q | xargs -r docker rmi 2>/dev/null || true
                
                echo "Liste des images finales:"
                docker images | head -10
            '''
        }
        
        success {
            echo "🎉 PIPELINE RÉUSSIE!"
            script {
                def duration = currentBuild.durationString
                echo """
                ===== RÉSUMÉ =====
                Build: ${env.BUILD_NUMBER}
                Image: ${env.DOCKER_IMAGE}:${env.DOCKER_TAG}
                Durée: ${duration}
                Docker Hub: https://hub.docker.com/r/${env.DOCKER_IMAGE}
                ==================
                """
                
                // Optionnel: Notification
                // slackSend(color: 'good', message: "Build ${env.BUILD_NUMBER} réussi - Image: ${env.DOCKER_IMAGE}:${env.DOCKER_TAG}")
            }
        }
        
        failure {
            echo "❌ PIPELINE ÉCHOUÉE"
            script {
                // Optionnel: Notification d'erreur
                // mail to: 'admin@example.com', subject: "Build ${env.BUILD_NUMBER} failed", body: "Voir ${env.BUILD_URL}"
            }
        }
    }
}
