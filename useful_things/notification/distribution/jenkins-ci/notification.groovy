#!groovy
/**
 * Джоба нотификации в каналы
 * ssh://git@bitbucket.pcbltools.ru:7999/qa/pagefactory-core.git
 */

@Library('jenkins-lib') _

WORK_DIR = 'useful_things/notification'

buildEnv = [:]

properties([
        parameters([
                choice(
                        name: 'PROJECT_KEY',
                        choices: [
                                'ALL',
                                'EDU',
                                'S21',
                                'MFE'

                        ],
                        defaultValue: 'ALL',
                        description: 'Выберете проект, MFE проект необходим для смок тестов'
                ),
                string(
                        name: 'SMOCK_STAND',
                        defaultValue: '',
                        trim: true,
                        description: 'Стенд смок тестов'
                ),
                string(
                        name: 'SMOCK_JOB_URL',
                        defaultValue: '',
                        trim: true,
                        description: 'URL прогона смок тестов'
                ),
        ])
])

pipeline {
  agent { label 'light-builder' }
  triggers {
    parameterizedCron('''
            0 10 * * *
        ''')
  }
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
                command: "mvn clean compile -U exec:java" +
                        " -DjiraProjectKey=${params.PROJECT_KEY}"+
                        " -DsmokeUrlStand=${params.SMOCK_STAND}"+
                        " -DsmokeJobUrl=${params.SMOCK_JOB_URL}",
                creates: buildEnv.creates,
                workDir: WORK_DIR
        )
      }
    }
  }

}
