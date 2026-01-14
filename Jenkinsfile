pipeline {
    agent any

    environment {
        IMAGE_NAME   = "vikas0112/java-web-app"
        DOCKER_CREDS = "dockerhub-creds"
        GIT_CREDS    = "github-jenkins"
        GIT_REPO     = "https://github.com/vikasrajput0112/java-argocd-project.git"
        K8S_SSH_CREDS = "k8s-ssh"  // SSH key for 192.168.136.163
        K8S_HOST = "root@192.168.136.163"
    }

    stages {

        /* --- 1. CHECKOUT CODE --- */
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        /* --- 2. BUILD DOCKER IMAGE --- */
        stage('Build Docker Image') {
            steps {
                script {
                    env.IMAGE_TAG = "build-${env.BUILD_NUMBER}"
                    sh """
                        cd java-app
                        docker build -t ${IMAGE_NAME}:${IMAGE_TAG} .
                    """
                }
            }
        }

        /* --- 3. PUSH DOCKER IMAGE TO DOCKER HUB --- */
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

        /* --- 4. CLEANUP OLD DOCKER IMAGES --- */
        stage('Cleanup Docker Images (Keep ONLY Latest 5)') {
            steps {
                sh """
                    echo "Keeping ONLY latest 5 Docker images for ${IMAGE_NAME}"

                    docker images ${IMAGE_NAME} --format "{{.Repository}}:{{.Tag}}" \
                    | tail -n +3 \
                    | xargs -r docker rmi -f

                    echo "Cleanup completed"
                """
            }
        }

        /* --- 5. REMOVE DANGLING IMAGES --- */
        stage('Cleanup Dangling Docker Images') {
            steps {
                sh '''
                    echo "Removing dangling Docker images (<none>)"

                    docker images -f "dangling=true" -q \
                    | xargs -r docker rmi -f

                    echo "Dangling image cleanup completed"
                '''
            }
        }

        /* --- 6. UPDATE K8s YAML --- */
        stage('Update Kubernetes Manifest') {
            steps {
                sh """
                    sed -i 's|image: ${IMAGE_NAME}:.*|image: ${IMAGE_NAME}:${IMAGE_TAG}|' \
                    k8s/java-web-app/deployment.yaml
                """
            }
        }

        /* --- 7. COMMIT YAML CHANGE BACK TO GITHUB --- */
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
                        git commit -m "ci: update image to ${IMAGE_TAG}" || true
                        git push https://${GITHUB_TOKEN}@github.com/vikasrajput0112/java-argocd-project.git HEAD:main
                    """
                }
            }
        }

        /* --- 8. SSH INTO K8S AND DEPLOY --- */
        stage('Deploy to Kubernetes via SSH') {
            steps {
                sshagent([K8S_SSH_CREDS]) {
                    sh """
                    ssh -o StrictHostKeyChecking=no ${K8S_HOST} "
                        echo 'Deploying updated image to Kubernetes...'

                        cd /root/java-argocd-project-main/k8s/java-web-app

                        kubectl apply -f deployment.yaml
                        kubectl apply -f service.yaml 2>/dev/null || true

                        kubectl rollout restart deployment/java-web-app

                        echo 'Sleeping to wait for pods...'
                        sleep 5

                        kubectl get pods -n default
                    "
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
