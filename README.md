# Jenkins Telegram notifications for pipeline

This project is a custom Groovy Shared Library for sending Telegram notifications.

## Prerequisites

- **Telegram Bot**: A Telegram bot with a token and chat ID for notifications.

## Installation Guide

### Set Up Shared Library

1. **Configure Shared Library in Jenkins**:
   - Go to `Manage Jenkins > Configure System > Global Pipeline Libraries`.
   - Add a library:
     - **Name**: `jenkins-telegram-notify`
     - **Default version**: `main`
     - **Retrieval method**: Git, with the repository URL (e.g., `https://github.com/nazmang/jenkins-telegram-notify.git`).

2. **Create credentials for Telegram Bot Token and chat ID**
   - Go to `Manage Jenkins > Creadentials > Global credentials (unrestricted)`.
   - Create Creadentials as `Secret text`
     - **telegramToken** for Token
     - **telegramChatid** for Chat ID

## Example pipeline

  ```groovy
@Library('jenkins-telegram-notify') _

def deployExecuted = false

pipeline {
    agent any 

    environment {        
        // Telegram configre
        TELEGRAM_BOT_TOKEN = credentials('telegramToken')
        TELEGRAM_CHAT_ID = credentials('telegramChatid')          
    }

    stages {
        stage('Checkout') {
            
            steps {
                git branch: 'master',
                url: 'https://github.com/username/repo.git'
            }
        }       
        stage('Deploy') {
            
            steps {
                script {
                    sh """
                        echo 'Deploying...'
                    """
                }
                script {
                    deployExecuted = true
                }
                script {                    
                    echo "Deploy executed: ${deployExecuted}"
                }
            }
        }
    }

    post {
        success {
            script {
                if (deployExecuted) {
                    def message = "✅ *Jenkins Deploy Success*\n" +
                                  "Job: ${env.JOB_NAME}\n" +
                                  "Build: #${env.BUILD_NUMBER}\n" +                               
                                  "URL: ${env.BUILD_URL}"
                    telegram.sendMessage(env.TELEGRAM_BOT_TOKEN, env.TELEGRAM_CHAT_ID, message)
                } else {
                    echo "Deploy stack was not executed, skipping Telegram notification."
                }
            }
        }
        failure {
            script {
                def message = "❌ *Jenkins Deploy Failed*\n" +
                              "Job: ${env.JOB_NAME}\n" +
                              "Build: #${env.BUILD_NUMBER}\n" +                          
                              "URL: ${env.BUILD_URL}"
                telegram.sendMessage(env.TELEGRAM_BOT_TOKEN, env.TELEGRAM_CHAT_ID, message)
            }
        }
        always {
            cleanWs()
        }
    }
}
  ```

## Troubleshooting

- **Telegram Errors**: Check the bot token and chat ID. Test the Telegram API manually:

  ```bash
  curl -X POST https://api.telegram.org/bot<YOUR_TOKEN>/sendMessage -d chat_id=<CHAT_ID> -d text="Test" -d parse_mode=MarkdownV2
  ```

## License

This project is licensed under the MIT License.
