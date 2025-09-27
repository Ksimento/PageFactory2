#!groovy
/**
* deploy regressman to dev stand
* Authors: demorozov@sberbank.ru
*/

@Library ('jenkins-lib') _

NEXUS_RW_CREDENTIALS_ID = env.GLOBAL_NEXUS_CRED_RW
BITBUCKET_USER_CREDENTIAL_ID = env.GLOBAL_CROWD_CRED
SLACK_BOT_TOKEN = 'regressman_bot_token'
SLACK_APP_TOKEN = 'regressman_app_token'
SERVICE_NAME = 'REGRESSMAN'
K8S_CONFIGS_DIR = 'useful_things/assist_bot/distribution/k8s'
K8S_NAMESPACE = 'qa-infra'
K8S_CREDENTIALS = 'k8s-dev-1'

properties([
    parameters([
        string(
            name: 'DOCKER_TAG',
            defaultValue: 'latest',
            description: 'Введите тег уже собранного образа',
            trim: true
        ),
        string(
            name: 'K8S_CONFIG_BRANCH',
            defaultValue: 'master',
            description: 'Имя ветки откуда берутся конфиги для k8s',
            trim: true
        )
    ])
])

pipeline {
    agent {label 'light-builder'}

    environment {
        DOCKER_TAG = normalizeTag(params.DOCKER_TAG)
    }
    options {
        buildDiscarder(logRotator(numToKeepStr: '30'))
        skipDefaultCheckout()
    }

    stages {
        stage('Set Env') {
            steps {
                script {
                    description =
                            "<br>Deploy: <b>${SERVICE_NAME}</b></br>" +
                            "<br>Docker tag: <b>${params.DOCKER_TAG}</b></br>" +
                            "<br>K8S_branch: <b>${params.K8S_CONFIG_BRANCH}</b></br>"
                    currentBuild.description = description

                    git(
                        url: 'ssh://git@bitbucket.pcbltools.ru:7999/qa/pagefactory-core.git',
                        credentialsId: 'new-school-git',
                        branch: params.K8S_CONFIG_BRANCH,
                        changelog: false,
                        poll: false,
                    )

                    dir (K8S_CONFIGS_DIR) {
                        if (fileExists('configmap/')) {
                            sh "sed -i 's@[[:blank:]]*\$@@' configmap/*.yaml"
                        }
                        sh """
                            grep -rl --include='*.yaml' "TAG_PLACEHOLDER" ./ | xargs -r sed -i "s@TAG_PLACEHOLDER@${DOCKER_TAG}@g"
                        """
                        echo '[INFO] check docker tag'
                        dir ('deployment') {
                            harborImageCheckTagByYaml(DEBUG: false)
                        }
                    }
                }
            }
        }

        stage('Prepare namespace') {
            steps {
                script {
                    dir (K8S_CONFIGS_DIR) {
                        withKubeConfig([credentialsId: K8S_CREDENTIALS, namespace: K8S_NAMESPACE]) {
                            sh 'kubectl delete -f deployment/ --force --wait || true'
                            sh 'kubectl delete -f configmap/ --wait || true'
                            sh 'kubectl delete -f secrets/ --wait || true'

                            withCredentials([usernamePassword(credentialsId: NEXUS_RW_CREDENTIALS_ID, usernameVariable: 'NEXUS_USERNAME', passwordVariable: 'NEXUS_PASSWORD')]) {
                                withCredentials([usernamePassword(credentialsId: BITBUCKET_USER_CREDENTIAL_ID, usernameVariable: 'BB_USERNAME', passwordVariable: 'BB_PASSWORD')]) {
                                    withCredentials([string(credentialsId: SLACK_BOT_TOKEN, variable: 'BOT_TOKEN')]) {
                                        withCredentials([string(credentialsId: SLACK_APP_TOKEN, variable: 'APP_TOKEN')])
                                    }
                                }
                            }

                            if (fileExists('configmap/')) {
                                sh 'kubectl create -f configmap/'
                            }
                        }
                    }
                }
            }
        }

        stage('Deploy Backend') {
            steps {
                script {
                    dir ("${K8S_CONFIGS_DIR}") {
                        withKubeConfig([credentialsId: K8S_CREDENTIALS, namespace: K8S_NAMESPACE]) {
                            sh 'kubectl create -f deployment/'
                            FILES = sh (returnStdout: true, script: 'ls deployment/*.yaml')
                            for (String file : FILES.split()) {
                                sh "kubectl rollout status -f ${file} --timeout 5m"
                            }
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                withKubeConfig([credentialsId: K8S_CREDENTIALS, namespace: K8S_NAMESPACE]) {
                    sh 'kubectl get pod'
                }
                def causes = currentBuild.getBuildCauses()
                if (causes.upstreamProject[0] == null || currentBuild.result != 'SUCCESS') {
                    slackNotify(
                        message: currentBuild.result + '\n' + description.replaceAll('<br>', '').replaceAll(/<b>|<\/b>/, '`').replaceAll('</br>', '\n'),
                        status: currentBuild.result,
                        attachments: getAttachments()
                    )
                }
            }
        }
        cleanup {
            cleanWs()
        }
    }
}

def getAttachments() {
    attachments =  [[ actions: [[ type: 'button', text: 'Pipeline', url: env.BUILD_URL ],]]]
    return attachments
}