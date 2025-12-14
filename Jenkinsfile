pipeline {
    agent any
    
    environment {
        DOCKERHUB_USER = 'ahmedwolf'
        APP_NAME = 'spring-test3'
        // Tag basé sur le hash du code
        DOCKER_TAG = "build-${env.BUILD_NUMBER}"
        K8S_NAMESPACE = 'default'
        K8S_DEPLOYMENT = 'spring-test3'
    }
    
    tools {
        maven 'Maven-3.9.9'  // À adapter à votre configuration
        jdk 'jdk17'          // À adapter
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Check for Code Changes') {
            steps {
                script {
                    echo "🔍 Vérification des changements de code..."
                    
                    // Calculer le hash SHA256 des fichiers sources
                    sh '''
                        # Créer un hash des fichiers sources importants
                        find . -type f \
                          -name "*.java" \
                          -o -name "*.xml" \
                          -o -name "*.properties" \
                          -o -name "*.yml" \
                          -o -name "*.yaml" \
                          -o -name "Dockerfile" \
                          -o -name "pom.xml" \
                          -o -name "Jenkinsfile" \
                          -o -name "*.json" \
                          -o -name "*.html" \
                          -o -name "*.js" \
                          -o -name "*.css" | \
                        sort | \
                        xargs cat 2>/dev/null | \
                        sha256sum | \
                        cut -d' ' -f1 > current_code_hash.txt
                        
                        # Afficher le hash
                        echo "Hash calculé: $(cat current_code_hash.txt)"
                        
                        # Liste des fichiers analysés (pour debug)
                        find . -type f \
                          -name "*.java" \
                          -o -name "*.xml" \
                          -o -name "*.properties" \
                          -o -name "*.yml" \
                          -o -name "*.yaml" \
                          -o -name "Dockerfile" \
                          -o -name "pom.xml" | wc -l > file_count.txt
                    '''
                    
                    // Lire le hash courant
                    currentHash = readFile('current_code_hash.txt').trim()
                    
                    // Initialiser BUILD_NEEDED
                    BUILD_NEEDED = true
                    
                    // Essayer de lire le hash précédent
                    try {
                        // Lire depuis le workspace (si fichier existe)
                        previousHash = readFile('previous_code_hash.txt').trim()
                        
                        if (currentHash == previousHash) {
                            echo "✅ Aucun changement détecté dans le code source"
                            BUILD_NEEDED = false
                        } else {
                            echo "🔄 Changements détectés! Hash précédent: ${previousHash}"
                            echo "🔄 Hash actuel: ${currentHash}"
                            BUILD_NEEDED = true
                        }
                    } catch (Exception e) {
                        echo "⚠️ Pas de hash précédent trouvé. Premier build?"
                        BUILD_NEEDED = true
                    }
                    
                    // Sauvegarder pour le prochain build
                    writeFile file: 'previous_code_hash.txt', text: currentHash
                    
                    // Afficher le résultat
                    echo "Build nécessaire? ${BUILD_NEEDED}"
                    
                    // Variables pour les étapes suivantes
                    env.BUILD_NEEDED = BUILD_NEEDED
                    env.CODE_HASH = currentHash
                }
            }
        }
        
        stage('Build Maven Project') {
            when {
                expression { return env.BUILD_NEEDED == 'true' }
            }
            steps {
                echo "🏗️  Construction du projet Maven..."
                sh 'mvn clean compile -B'
                sh 'mvn package -DskipTests -B'
                
                // Vérifier que le JAR a été créé
                sh '''
                    if [ -f "target/*.jar" ]; then
                        echo "✅ JAR créé avec succès"
                        ls -la target/*.jar
                    else
                        echo "❌ Aucun JAR trouvé!"
                        exit 1
                    fi
                '''
            }
        }
        
        stage('Check Docker Image Hash') {
            when {
                expression { return env.BUILD_NEEDED == 'true' }
            }
            steps {
                script {
                    echo "🐳 Vérification de l'image Docker existante..."
                    
                    // Vérifier si une image existe déjà avec ce hash
                    try {
                        // Tag de l'image basé sur le hash
                        IMAGE_TAG = "hash-${env.CODE_HASH.take(12)}"
                        env.IMAGE_TAG = IMAGE_TAG
                        
                        // Vérifier si l'image existe localement
                        sh """
                            if docker image inspect ${DOCKERHUB_USER}/${APP_NAME}:${IMAGE_TAG} >/dev/null 2>&1; then
                                echo "✅ Image Docker avec ce hash existe déjà localement"
                                env.DOCKER_BUILD_NEEDED = 'false'
                            else
                                echo "🔄 Image Docker avec ce hash n'existe pas localement"
                                env.DOCKER_BUILD_NEEDED = 'true'
                            fi
                        """
                    } catch (Exception e) {
                        echo "🔄 Build Docker nécessaire"
                        env.DOCKER_BUILD_NEEDED = 'true'
                    }
                }
            }
        }
        
        stage('Build Docker Image') {
            when {
                allOf {
                    expression { return env.BUILD_NEEDED == 'true' }
                    expression { return env.DOCKER_BUILD_NEEDED == 'true' }
                }
            }
            steps {
                script {
                    echo "🐳 Construction de l'image Docker..."
                    
                    // Tag supplémentaire avec la date
                    def dateTag = new Date().format('yyyyMMdd-HHmmss')
                    
                    // Construire l'image avec plusieurs tags
                    sh """
                        docker build -t ${DOCKERHUB_USER}/${APP_NAME}:${env.IMAGE_TAG} \\
                                    -t ${DOCKERHUB_USER}/${APP_NAME}:${env.DOCKER_TAG} \\
                                    -t ${DOCKERHUB_USER}/${APP_NAME}:${dateTag} \\
                                    -t ${DOCKERHUB_USER}/${APP_NAME}:latest .
                    """
                    
                    // Vérifier l'image créée
                    sh """
                        echo "📦 Images Docker créées:"
                        docker images | grep ${APP_NAME}
                        
                        echo "\\n🔍 Détails de l'image:"
                        docker inspect ${DOCKERHUB_USER}/${APP_NAME}:${env.IMAGE_TAG} --format='{{.Created}}'
                    """
                }
            }
        }
        
        stage('Push Docker Image') {
            when {
                allOf {
                    expression { return env.BUILD_NEEDED == 'true' }
                    expression { return env.DOCKER_BUILD_NEEDED == 'true' }
                }
            }
            steps {
                script {
                    echo "📤 Pushing images to DockerHub..."
                    
                    withCredentials([usernamePassword(
                        credentialsId: 'dockerhub-credentials',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )]) {
                        sh """
                            echo \$DOCKER_PASS | docker login -u \$DOCKER_USER --password-stdin
                            
                            # Push toutes les tags
                            docker push ${DOCKERHUB_USER}/${APP_NAME}:${env.IMAGE_TAG}
                            docker push ${DOCKERHUB_USER}/${APP_NAME}:${env.DOCKER_TAG}
                            docker push ${DOCKERHUB_USER}/${APP_NAME}:latest
                            
                            echo "✅ Images pushées avec succès!"
                        """
                    }
                }
            }
        }
        
        stage('Skip Build (No Changes)') {
            when {
                expression { return env.BUILD_NEEDED == 'false' }
            }
            steps {
                script {
                    echo "⏭️  Aucun changement détecté - skip du build"
                    echo "📊 Code hash: ${env.CODE_HASH}"
                    echo "ℹ️  L'image existante sera utilisée"
                    
                    // Trouver la dernière image tag basée sur le hash
                    sh """
                        # Essayer de trouver l'image avec le hash courant
                        if docker images | grep -q "${APP_NAME}.*hash-${env.CODE_HASH.take(12)}"; then
                            echo "✅ Image trouvée localement"
                        else
                            echo "⚠️ Image non trouvée localement, pull depuis DockerHub"
                            docker pull ${DOCKERHUB_USER}/${APP_NAME}:latest
                        fi
                    """
                }
            }
        }
        
        stage('Deploy to Kubernetes') {
            steps {
                script {
                    echo "🚀 Déploiement sur Kubernetes..."
                    
                    // Déterminer quelle image utiliser
                    if (env.BUILD_NEEDED == 'true' && env.DOCKER_BUILD_NEEDED == 'true') {
                        // Nouvelle image construite
                        DEPLOY_IMAGE = "${DOCKERHUB_USER}/${APP_NAME}:${env.IMAGE_TAG}"
                    } else if (env.BUILD_NEEDED == 'true' && env.DOCKER_BUILD_NEEDED == 'false') {
                        // Image existante avec le même hash
                        DEPLOY_IMAGE = "${DOCKERHUB_USER}/${APP_NAME}:${env.IMAGE_TAG}"
                    } else {
                        // Aucun changement - utiliser latest
                        DEPLOY_IMAGE = "${DOCKERHUB_USER}/${APP_NAME}:latest"
                    }
                    
                    env.DEPLOY_IMAGE = DEPLOY_IMAGE
                    
                    echo "🎯 Image à déployer: ${DEPLOY_IMAGE}"
                    
                    // Vérifier si le déploiement existe
                    sh """
                        if kubectl get deployment ${K8S_DEPLOYMENT} -n ${K8S_NAMESPACE} >/dev/null 2>&1; then
                            echo "🔄 Mise à jour du déploiement existant"
                            kubectl set image deployment/${K8S_DEPLOYMENT} \\
                                ${APP_NAME}=${DEPLOY_IMAGE} \\
                                -n ${K8S_NAMESPACE} --record
                        else
                            echo "🆕 Création d'un nouveau déploiement"
                            # Créer un déploiement simple si inexistant
                            kubectl create deployment ${K8S_DEPLOYMENT} \\
                                --image=${DEPLOY_IMAGE} \\
                                -n ${K8S_NAMESPACE}
                            
                            # Exposer le service
                            kubectl expose deployment ${K8S_DEPLOYMENT} \\
                                --port=8080 \\
                                --target-port=8080 \\
                                -n ${K8S_NAMESPACE}
                        fi
                        
                        # Attendre le rollout
                        echo "⏳ Attente du déploiement..."
                        kubectl rollout status deployment/${K8S_DEPLOYMENT} \\
                            -n ${K8S_NAMESPACE} --timeout=300s
                    """
                }
            }
        }
        
        stage('Verify Deployment') {
            steps {
                script {
                    echo "🔍 Vérification du déploiement..."
                    
                    sh """
                        # Vérifier les pods
                        echo "📋 Pods:"
                        kubectl get pods -n ${K8S_NAMESPACE} -l app=${K8S_DEPLOYMENT}
                        
                        # Vérifier le déploiement
                        echo "\\n🎯 Déploiement:"
                        kubectl get deployment ${K8S_DEPLOYMENT} -n ${K8S_NAMESPACE} -o wide
                        
                        # Vérifier l'image utilisée
                        echo "\\n🐳 Image déployée:"
                        kubectl get deployment ${K8S_DEPLOYMENT} -n ${K8S_NAMESPACE} \\
                            -o jsonpath='{.spec.template.spec.containers[0].image}'
                        
                        # Vérifier le service
                        echo "\\n🔌 Service:"
                        kubectl get service -n ${K8S_NAMESPACE} | grep ${K8S_DEPLOYMENT}
                    """
                    
                    // Test de santé (attendre un peu)
                    sleep 10
                    
                    sh """
                        # Test de connectivité
                        POD_NAME=\$(kubectl get pods -n ${K8S_NAMESPACE} -l app=${K8S_DEPLOYMENT} -o jsonpath='{.items[0].metadata.name}')
                        echo "\\n🏥 Test de santé sur le pod: \${POD_NAME}"
                        
                        if kubectl exec -n ${K8S_NAMESPACE} \$POD_NAME -- \\
                           curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health 2>/dev/null; then
                            echo "✅ Application en bonne santé"
                        else
                            echo "⚠️ Application en démarrage..."
                            # Afficher les logs
                            kubectl logs \$POD_NAME -n ${K8S_NAMESPACE} --tail=10
                        fi
                    """
                }
            }
        }
    }
    
    post {
        always {
            echo "🧹 Nettoyage..."
            sh 'docker logout || true'
            
            // Sauvegarder les informations du build
            script {
                def buildInfo = """
                ===============================
                📊 BUILD INFORMATION
                ===============================
                Build Number: ${env.BUILD_NUMBER}
                Build ID: ${env.BUILD_ID}
                Code Hash: ${env.CODE_HASH ?: 'N/A'}
                Image Tag: ${env.IMAGE_TAG ?: 'N/A'}
                Deployed Image: ${env.DEPLOY_IMAGE ?: 'N/A'}
                Build Needed: ${env.BUILD_NEEDED ?: 'N/A'}
                Docker Build Needed: ${env.DOCKER_BUILD_NEEDED ?: 'N/A'}
                Timestamp: ${new Date().format('yyyy-MM-dd HH:mm:ss')}
                ===============================
                """
                
                writeFile file: 'build_info.txt', text: buildInfo
                archiveArtifacts artifacts: 'build_info.txt', fingerprint: true
            }
            
            // Nettoyer les images intermédiaires
            sh '''
                docker system prune -f --filter "until=24h" || true
            '''
        }
        
        success {
            echo "🎉 PIPELINE RÉUSSIE !"
            echo "✅ Code analysé et déployé avec succès"
            
            // Notification simple
            emailext (
                subject: "SUCCÈS Pipeline ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                body: """
                Pipeline ${env.JOB_NAME} #${env.BUILD_NUMBER} terminée avec succès!
                
                Détails:
                - Commit: ${env.GIT_COMMIT}
                - Code Hash: ${env.CODE_HASH ?: 'N/A'}
                - Image: ${env.DEPLOY_IMAGE ?: 'N/A'}
                - Build Log: ${env.BUILD_URL}
                
                Vérification Kubernetes:
                kubectl get pods -l app=${K8S_DEPLOYMENT}
                """,
                to: 'ahmeddhib20@gmail.com',  // Remplacez par votre email
                from: 'jenkins@localhost'
            )
        }
        
        failure {
            echo "💥 PIPELINE ÉCHOUÉE !"
            emailext (
                subject: "ÉCHEC Pipeline ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                body: "Le build ${env.BUILD_URL} a échoué. Consultez les logs pour plus de détails.",
                to: 'ahmeddhib20@gmail.com',  // Remplacez par votre email
                from: 'jenkins@localhost'
            )
        }
        
        unstable {
            echo "⚠️ Pipeline instable"
        }
    }
}
