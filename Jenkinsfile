pipeline {
    agent any

    environment {
        // ⭐ CORRECTION: Utilisez votre vrai repository Docker Hub
        DOCKER_IMAGE = "ahmedwolf/spring-test1"
        DOCKER_TAG = "latest"
    }

    stages {
        // ===== STAGE 1: CHECKOUT =====
        stage('Checkout') {
            steps {
                echo "📥 CHECKOUT CODE"
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: '*/main']],
                    userRemoteConfigs: [[
                        url: 'https://github.com/Ahmeddhib/StudentsManagement-DevOps.git',
                        credentialsId: 'ahmedwolf'  // ⭐ AJOUTER CREDENTIALS
                    ]],
                    extensions: [[$class: 'CloneOption', shallow: true, depth: 1]]
                ])
                
                sh '''
                    echo "✅ Code récupéré"
                    ls -la
                    echo ""
                    echo "🔍 Vérification Dockerfile:"
                    if [ -f "Dockerfile" ]; then
                        echo "✅ Dockerfile trouvé"
                        cat Dockerfile
                    else
                        echo "❌ Dockerfile non trouvé!"
                        exit 1
                    fi
                '''
            }
        }

        // ===== STAGE 2: VÉRIFICATION DOCKER =====
        stage('Vérification Docker') {
            steps {
                script {
                    echo "🔍 VÉRIFICATION DOCKER"
                    
                    // Test 1: Docker est-il installé?
                    sh '''
                        echo "=== DOCKER VERSION ==="
                        docker --version || echo "❌ Docker non installé"
                    '''
                    
                    // Test 2: L'image existe-t-elle déjà?
                    try {
                        sh "docker image inspect ${env.DOCKER_IMAGE}:${env.DOCKER_TAG} > /dev/null 2>&1"
                        echo "✅ Image Docker existe déjà"
                        env.BUILD_NEEDED = "false"
                    } catch (Exception e) {
                        echo "ℹ️ Image Docker n'existe pas, besoin de build"
                        env.BUILD_NEEDED = "true"
                    }
                    
                    echo "Build nécessaire? ${env.BUILD_NEEDED}"
                }
            }
        }

        // ===== STAGE 3: BUILD MAVEN =====
        stage('Build Maven') {
            when {
                expression { env.BUILD_NEEDED == "true" }
            }
            steps {
                echo "🔨 BUILD MAVEN"
                
                sh '''
                    echo "🧹 Nettoyage..."
                    mvn clean || echo "Clean échoué, continuation..."
                    
                    echo "📦 Packaging..."
                    mvn package -DskipTests -B
                    
                    echo "✅ JAR créé:"
                    ls -lh target/*.jar
                '''
            }
        }

        // ===== STAGE 4: BUILD DOCKER =====
        stage('Build Docker Image') {
            when {
                expression { env.BUILD_NEEDED == "true" }
            }
            steps {
                echo "🐳 BUILD DOCKER IMAGE"
                
                script {
                    // D'abord, télécharger les images de base
                    sh '''
                        echo "📥 Téléchargement des images de base..."
                        docker pull maven:3.8.6-amazoncorretto-17 || true
                        docker pull amazoncorretto:17-alpine || true
                    '''
                    
                    // Ensuite builder
                    sh """
                        echo "🔨 Construction de l'image Docker..."
                        docker build \\
                            --no-cache \\
                            -t ${env.DOCKER_IMAGE}:${env.DOCKER_TAG} \\
                            .
                        
                        echo "✅ Image construite:"
                        docker images | grep ${env.DOCKER_IMAGE}
                    """
                }
            }
        }

        // ===== STAGE 5: PUSH DOCKER =====
        stage('Push Docker Image') {
            steps {
                echo "📤 PUSH TO DOCKER HUB"
                
                script {
                    // ⭐ CORRECTION: Utilisez le bon ID de credentials
                    withCredentials([usernamePassword(
                        credentialsId: 'docker-hub-credentials',  // ⭐ CORRIGER ICI
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )]) {
                        sh """
                            echo "🔐 Authentification Docker Hub..."
                            echo \$DOCKER_PASS | docker login -u \$DOCKER_USER --password-stdin
                            
                            echo "📤 Pushing image..."
                            docker push ${env.DOCKER_IMAGE}:${env.DOCKER_TAG}
                            
                            echo "✅ Image poussée avec succès!"
                            echo "🔗 https://hub.docker.com/r/${env.DOCKER_IMAGE}"
                        """
                    }
                }
            }
        }
        
        // ===== STAGE 6: TEST DOCKER =====
        stage('Test Docker Image') {
            steps {
                echo "🧪 TEST DOCKER IMAGE"
                
                script {
                    sh """
                        echo "🚀 Lancement du conteneur de test..."
                        
                        # Lancer un conteneur temporaire
                        docker run --rm \\
                            ${env.DOCKER_IMAGE}:${env.DOCKER_TAG} \\
                            java -version
                        
                        echo "✅ Image Docker fonctionnelle!"
                    """
                }
            }
        }
    }

    post {
        always {
            echo "🧹 NETTOYAGE"
            sh '''
                echo "Nettoyage des images intermédiaires..."
                docker images -f "dangling=true" -q | xargs -r docker rmi 2>/dev/null || true
            '''
            
            echo "📊 RÉSUMÉ DU BUILD"
            echo "Image: ${env.DOCKER_IMAGE}:${env.DOCKER_TAG}"
            echo "Statut: ${currentBuild.currentResult}"
        }
        
        success {
            echo "🎉 PIPELINE RÉUSSIE!"
        }
        
        failure {
            echo "❌ PIPELINE ÉCHOUÉE"
            echo "Consultez les logs pour les erreurs"
        }
    }
}