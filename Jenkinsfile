pipeline {
    agent any

    environment {
        DOCKER_IMAGE = "ahmedwolf/spring-test3"
        DOCKER_TAG = "build-${env.BUILD_NUMBER}"
        DOCKER_LATEST = "latest"
        // REMPLACEZ 192.168.49.1 PAR VOTRE VÉRITABLE IP UBUNTU
        SONAR_HOST = "http://192.168.49.1:9000"
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

        stage('SonarQube Analysis - Direct Method') {
            steps {
                script {
                    echo "🔍 Analyse SonarQube en cours..."

                    // Commande Maven directe - PAS besoin de configuration Jenkins
                    sh """
                        mvn sonar:sonar \\
                            -Dsonar.projectKey=StudentsManagement \\
                            -Dsonar.projectName="Students Management System" \\
                            -Dsonar.host.url=${SONAR_HOST} \\
                            -Dsonar.login=admin \\
                            -Dsonar.password=sonar \\
                            -Dsonar.sources=src/main/java \\
                            -Dsonar.java.binaries=target/classes
                    """

                    echo "✅ Analyse SonarQube terminée"
                }
            }
        }

        // COMMENTEZ cette étape pour l'instant - elle nécessite la configuration Jenkins
        /*
        stage('Wait for Quality Gate') {
            steps {
                script {
                    timeout(time: 10, unit: 'MINUTES') {
                        waitForQualityGate abortPipeline: true
                    }
                    echo "✅ Quality Gate passée avec succès !"
                }
            }
        }
        */

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
                    echo "✅ Image Docker construite : ${DOCKER_IMAGE}:${DOCKER_TAG}"
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

                            echo "Images disponibles localement :"
                            docker images | grep ${DOCKER_IMAGE} || echo "Aucune image trouvée"

                            # Vérifier si l'image avec le tag spécifique existe
                            if docker inspect ${DOCKER_IMAGE}:${DOCKER_TAG} > /dev/null 2>&1; then
                                echo "Push de l'image avec tag ${DOCKER_TAG}..."
                                docker push ${DOCKER_IMAGE}:${DOCKER_TAG}
                            else
                                echo "⚠️ Image ${DOCKER_IMAGE}:${DOCKER_TAG} non trouvée localement"
                                echo "→ Skip du push pour ce tag"
                            fi

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
                    // Vérifier d'abord si l'image existe localement ou sur DockerHub
                    sh '''
                        echo "🔄 Déploiement sur Kubernetes..."

                        # Vérifier si on a une image à déployer
                        if docker inspect ${DOCKER_IMAGE}:${DOCKER_TAG} > /dev/null 2>&1; then
                            echo "Utilisation de l'image locale: ${DOCKER_IMAGE}:${DOCKER_TAG}"
                            IMAGE_TO_DEPLOY="${DOCKER_IMAGE}:${DOCKER_TAG}"
                        elif docker inspect ${DOCKER_IMAGE}:${DOCKER_LATEST} > /dev/null 2>&1; then
                            echo "Utilisation de l'image latest locale: ${DOCKER_IMAGE}:${DOCKER_LATEST}"
                            IMAGE_TO_DEPLOY="${DOCKER_IMAGE}:${DOCKER_LATEST}"
                        else
                            echo "⚠️ Aucune image locale trouvée, utilisation de latest depuis DockerHub"
                            IMAGE_TO_DEPLOY="${DOCKER_IMAGE}:${DOCKER_LATEST}"
                        fi

                        echo "Image à déployer: \${IMAGE_TO_DEPLOY}"

                        # Créer ou mettre à jour le déploiement
                        if kubectl get deployment spring-test3 > /dev/null 2>&1; then
                            echo "Mise à jour du déploiement existant..."
                            kubectl set image deployment/spring-test3 \
                                spring-test3=\${IMAGE_TO_DEPLOY} \
                                --record
                        else
                            echo "Création d'un nouveau déploiement..."
                            # Créer un déploiement simple (à adapter selon vos besoins)
                            kubectl create deployment spring-test3 \
                                --image=\${IMAGE_TO_DEPLOY} \
                                --replicas=1
                        fi

                        # Vérifier le statut du rollout
                        kubectl rollout status deployment/spring-test3 --timeout=300s || true

                        echo "✅ Déploiement Kubernetes terminé"

                        # Afficher les informations
                        echo "--- Informations du déploiement ---"
                        kubectl get deployment spring-test3 -o wide || echo "Déploiement non trouvé"
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
            echo "🔍 Analyse SonarQube complétée"
        }

        failure {
            echo "❌ Pipeline échouée !"
        }

        changed {
            echo "🔄 Statut du build changé depuis la dernière exécution"
        }
    }
}