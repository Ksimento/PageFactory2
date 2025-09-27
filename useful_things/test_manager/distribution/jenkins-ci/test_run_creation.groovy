#!groovy
/**
 * Джоба создания тест-сета
 * ssh://git@bitbucket.pcbltools.ru:7999/qa/pagefactory-core.git
 */

@Library('jenkins-lib') _

WORK_DIR = 'useful_things/test_manager'

buildEnv = [:]

properties([
        parameters([
                choice(
                        name: 'PROJECT',
                        description: 'Проектная область, в которой создаётся тест-сет',
                        choices: [
                                '',
                                'EDU',
                                'S21'
                        ],
                ),
                string(
                        name: 'VERSION',
                        description: 'Версия для создания тест-сета, например r/31.0.0-rc4',
                        defaultValue: '',
                        trim: true
                ),
                choice(
                        name: 'TEST_RUN_PRESET',
                        choices: [
                                '',
                                'FULL_REGRESS',
                                'FULL_REGRESS_MFE',
                                'SMALL_REGRESS',
                                'SMALL_REGRESS_MFE',
                                'HIGH_PRIORITY_ONLY',
                                'HIGH_PRIORITY_ONLY_MFE',
                                'AUTO_ONLY',
                                'AUTO_ONLY_MFE',
                        ],
                        description: '''
                            <b>FULL_REGRESS</b> - Полный набор тест-кейсов<br>
                            <b>FULL_REGRESS_MFE</b> - Полный набор тест-кейсов с лэйблами MFE<br>
                            <b>SMALL_REGRESS</b> - Все автоматизированные кейсы плюс кейсы HIGH приоритета<br>
                            <b>SMALL_REGRESS_MFE</b> - Все автоматизированные кейсы плюс кейсы HIGH приоритета с лэйблами MFE<br>
                            <b>HIGH_PRIORITY_ONLY</b> - Только тесты HIGH приоритета<br>
                            <b>HIGH_PRIORITY_ONLY_MFE</b> - Только тесты HIGH приоритета с лэйблами MFE<br>
                            <b>AUTO_ONLY</b> - Только автоматизированные кейсы<br>
                            <b>AUTO_ONLY_MFE</b> - Только автоматизированные кейсы с лэйблами MFE<br>
                        '''
                ),
                choice(
                        name: 'RISK_LEVEL',
                        choices: [
                                '',
                                'LOWEST',
                                'LOW',
                                'MEDIUM',
                                'HIGH',
                                'CRITICAL',
                        ],
                        description: '''
                                <b>LOWEST:</b> 1 - Минимальный<br>
                                <b>LOW:</b> 2 - Низкий<br>
                                <b>MEDIUM:</b> 3 - Средний<br>
                                <b>HIGH:</b> 4 - Высокий<br>
                                <b>CRITICAL:</b> 5 - Критический<br>
                        '''
                ),
                booleanParam(
                        name: 'SKIP_RISK_UPDATE',
                        defaultValue: false,
                        description: 'Пропустить обновление рисков'
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
                                " -DtestRunPreset=${params.TEST_RUN_PRESET} " +
                                " -DriskLevel=${params.RISK_LEVEL} " +
                                " -DskipRiskUpdate=${params.SKIP_RISK_UPDATE} " +
                                " -DtestManagerProcess=TEST_RUN_CREATION " +
                                " -Dversion=${params.VERSION} " +
                                " -DjiraProjectKey=${params.PROJECT} ",
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
