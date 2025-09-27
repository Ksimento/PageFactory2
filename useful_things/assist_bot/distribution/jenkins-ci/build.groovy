#!groovy
/**
* Сборка Regressman Bot
* ssh://git@bitbucket.pcbltools.ru:7999/qa/pagefactory-core.git
*/

@Library ('jenkins-lib') _

REGISTRY_HARBOR_URL = env.GLOBAL_HARBOR_URL
REGISTRY_HARBOR_ROOT = 'harbor-dev.pcbltools.ru/education-release'
REGISTRY_HARBOR_CREDENTIALS_ID = env.GLOBAL_HARBOR_CRED
SERVICE_NAME = 'REGRESSMAN'
WORK_DIR = 'useful_things/assist_bot'

buildEnv = [:]

properties([
    parameters([
        string(
            name: 'BRANCH',
            defaultValue: 'master',
            trim: true
        ),
        string(
            name: 'DOCKER_TAG',
            defaultValue: 'latest',
            trim: true
        ),
    ])
])

pipeline {
    agent { label 'builder' }

    options {
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    stages {
        stage('Set Env') {
            steps {
                script {
                    git(
                        url: 'ssh://git@bitbucket.pcbltools.ru:7999/qa/pagefactory-core.git',
                        credentialsId: 'new-school-git',
                        branch: params.BRANCH,
                        changelog: false,
                        poll: false,
                    )
                    GIT_COMMIT = sh(returnStdout: true, script: 'git rev-parse --verify HEAD').trim()
                    buildEnv.dockerTag = params.DOCKER_TAG

                    generateDockerList()
                    currentBuild.description = "Build <b>${SERVICE_NAME}</b><br>docker tag: <b>${buildEnv.dockerTag}</b></br>"
                }
            }
        }

        stage('Build') {
            steps {
                mvnExecute(
                    command: 'mvn package -U',
                    creates: buildEnv.creates,
                    workDir: WORK_DIR
                )
            }
        }

        stage('Create Images') {
            steps {
                script {
                    buildDockerImages(
                        registryURL: REGISTRY_HARBOR_URL,
                        registryCredentialId: REGISTRY_HARBOR_CREDENTIALS_ID,
                        dockerList: buildEnv.dockerList,
                        workDir: WORK_DIR
                    )
                }
            }
        }

        stage('Push Images') {
            steps {
                script {
                    pushDockerImages(
                        registryURL: REGISTRY_HARBOR_URL,
                        registryCredentialId: REGISTRY_HARBOR_CREDENTIALS_ID,
                        dockerList: buildEnv.dockerList
                    )
                }
            }
        }
    }

    post {
        always {
            script {
                // DEBUG
                sh 'printenv'
                slackNotify(
                    message: getMessage(),
                    status: currentBuild.result,
                    attachments: getAttachments(),
                )
            }
        }
        cleanup {
            cleanWs()
            cleanDocker(
                dockerList: buildEnv.dockerList
            )
        }
    }
}

def getMessage() {
    message = """\
        *Service:* `${SERVICE_NAME}` BUILD ${currentBuild.result}
        *Branch:* `${params.BRANCH}`
        *Git Commit:* `${GIT_COMMIT}`
        *Docker Tag:* `${buildEnv.dockerTag}`
    """.stripIndent()
    return message
}

def getAttachments() {
    attachments =  [[ actions: [[ type: 'button', text: 'Build', url: env.BUILD_URL ],]]]
    return attachments
}

def generateDockerList() {
    buildEnv.dockerList = readDockerList(
        file: "${WORK_DIR}/distribution/jenkins-ci/docker-list.yml",
        tag: buildEnv.dockerTag,
        registry: REGISTRY_HARBOR_ROOT,
    )

    buildEnv.creates = buildEnv.dockerList. //collect creates folder
        collectMany {
            it.resources ?: []
        }
}
