pipeline {
    agent any
    
    tools {
        maven 'M3'
        jdk 'jdk17'
    }
    
    options {
        timeout(time: 30, unit: 'MINUTES')
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }
    
    environment {
        // Configuration Docker
        DOCKER_REGISTRY = "docker.io"
        DOCKER_IMAGE = "ahmeddhib/students-management"
        DOCKER_TAG = "${env.BUILD_NUMBER}"
        
        // Configuration Git
        GIT_REPO = "https://github.com/Ahmeddhib/StudentsManagement-DevOps.git"
        GIT_BRANCH = "main"
        
        // Variables de build
        BUILD_TRIGGER = "UNKNOWN"
        COMMIT_MESSAGE = ""
        COMMIT_AUTHOR = ""
    }
    
    triggers {
        // Option A: Polling SCM (toutes les minutes)
        pollSCM('* * * * *')
        
        // Option B: GitHub Hook (à configurer manuellement dans Jenkins)
    }
    
    stages {
        // ===== STAGE 1: Détection du changement =====
        stage('Détection du Commit') {
            steps {
                script {
                    echo "=== DÉTECTION DU CHANGEMENT ==="
                    
                    // Récupérer les informations du commit
                    def changeLogSets = currentBuild.changeSets
                    if (!changeLogSets.isEmpty()) {
                        def entries = changeLogSets[0].items
                        env.COMMIT_MESSAGE = entries[0].msg
                        env.COMMIT_AUTHOR = entries[0].author.fullName
                        env.BUILD_TRIGGER = "GIT_COMMIT"
                        
                        echo "📌 Nouveau commit détecté!"
                        echo "Auteur: ${env.COMMIT_AUTHOR}"
                        echo "Message: ${env.COMMIT_MESSAGE}"
                        echo "Commit ID: ${entries[0].commitId}"
                    } else {
                        env.BUILD_TRIGGER = "MANUAL_OR_POLLING"
                        echo "⚠️ Build déclenché manuellement ou par polling"
                    }
                }
            }
        }
        
        // ===== STAGE 2: Récupération du code =====
        stage('Checkout Git') {
            steps {
                echo "=== RÉCUPÉRATION DU DÉPÔT ==="
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: "*/${env.GIT_BRANCH}"]],
                    extensions: [
                        [$class: 'CleanCheckout'],
                        [$class: 'CloneOption', depth: 1, noTags: false, shallow: true],
                        [$class: 'LocalBranch', localBranch: "**"]
                    ],
                    userRemoteConfigs: [[
                        url: env.GIT_REPO,
                        credentialsId: 'github-credentials'
                    ]]
                ])
                
                // Afficher les derniers commits
                sh '''
                    echo "=== DERNIERS COMMITS ==="
                    git log --oneline -5
                    echo "=== BRANCH ACTUELLE ==="
                    git branch -a
                '''
            }
        }
        
        // ===== STAGE 3: Nettoyage et reconstruction =====
        stage('Clean & Build') {
            steps {
                echo "=== NETTOYAGE ET CONSTRUCTION ==="
                
                // Option A: Avec Maven (Spring Boot)
                sh '''
                    echo "🧹 Nettoyage du projet..."
                    mvn clean
                    
                    echo "🔨 Construction du projet..."
                    mvn compile
                    
                    echo "🧪 Exécution des tests..."
                    mvn test
                    
                    echo "📦 Création du package..."
                    mvn package -DskipTests
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
        
        // ===== STAGE 4: Construction Docker =====
        stage('Build Docker Image') {
            steps {
                echo "=== CONSTRUCTION DE L'IMAGE DOCKER ==="
                
                script {
                    // Vérifier que le Dockerfile existe
                    sh 'ls -la Dockerfile'
                    
                    // Construire l'image
                    sh """
                        docker build \
                            --no-cache \
                            --pull \
                            -t ${env.DOCKER_IMAGE}:${env.DOCKER_TAG} \
                            -t ${env.DOCKER_IMAGE}:latest \
                            .
                    """
                    
                    // Vérifier l'image créée
                    sh "docker images | grep ${env.DOCKER_IMAGE}"
                }
            }
        }
        
        // ===== STAGE 5: Tests Docker =====
        stage('Test Docker Image') {
            steps {
                echo "=== TESTS DE L'IMAGE DOCKER ==="
                
                script {
                    // Lancer un conteneur de test
                    sh """
                        docker run -d \
                            --name test-container \
                            -p 8081:8080 \
                            ${env.DOCKER_IMAGE}:${env.DOCKER_TAG}
                        
                        sleep 10
                        
                        echo "=== VÉRIFICATION DU CONTENEUR ==="
                        docker ps
                        
                        echo "=== LOGS DU CONTENEUR ==="
                        docker logs test-container
                        
                        echo "=== TEST DE SANTÉ ==="
                        curl -f http://localhost:8081/actuator/health || echo "Application en démarrage..."
                        
                        # Nettoyage
                        docker stop test-container
                        docker rm test-container
                    """
                }
            }
        }
        
        // ===== STAGE 6: Publication Docker Registry =====
        stage('Push to Docker Registry') {
            steps {
                echo "=== PUBLICATION SUR DOCKER REGISTRY ==="
                
                script {
                    withCredentials([usernamePassword(
                        credentialsId: 'docker-hub-credentials',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )]) {
                        sh """
                            # Login à Docker Hub
                            echo \$DOCKER_PASS | docker login -u \$DOCKER_USER --password-stdin
                            
                            # Push des images
                            docker push ${env.DOCKER_IMAGE}:${env.DOCKER_TAG}
                            docker push ${env.DOCKER_IMAGE}:latest
                            
                            echo "✅ Images publiées avec succès!"
                        """
                    }
                }
            }
        }
        
        // ===== STAGE 7: Déploiement (Optionnel) =====
        stage('Deploy') {
            when {
                branch 'main'
            }
            steps {
                echo "=== DÉPLOIEMENT ==="
                
                script {
                    sh """
                        # Arrêter l'ancien conteneur
                        docker stop students-app || true
                        docker rm students-app || true
                        
                        # Démarrer le nouveau conteneur
                        docker run -d \
                            --name students-app \
                            --restart always \
                            -p 8080:8080 \
                            ${env.DOCKER_IMAGE}:latest
                        
                        echo "🎉 Application déployée sur le port 8080"
                    """
                }
            }
        }
    }
    
    // ===== POST-BUILD ACTIONS =====
    post {
        always {
            echo "=== NETTOYAGE ==="
            sh '''
                echo "🧹 Nettoyage des conteneurs..."
                docker ps -aq | xargs -r docker rm -f 2>/dev/null || true
                
                echo "🗑️ Nettoyage des images intermédiaires..."
                docker images -f "dangling=true" -q | xargs -r docker rmi 2>/dev/null || true
            '''
            
            // Notification
            script {
                def duration = currentBuild.durationString
                def trigger = env.BUILD_TRIGGER
                
                echo """
                ===== RÉSUMÉ DU BUILD =====
                Numéro: ${env.BUILD_NUMBER}
                Déclencheur: ${trigger}
                Durée: ${duration}
                Statut: ${currentBuild.currentResult}
                Commit: ${env.COMMIT_MESSAGE}
                Auteur: ${env.COMMIT_AUTHOR}
                Image Docker: ${env.DOCKER_IMAGE}:${env.DOCKER_TAG}
                ===========================
                """
            }
        }
        
        success {
            echo "✅ PIPELINE RÉUSSIE !"
            // Notification Slack/Email optionnelle
            // slackSend(color: 'good', message: "Build ${env.BUILD_NUMBER} réussi")
        }
        
        failure {
            echo "❌ PIPELINE ÉCHOUÉE"
            // mail to: 'admin@example.com', subject: "Build failed", body: "Voir ${env.BUILD_URL}"
        }
        
        unstable {
            echo "⚠️ PIPELINE INSTABLE"
        }
    }
}
