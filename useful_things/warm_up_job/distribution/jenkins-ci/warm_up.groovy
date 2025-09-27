#!groovy
import java.util.regex.Pattern

/**
 * Джоба выполнения разогревочного прогона Smoke тестов
 * ssh://git@bitbucket.pcbltools.ru:7999/qa/pagefactory-core.git
 */

@Library('jenkins-lib') _

WORK_DIR = 'useful_things/warm_up_job'
NEXUS_RW_CREDENTIALS_ID = env.GLOBAL_NEXUS_CRED_RW

buildEnv = [:]

properties([
        parameters([
                choice(
                        name: 'PROJECT',
                        description: 'Проектная область, в которой выполняется ',
                        choices: [
                                '',
                                'EDU',
                                'MFE',
                                'S21'
                        ],
                ),
                string(
                        name: 'STAND_NAME',
                        description: 'Номер стенда для тестирования dev[X-XX]. Например dev2-20, dev2-14',
                        defaultValue: 'devX-XX',
                        trim: true
                ),
                string(
                        name: 'PASS_PERCENT',
                        description: 'Процент успешного прохождения тестов',
                        defaultValue: '98',
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
                    currentBuild.description =
                            "PROJECT: <b>${params.PROJECT}</b><br>" +
                                    "STAND_NAME: <b>${params.STAND_NAME}</b><br>" +
                                    "PASS_PERCENT: <b>${params.PASS_PERCENT}</b><br>"
                    def errorMessage = ""
                    if (!params.PROJECT) {
                        errorMessage += "<b>Не указан параметр PROJECT</b><br>"
                    }
                    if (!Pattern.compile("^dev\\d-\\d{1,2}\$").matcher(params.STAND_NAME).find()) {
                        errorMessage += "<b>Не указан параметр STAND_NAME</b><br>"
                    }
                    if (!Pattern.compile("^\\d{1,2}\$").matcher(params.PASS_PERCENT).find()) {
                        errorMessage += "<b>Не указан параметр PASS_PERCENT</b><br>"
                    }
                    if (errorMessage != "") {
                        currentBuild.description = errorMessage + currentBuild.description
                        error errorMessage
                    }
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

        stage('Compile') {
            steps {
                mvnExecute(
                        command: "mvn clean compile -U ",
                        creates: buildEnv.creates,
                        workDir: WORK_DIR
                )
            }
        }

        stage('Warm Up') {
            steps {
                script {
                    def counter = 3
                    while (counter > 0) {
                        def downstreamJob = build(
                                job: 'EduPower/QA/qa-java-ui-smoke',
                                wait: true,
                                propagate: false,
                                parameters: [
                                        string(name: 'FRONTEND_BRANCH', value: 'master'),
                                        string(name: 'PROJECT', value: params.PROJECT),
                                        string(name: 'STAND_NAME', value: params.STAND_NAME),
                                ]
                        )
                        try {
                            withCredentials([usernamePassword(credentialsId: NEXUS_RW_CREDENTIALS_ID, usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                                mvnExecute(
                                        command: "mvn exec:java " +
                                                " -Dexec.cleanupDaemonThreads=false " +
                                                " -DjobUrl=${downstreamJob.getAbsoluteUrl()} " +
                                                " -DexpectedPassedPercent=${params.PASS_PERCENT} " +
                                                " -DnexusLogin=${USERNAME} " +
                                                " -DnexusPassword=${PASSWORD} ",
                                        creates: buildEnv.creates,
                                        workDir: WORK_DIR
                                )
                            }
                        } catch (Throwable ignored) {
                            counter--
                            continue
                        }
                        return
                    }
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
