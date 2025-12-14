pipeline {
    agent any

    environment {
        DOCKER_IMAGE = "ahmedwolf/spring-test3"
        // Utiliser un tag unique par build
        DOCKER_TAG = "build-${env.BUILD_NUMBER}"
        DOCKER_LATEST = "latest"
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

        stage('Check for Code Changes') {
            steps {
                script {
                    // Calculer le hash SHA256 du code source
                    sh '''
                        # Calculer le hash de tous les fichiers sources
                        find . -type f \
                            -name "*.java" -o \
                            -name "*.xml" -o \
                            -name "*.properties" -o \
                            -name "*.yml" -o \
                            -name "*.yaml" -o \
                            -name "Dockerfile" -o \
                            -name "pom.xml" -o \
                            -name "Jenkinsfile" | \
                        sort | \
                        xargs cat 2>/dev/null | \
                        sha256sum | \
                        awk '{print $1}' > current_code_hash.txt
                        
                        echo "Hash calculé : $(cat current_code_hash.txt)"
                    '''
                    
                    // Lire le hash actuel
                    def currentHash = readFile('current_code_hash.txt').trim()
                    
                    // Essayer de lire le hash précédent (stocké dans workspace)
                    def previousHash = ""
                    try {
                        previousHash = readFile('previous_code_hash.txt').trim()
                        echo "Hash précédent trouvé : ${previousHash}"
                    } catch(e) {
                        echo "Aucun hash précédent trouvé (premier build?)"
                        previousHash = ""
                    }
                    
                    // Comparer les hashs
                    if (currentHash != previousHash) {
                        echo "🔄 Changements détectés dans le code !"
                        env.BUILD_NEEDED = "true"
                    } else {
                        echo "✅ Aucun changement dans le code"
                        env.BUILD_NEEDED = "false"
                    }
                    
                    // Sauvegarder le hash actuel pour le prochain build
                    writeFile file: 'previous_code_hash.txt', text: currentHash
                    
                    echo "Build nécessaire ? ${env.BUILD_NEEDED}"
                }
            }
        }

        stage('Build Maven Project') {
            when {
                expression { env.BUILD_NEEDED == "true" }
            }
            steps {
                sh 'mvn clean install -DskipTests -B'
            }
        }

        stage('Build Docker Image') {
            when {
                expression { env.BUILD_NEEDED == "true" }
            }
            steps {
                script {
                    // Builder avec le tag unique
                    sh "docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} ."
                    // Tagger aussi en latest pour usage local
                    sh "docker tag ${DOCKER_IMAGE}:${DOCKER_TAG} ${DOCKER_IMAGE}:${DOCKER_LATEST}"
                }
            }
        }

        stage('Push Docker Image') {
            steps {
                script {
                    withCredentials([usernamePassword(
                        credentialsId: 'dockerhub', 
                        usernameVariable: 'DOCKER_USER', 
                        passwordVariable: 'DOCKER_PASS'
                    )]) {
                        sh '''
                            echo "Authentification sur Docker Hub..."
                            echo \$DOCKER_PASS | docker login -u \$DOCKER_USER --password-stdin
                            
                            # Vérifier quelles images sont disponibles localement
                            echo "Images disponibles localement :"
                            docker images | grep ${DOCKER_IMAGE} || echo "Aucune image trouvée"
                            
                            # Push du tag spécifique (toujours faire)
                            echo "Push de l'image avec tag ${DOCKER_TAG}..."
                            docker push ${DOCKER_IMAGE}:${DOCKER_TAG}
                            
                            # Push du tag latest seulement si le build a été fait
                            if [ "${BUILD_NEEDED}" = "true" ]; then
                                echo "Push de l'image avec tag latest..."
                                docker push ${DOCKER_IMAGE}:${DOCKER_LATEST}
                            else
                                echo "Skip push latest (aucun changement détecté)"
                            fi
                        '''
                    }
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                script {
                    // Toujours déployer la dernière version pushée
                    sh '''
                        echo "🔄 Déploiement sur Kubernetes..."
                        
                        # Mettre à jour l'image dans le déploiement
                        kubectl set image deployment/spring-test3 \
                            spring-test3=${DOCKER_IMAGE}:${DOCKER_TAG} \
                            --record
                        
                        # Vérifier le statut du rollout
                        kubectl rollout status deployment/spring-test3 --timeout=300s
                        
                        echo "✅ Déploiement Kubernetes terminé"
                        
                        # Afficher les informations
                        echo "--- Informations du déploiement ---"
                        kubectl get deployment spring-test3 -o wide
                        echo ""
                        kubectl get pods -l app=spring-test3
                    '''
                }
            }
        }

        stage('Verify Deployment') {
            steps {
                script {
                    sh '''
                        echo "🔍 Vérification du déploiement..."
                        
                        # Attendre que les pods soient prêts
                        sleep 10
                        
                        # Récupérer le nom du pod
                        POD_NAME=$(kubectl get pods -l app=spring-test3 -o jsonpath='{.items[0].metadata.name}')
                        
                        # Vérifier quelle image est utilisée
                        echo "Image utilisée dans le pod:"
                        kubectl get pod $POD_NAME -o jsonpath='{.spec.containers[0].image}'
                        echo ""
                        
                        # Vérifier les logs (premières lignes)
                        echo "Logs du pod (dernières 5 lignes):"
                        kubectl logs $POD_NAME --tail=5 || echo "Logs non disponibles encore"
                        
                        # Vérifier la santé de l'application (si elle expose un endpoint health)
                        echo ""
                        echo "Vérification de la santé de l'application..."
                        kubectl port-forward $POD_NAME 8080:8080 &
                        PF_PID=$!
                        sleep 5
                        
                        if curl -s -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
                            echo "✅ Application en bonne santé"
                        else
                            echo "⚠️ Application non accessible sur /actuator/health"
                        fi
                        
                        # Tuer le port-forward
                        kill $PF_PID 2>/dev/null || true
                    '''
                }
            }
        }
    }

    post {
        always {
            echo "🧹 Nettoyage..."
            sh '''
                # Déconnexion Docker
                docker logout || true
                
                # Nettoyer les images temporaires
                docker image prune -f || true
            '''
            
            echo "📊 Résumé du build:"
            echo "Build #${env.BUILD_NUMBER}"
            echo "Image: ${DOCKER_IMAGE}:${DOCKER_TAG}"
            echo "Build nécessaire: ${env.BUILD_NEEDED}"
            
            // Sauvegarder le hash pour les builds suivants
            archiveArtifacts artifacts: 'previous_code_hash.txt', fingerprint: true
        }
        
        success {
            echo "✅ Pipeline terminée avec succès !"
            echo "📦 Image Docker: ${DOCKER_IMAGE}:${DOCKER_TAG}"
            echo "🚀 Déployé sur Kubernetes"
            
            // Envoyer une notification (optionnel)
            emailext (
                subject: "SUCCESS: Pipeline ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                body: """
                ✅ Build réussi !
                
                Détails:
                - Job: ${env.JOB_NAME}
                - Build: #${env.BUILD_NUMBER}
                - Image: ${DOCKER_IMAGE}:${DOCKER_TAG}
                - Changements détectés: ${env.BUILD_NEEDED}
                - URL: ${env.BUILD_URL}
                
                Déploiement Kubernetes vérifié.
                """,
                to: 'ahmed@example.com', // Remplacez par votre email
                attachLog: true
            )
        }
        
        failure {
            echo "❌ Pipeline échouée !"
            
            emailext (
                subject: "FAILURE: Pipeline ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                body: """
                ❌ Build échoué !
                
                Détails:
                - Job: ${env.JOB_NAME}
                - Build: #${env.BUILD_NUMBER}
                - URL: ${env.BUILD_URL}
                
                Consultez les logs pour plus de détails.
                """,
                to: 'ahmed@example.com',
                attachLog: true
            )
        }
        
        changed {
            echo "🔄 Statut du build changé depuis la dernière exécution"
        }
    }
}
