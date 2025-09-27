#!groovy
package QA

/**
 * Сравнение прогонов по allure отчётам
 *
 */

@Library ('jenkins-lib') _
BITBUCKET_USER_CREDENTIAL_ID = env.GLOBAL_CROWD_CRED //id = "jenkins"
BITBUCKET_URL = env.GLOBAL_BITBUCKET_URL
NEXUS_RW_CREDENTIALS_ID = env.GLOBAL_NEXUS_CRED_RW
REGISTRY_HARBOR_URL = env.GLOBAL_HARBOR_URL
REGISTRY_HARBOR_CREDENTIALS_ID = env.GLOBAL_HARBOR_CRED
SLACK_CREDENTIALS = 'regressman_bot_token'
E2E_REPO = [
        "EDU": "pagefactory-core",
        "S21": "pagefactory-core",
        "UP" : "pagefactory-core",
        "ACC": "pagefactory-core",
        "BTC": "pagefactory-core"
]

properties([
        parameters([
                choice(
                        name: 'JIRA_PROJECT_KEY',
                        choices: [
                                '',
                                'EDU',
                                'S21',
                                'UP',
                                'ACC',
                                'BTC'
                        ],
                        description: 'Код проекта в Jira Test Manager для работы синхронизации с тест-кейсами и тест-сетами'
                ),
                booleanParam(
                        name: 'SET_JOB_LINK_1_AS_FIRST_JOB',
                        defaultValue: false,
                        description: 'Параметр устанавливает последовательность джоб указанную в полях:<br>' +
                                'JOB_LINK_1 - базовая (относительно неё выполняется сравнение)<br>' +
                                'JOB_LINK_2 - сравниваемая<br>' +
                                'Иначе, последовательность будет определена автоматически на основании времени запуска'
                ),
                string(
                        name: 'JOB_LINK_1',
                        defaultValue: '',
                        trim: true,
                        description: 'Ссылка на джобу с автотестами для сравнения отчётов'
                ),
                string(
                        name: 'JOB_LINK_2',
                        defaultValue: '',
                        trim: true,
                        description: 'Ссылка на джобу с автотестами для сравнения отчётов'
                ),
                string(
                        name: 'SLACK_CHANNEL',
                        defaultValue: '',
                        trim: true,
                        description: 'Канал слака для нотификации в случае, если diff покажет новые упавшие тесты'
                )
        ])
])

pipeline {
  agent { label 'light-builder' }

  environment {
    EDU_FRONT_DIR = 'edu_front'
  }

  options {
    buildDiscarder(logRotator(daysToKeepStr: '10', artifactDaysToKeepStr: '10'))
    skipDefaultCheckout()
  }

  stages {
    stage('Env') {
      steps {
        script {
          wrap([$class: 'BuildUser']) {
            USER_ID = env.BUILD_USER_ID
            USER_EMAIL = env.BUILD_USER_EMAIL
            BUILD_URL = env.BUILD_URL
            BUILD_USER = env.BUILD_USER
          }

          PROJECT = params.JIRA_PROJECT_KEY ? params.JIRA_PROJECT_KEY : "EDU"

          APP_INFO = getCommitByDockerTag(
                  '',
                  '',
                  '',
                  'master',
                  JIRA_PROJECT_KEY
          )

          echo APP_INFO.toString()
          // SWAGGER_VERSION = normalizeTag("${APP_INFO.backend_bb_info.branch}-${APP_INFO.backend_bb_info.short_commit}-SNAPSHOT")

          currentBuild.description =

                          "USER: <b>${BUILD_USER}</b>;<br>" +
                          "JIRA_PROJECT_KEY: <b>${params.JIRA_PROJECT_KEY}</b>;<br>" +
                          "JOB_LINK_2: <b>${params.JOB_LINK_1}</b>;<br>" +
                          "JOB_LINK_2: <b>${params.JOB_LINK_2}</b>;<br><br>"


          if (params.SLACK_NOTIFY) {
            slackNotify(
                message: getMessage('BUILD STARTED'),
                color: 'good',
            )
          }

          def errorMsg
          if (!params.JOB_LINK_1 && !params.JOB_LINK_2) {
            errorMsg = 'Для выполнения джобы необходимо указать одно из значений JOB_LINK_1 и JOB_LINK_2'
          }

          if (errorMsg) {
            currentBuild.description = '<p><b>' + errorMsg + '</b><p>' + currentBuild.description;
            error errorMsg
          }
        }
      }
    }

    stage('SCM') {
      steps {
        script {
          dir(env.EDU_FRONT_DIR) {
            checkout([
                    $class           : 'GitSCM',
                    branches         : [[name: APP_INFO.frontend_bb_info.commit]],
                    extensions       : [
                            [$class: 'CheckoutOption', timeout: 20],
                            [$class : 'CloneOption',
                             depth  : 1,
                             noTags : true,
                             shallow: false,
                             timeout: 120]
                    ],
                    userRemoteConfigs: [[
                                                credentialsId: 'new-school-git',
                                                url          : "ssh://git@bitbucket.pcbltools.ru:7999/qa/${E2E_REPO[PROJECT]}.git"]]
            ])
          }
        }
      }
    }

    stage('Allure diff') {

      options {
        timeout(time: 15, unit: 'MINUTES')
      }
      steps {
        script {
          withCredentials([usernamePassword(credentialsId: NEXUS_RW_CREDENTIALS_ID, usernameVariable: 'NEXUS_USERNAME', passwordVariable: 'NEXUS_PASSWORD')]) {
            withCredentials([string(credentialsId: SLACK_CREDENTIALS, variable: 'BOT_TOKEN')]) {
              mvnExecute(
                      command: "mvn clean install -U exec:java " +
                              "-Dexec.cleanupDaemonThreads=false" +
                              " -DjiraProjectKey=${params.JIRA_PROJECT_KEY}" +
                              " -DjobA=${params.JOB_LINK_1}" +
                              " -DjobB=${params.JOB_LINK_2}" +
                              " -DnexusLogin=${NEXUS_USERNAME}" +
                              " -DnexusPassword=${NEXUS_PASSWORD}" +
                              " -DslackChannel=${params.SLACK_CHANNEL}" +
                              " -DbotToken=${BOT_TOKEN}" +
                              " -DbuildId=${env.BUILD_NUMBER}" +
                              " -DuseSpecifiedSequence=${params.SET_JOB_LINK_1_AS_FIRST_JOB}",
                      creates: [],
                      workDir: "${env.EDU_FRONT_DIR}/useful_things/allure_comparator",
              )
            }
          }
        }
      }
    }
  }

  post {
    success {
      dir("${env.EDU_FRONT_DIR}/useful_things/allure_comparator/target") {
        archiveArtifacts artifacts: 'diff-report/*.*'
      }
      script {
        slackNotify(
            message: 'Сборка отчёта завершена ' + BUILD_URL + 'artifact/diff-report/index.html',
            status: currentBuild.result,
        )
      }
    }
    failure {
      script {
        slackNotify(
            message: 'Ошибка при выполнении сборки ' + BUILD_URL,
            status: currentBuild.result,
        )
      }
    }

    cleanup {
      cleanupWorkspace([
              // env.EDU_BACK_DIR,
              env.EDU_FRONT_DIR
      ])
    }
  }
}


def cleanupWorkspace(directories) {
  // CleanWs не работает с поддиректориями,
  // поэтому в цикле удалаяем все сожержимое поддиректорий,
  // кроме вложенных .git
  directories.each { directory ->
    dir(directory) {
      cleanWs(
              patterns: [
                      [pattern: '**/.git/**', type: 'EXCLUDE'],
              ],
              deleteDirs: true
      )
    }
  }
}

def getMessage(title = 'BUILD') {
  message = """
        *${title}*
        *Job:* ${env.JOB_NAME}
        *Frontend branch:* ${APP_INFO.frontend_bb_info.branch}
        *Frontend commit:* ${APP_INFO.frontend_bb_info.short_commit}
        *Стенд:* ${params.STAND_NAME}
        *Jenkins:* ${env.BUILD_URL}
    """.stripIndent()
  return message
}

def getCommitByDockerTag(backendDockerTag, frontendDockerTag, backendBranch='', frontendBranch='', project) {
  if (!project) {
    errorMsg = 'Параметр project является обязательным'
    currentBuild.description = '<p><b>' + errorMsg + '</b></p>'
    throw new Exception(errorMsg)
  }
  if (backendDockerTag) {
    backendTagInfo = getTagInfo('edupower-team-service', backendDockerTag, "${REGISTRY_HARBOR_CREDENTIALS_ID}", "${REGISTRY_HARBOR_URL}")
    echo backendTagInfo.toString()
    backendBbInfo = getBbInfo(backendTagInfo.tag, "${BITBUCKET_USER_CREDENTIAL_ID}", "${BITBUCKET_URL}", 'edu-back')
  } else if (backendBranch) {
    backendTagInfo = [
            'hash_commit': '',
            'push_time': '',
            'tag': ''
    ]
    backendBbInfo = getBbInfoByBranch(backendBranch, "${BITBUCKET_USER_CREDENTIAL_ID}", "${BITBUCKET_URL}", 'edu-back')
  } else {
    backendTagInfo = [
            'hash_commit': '',
            'push_time': '',
            'tag': ''
    ]
    backendBbInfo = [
            'branch': '',
            'commit': '',
            'short_commit': ''
    ]
  }

  frontendTagInfo = [
          'hash_commit': '',
          'push_time': '',
          'tag': ''
  ]

  frontendBbInfo = [
          'branch': '',
          'commit': '',
          'short_commit': ''
  ]

  if (frontendDockerTag) {
    frontendTagInfo = getTagInfo('edupower-frontend', frontendDockerTag, "${REGISTRY_HARBOR_CREDENTIALS_ID}", "${REGISTRY_HARBOR_URL}")
    echo frontendTagInfo.toString()
    frontendBbInfo = getBbInfo(frontendTagInfo.tag, "${BITBUCKET_USER_CREDENTIAL_ID}", "${BITBUCKET_URL}", 'edu-front')
  }

  if (frontendBranch) {
    echo 'frontendBranch: ' + frontendBranch
    echo 'repo: ' + E2E_REPO[project]
    frontendBbInfo = getBbInfoByBranch(frontendBranch, "${BITBUCKET_USER_CREDENTIAL_ID}", "${BITBUCKET_URL}", E2E_REPO[project])
  }

  return [
          'backend_tag_info': backendTagInfo,
          'backend_bb_info': backendBbInfo,
          'frontend_tag_info': frontendTagInfo,
          'frontend_bb_info': frontendBbInfo,
  ]
}

def getBbInfoByBranch(branch, bbСredId, bbUrl, repo='edupower'){
  def response = httpRequest authentication: "${bbСredId}", url: "${bbUrl}/bitbucket/rest/api/1.0/projects/QA/repos/${repo}/commits?until=${branch}&limit=0&start=0"
  def json = readJSON text: response.content
  echo json.toString()
  def fullCommit = json['values'][0]['id']
  return ['branch': branch, 'commit': fullCommit, 'short_commit': fullCommit[0..10]]
}