#!groovy

/**
 * Джоба поиска устаревших веток и нотификации в ММ
 * ssh://git@bitbucket.pcbltools.ru:7999/qa/pagefactory-core.git
 */

@Library ('jenkins-lib') _

WORK_DIR = 'useful_things/test_manager'
BITBUCKET_USER_CREDENTIAL_ID = env.GLOBAL_CROWD_CRED

buildEnv = [:]

properties([
        parameters([
                choice(
                        name: 'REPOSITORY',
                        description: 'Репозиторий для которого нужно выполнить проверку',
                        choices: [
                                'EDU_BACK',
                                'EDU_FRONT',
                                'S21_APPLICATION',
                                'S21_APPLICATION_EXAM',
                                'MFE_DEPLOY_VERSION',
                        ],
                ),
        ])
])

pipeline {
    agent { label 'light-builder' }

    options {
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    triggers {
        parameterizedCron('''
            H 8 * * 1 %REPOSITORY=EDU_BACK
            H 8 * * 1 %REPOSITORY=EDU_FRONT
            H 8 * * 1 %REPOSITORY=S21_APPLICATION
            H 8 * * 1 %REPOSITORY=S21_APPLICATION_EXAM
            H 8 * * 1 %REPOSITORY=MFE_DEPLOY_VERSION
        ''')
    }

    stages {
        stage('Set Env') {
            steps {
                script {
                    git(
                            url: 'ssh://git@bitbucket.pcbltools.ru:7999/qa/pagefactory-core.git',
                            credentialsId: 'new-school-git',
                            branch: 'master',
                            changelog: false,
                            poll: false,
                    )
                }
            }
        }

        stage('Test-run create') {
            steps {

                withCredentials([usernamePassword(credentialsId: BITBUCKET_USER_CREDENTIAL_ID, usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                    mvnExecute(
                            command: "mvn clean compile -U exec:java " +
                                    " -Dexec.cleanupDaemonThreads=false " +
                                    " -Drepository=${params.REPOSITORY} " +
                                    " -DtestManagerProcess=OLD_BRANCH_NOTIFICATION " +
                                    ' -DbbLogin=$USERNAME ' +
                                    ' -DbbPassword=$PASSWORD ',
                            creates: buildEnv.creates,
                            workDir: WORK_DIR
                    )
                }
            }
        }
    }

    post {
//        always {
//            script {
//                // DEBUG
//                sh 'printenv'
//                slackNotify(
//                        message: getMessage(),
//                        status: currentBuild.result,
//                        attachments: getAttachments(),
//                )
//            }
//        }
        cleanup {
            cleanWs()
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
    attachments = [[actions: [[type: 'button', text: 'Build', url: env.BUILD_URL],]]]
    return attachments
}
