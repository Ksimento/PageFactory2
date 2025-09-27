#!groovy
/**
 * Джоба генерации отчёта о прохождении регресса
 * ssh://git@bitbucket.pcbltools.ru:7999/qa/pagefactory-core.git
 */

@Library('jenkins-lib') _

WORK_DIR = 'useful_things/test_manager'

buildEnv = [:]

properties([
        parameters([
                string(
                        name: 'TEST_RUN_KEY',
                        description: 'ID тест-сета, например EDU-C1234',
                        defaultValue: '',
                        trim: true
                ),
                choice(
                        name: 'SEND_MESSAGE_TO_CHANNEL',
                        description: 'Отправить сообщение в канал',
                        choices: [
                                '',
                                'qa',
                                's21_testing'
                        ],
                ),
        ])
])

pipeline {
    agent { label 'light-builder' }

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
                            branch: 'master',
                            changelog: false,
                            poll: false,
                    )
                }
            }
        }

        stage('Test-run create') {
            steps {
                mvnExecute(
                        command: "mvn clean compile -U exec:java " +
                                " -Dexec.cleanupDaemonThreads=false " +
                                " -DtestRunKey=${params.TEST_RUN_KEY} " +
                                " -DsendMessageToChannel=${params.SEND_MESSAGE_TO_CHANNEL} " +
                                " -DtestManagerProcess=REGRESS_REPORT ",
                        creates: buildEnv.creates,
                        workDir: WORK_DIR
                )
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
