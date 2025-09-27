#!groovy

/**
 * Джоба для удаления старых веток
 * ssh://git@bitbucket.pcbltools.ru:7999/qa/pagefactory-core.git
 */

@Library ('jenkins-lib') _

WORK_DIR = 'useful_things/test_manager'
BITBUCKET_USER_CREDENTIAL_ID = env.GLOBAL_CROWD_CRED

buildEnv = [:]

pipeline {
    agent { label 'light-builder' }

    options {
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    triggers {
        parameterizedCron('''
            H 6 * * 1
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
                                    " -DtestManagerProcess=DELETE_OLD_BRANCH " +
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
