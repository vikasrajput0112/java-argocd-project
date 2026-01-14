pipeline {
    agent any

    environment {
        IMAGE_NAME   = "vikas0112/java-web-app"
        DOCKER_CREDS = "dockerhub-creds"
        GIT_CREDS    = "github-jenkins"
        GIT_REPO     = "https://github.com/vikasrajput0112/java-argocd-project.git"
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    def IMAGE_TAG = "build-${env.BUILD_NUMBER}"
                    sh """
                        cd java-app
                        docker build -t ${IMAGE_NAME}:${IMAGE_TAG} .
                    """
                    env.IMAGE_TAG = IMAGE_TAG
                }
            }
        }

        stage('Push Docker Image') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: DOCKER_CREDS,
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    sh """
                        echo \$DOCKER_PASS | docker login -u \$DOCKER_USER --password-stdin
                        docker push ${IMAGE_NAME}:${IMAGE_TAG}
                    """
                }
            }
        }

        stage('Cleanup Docker Images (Keep ONLY Latest 5)') {
            steps {
                sh """
                    echo "Keeping ONLY latest 5 Docker images for ${IMAGE_NAME}"

                    docker images ${IMAGE_NAME} --format "{{.Repository}}:{{.Tag}}" \
                    | tail -n +6 \
                    | xargs -r docker rmi -f

                    echo "Cleanup completed"
                """
            }
        }

        stage('Update Kubernetes Manifest') {
            steps {
                sh """
                    sed -i 's|image: ${IMAGE_NAME}:.*|image: ${IMAGE_NAME}:${IMAGE_TAG}|' \
                    k8s/java-web-app/deployment.yaml
                """
            }
        }

        stage('Commit & Push Git Changes') {
            steps {
                withCredentials([string(
                    credentialsId: GIT_CREDS,
                    variable: 'GITHUB_TOKEN'
                )]) {
                    sh """
                        git config user.name "jenkins"
                        git config user.email "jenkins@ci.local"

                        git add k8s/java-web-app/deployment.yaml
                        git commit -m "ci: update image to ${IMAGE_TAG}"
                        git push https://${GITHUB_TOKEN}@github.com/vikasrajput0112/java-argocd-project.git HEAD:main
                    """
                }
            }
        }
    }

    post {
        always {
            sh "docker logout || true"
        }
    }
}
