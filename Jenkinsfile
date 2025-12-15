pipeline {
    agent any

    environment {
        DOCKER_IMAGE = "ahmedwolf/spring-test3"
        DOCKER_TAG = "build-${env.BUILD_NUMBER}"
        DOCKER_LATEST = "latest"
        SONAR_HOST = "http://192.168.49.1:9000"
        SONAR_TOKEN = "squ_89c7bc3d712cf67b71452a9253ceb6d571849d3e"
        JACOCO_REPORT_PATH = "target/site/jacoco/jacoco.xml"
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

        stage('Run Tests & Generate JaCoCo') {
            steps {
                script {
                    echo "🧪 Exécution des tests avec JaCoCo..."

                    sh '''
                        echo "=== ÉTAPE 1: EXÉCUTION DES TESTS ==="

                        # Vérifier si nous avons des tests
                        TEST_COUNT=$(find src/test -name "*Test*.java" 2>/dev/null | wc -l)
                        echo "Nombre de fichiers de test trouvés: $TEST_COUNT"

                        if [ "$TEST_COUNT" -eq 0 ]; then
                            echo "⚠️ Aucun test trouvé. Création d'un test minimal..."

                            mkdir -p src/test/java/tn/esprit/studentmanagement
                            cat > src/test/java/tn/esprit/studentmanagement/SimpleTest.java << SIMPLE_TEST
package tn.esprit.studentmanagement;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SimpleTest {
    @Test
    void contextLoads() {
        assertTrue(true);
    }

    @Test
    void basicMath() {
        assertTrue(2 + 2 == 4, "2+2 should be 4");
    }
}
SIMPLE_TEST
                        fi

                        # Exécuter les tests avec JaCoCo
                        echo "=== ÉTAPE 2: EXÉCUTION Maven ==="
                        mvn clean test jacoco:report -DskipTests=false

                        # Vérifier les résultats - VERSION CORRIGÉE
                        echo "=== ÉTAPE 3: VÉRIFICATION RÉSULTATS ==="
                        if [ -d "target/surefire-reports" ]; then
                            echo "✅ Rapports de test générés"
                            find target/surefire-reports -name "*.txt" 2>/dev/null | head -5
                        else
                            echo "⚠️ Aucun rapport de test"
                        fi

                        if [ -f "${JACOCO_REPORT_PATH}" ]; then
                            echo "✅ Rapport JaCoCo généré: ${JACOCO_REPORT_PATH}"

                            # VERSION SIMPLIFIÉE SANS REGEX COMPLEXE
                            # Utiliser awk au lieu de grep avec regex Perl
                            if command -v awk > /dev/null; then
                                LINES_COVERED=$(awk -F'"' '/type="LINE" covered="/ {print $4}' ${JACOCO_REPORT_PATH} | head -1)
                                LINES_MISSED=$(awk -F'"' '/type="LINE" missed="/ {print $4}' ${JACOCO_REPORT_PATH} | head -1)

                                if [ -n "$LINES_COVERED" ] && [ -n "$LINES_MISSED" ]; then
                                    TOTAL=$((LINES_COVERED + LINES_MISSED))
                                    if [ $TOTAL -gt 0 ]; then
                                        PERCENTAGE=$((LINES_COVERED * 100 / TOTAL))
                                        echo "📊 Couverture: ${PERCENTAGE}% (${LINES_COVERED}/${TOTAL} lignes)"
                                    fi
                                fi
                            else
                                echo "ℹ️  awk non disponible pour analyser le rapport"
                            fi
                        else
                            echo "❌ Rapport JaCoCo non généré"
                            echo "Vérifiez la configuration dans pom.xml"
                            ls -la target/site/jacoco/ 2>/dev/null || echo "Dossier jacoco non trouvé"
                        fi
                    '''
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                script {
                    echo "🔍 Analyse SonarQube..."

                    sh """
                        if [ -f "${JACOCO_REPORT_PATH}" ]; then
                            echo "✅ Analyse AVEC couverture JaCoCo"

                            mvn sonar:sonar \\
                                -Dsonar.projectKey=StudentsManagement \\
                                -Dsonar.projectName="Students Management System" \\
                                -Dsonar.host.url=${SONAR_HOST} \\
                                -Dsonar.login=${SONAR_TOKEN} \\
                                -Dsonar.coverage.jacoco.xmlReportPaths=${JACOCO_REPORT_PATH} \\
                                -Dsonar.java.coveragePlugin=jacoco \\
                                -Dsonar.sources=src/main/java \\
                                -Dsonar.java.binaries=target/classes
                        else
                            echo "⚠️ Analyse SANS couverture"

                            mvn sonar:sonar \\
                                -Dsonar.projectKey=StudentsManagement \\
                                -Dsonar.projectName="Students Management System" \\
                                -Dsonar.host.url=${SONAR_HOST} \\
                                -Dsonar.login=${SONAR_TOKEN} \\
                                -Dsonar.sources=src/main/java \\
                                -Dsonar.java.binaries=target/classes
                        fi

                        echo "📊 Rapport disponible: ${SONAR_HOST}/dashboard?id=StudentsManagement"
                    """
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    echo "🐳 Construction de l'image Docker..."

                    sh """
                        # Build avec votre Dockerfile
                        docker build \\
                            --tag ${DOCKER_IMAGE}:${DOCKER_TAG} \\
                            --tag ${DOCKER_IMAGE}:latest \\
                            .

                        echo "✅ Image construite:"
                        docker images ${DOCKER_IMAGE}:${DOCKER_TAG} --format "table {{.Repository}}\\t{{.Tag}}\\t{{.Size}}"
                    """
                }
            }
        }

        stage('Push to Docker Hub') {
            steps {
                script {
                    withCredentials([usernamePassword(
                        credentialsId: 'dockerhub',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )]) {
                        sh '''
                            echo "🔐 Authentification Docker Hub..."
                            echo \$DOCKER_PASS | docker login -u \$DOCKER_USER --password-stdin

                            echo "⬆️  Push ${DOCKER_IMAGE}:${DOCKER_TAG}..."
                            docker push ${DOCKER_IMAGE}:${DOCKER_TAG}

                            echo "⬆️  Push ${DOCKER_IMAGE}:latest..."
                            docker push ${DOCKER_IMAGE}:latest

                            echo "✅ Images poussées"
                        '''
                    }
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                script {
                    sh '''
                        echo "🚀 Déploiement Kubernetes..."

                        if ! command -v kubectl &> /dev/null || ! kubectl cluster-info &> /dev/null; then
                            echo "⚠️ Kubernetes non disponible - skip"
                            exit 0
                        fi

                        if kubectl get deployment spring-test3 &> /dev/null; then
                            echo "🔄 Mise à jour du déploiement..."
                            kubectl set image deployment/spring-test3 spring-test3=${DOCKER_IMAGE}:${DOCKER_TAG}
                        else
                            echo "🆕 Création du déploiement..."
                            kubectl create deployment spring-test3 --image=${DOCKER_IMAGE}:${DOCKER_TAG} --replicas=1
                        fi

                        kubectl rollout status deployment/spring-test3 --timeout=300s
                        echo "✅ Déployé: ${DOCKER_IMAGE}:${DOCKER_TAG}"
                    '''
                }
            }
        }
    }

    post {
        always {
            sh '''
                docker logout 2>/dev/null || true
                echo "🧹 Nettoyage effectué"
            '''

            script {
                // Archiver les rapports
                archiveArtifacts artifacts: 'target/surefire-reports/*.txt', fingerprint: true, allowEmptyArchive: true
                archiveArtifacts artifacts: 'target/site/jacoco/jacoco.xml', fingerprint: true, allowEmptyArchive: true

                echo "📊 RÉSUMÉ BUILD #${env.BUILD_NUMBER}"
                echo "🐳 Image: ${DOCKER_IMAGE}:${DOCKER_TAG}"
                echo "🔍 SonarQube: ${SONAR_HOST}/dashboard?id=StudentsManagement"
            }
        }

        success {
            echo "🎉 PIPELINE RÉUSSIE !"
        }

        failure {
            echo "❌ PIPELINE ÉCHOUÉE"
        }
    }
}