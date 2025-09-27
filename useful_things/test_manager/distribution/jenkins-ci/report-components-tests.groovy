#!groovy
/**
 * Джоба для выгрузки отчета по компонентным тестам
 * ssh://git@bitbucket.pcbltools.ru:7999/qa/pagefactory-core.git
 */

@Library('jenkins-lib') _

WORK_DIR = 'useful_things/test_manager'
NEXUS_RW_CREDENTIALS_ID = env.GLOBAL_NEXUS_CRED_RW
buildEnv = [:]

properties([
        parameters([
                string(
                        name: 'DATA_COMPONENTS',
                        description: 'Данные в виде название MFE = SSH URL',
                        defaultValue: '',
                        trim: true
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
                withCredentials([usernamePassword(credentialsId: NEXUS_RW_CREDENTIALS_ID, usernameVariable: 'NEXUS_USERNAME', passwordVariable: 'NEXUS_PASSWORD')]) {
                        mvnExecute(
                                command: "mvn clean compile -U exec:java " +
                                        " -Dexec.cleanupDaemonThreads=false " +
                                        " -DnexusLogin=${NEXUS_USERNAME}" +
                                        " -DnexusPassword=${NEXUS_PASSWORD}" +
                                        " -DtestManagerProcess=REPORT_COMPONENT " +
                                        " -DjobUrl=${params.DATA_COMPONENTS} ",
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
