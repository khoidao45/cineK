pipeline {
  agent any

  options {
    timestamps()
    disableConcurrentBuilds()
  }

  environment {
    ACR_NAME         = 'goktacr'
    ACR_LOGIN_SERVER = 'goktacr.azurecr.io'

    IMAGE_TAG    = "${env.BUILD_NUMBER}"
    CINEK_IMAGE  = "${env.ACR_LOGIN_SERVER}/cinek-backend:${env.IMAGE_TAG}"

    DEPLOY_PATH  = '/opt/cinek'
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Test') {
      steps {
        sh './mvnw -q -DskipTests=false test'
      }
    }

    stage('Azure Login + ACR Login') {
      steps {
        withCredentials([
          string(credentialsId: 'AZURE_CLIENT_ID',       variable: 'AZURE_CLIENT_ID'),
          string(credentialsId: 'AZURE_CLIENT_SECRET',   variable: 'AZURE_CLIENT_SECRET'),
          string(credentialsId: 'AZURE_TENANT_ID',       variable: 'AZURE_TENANT_ID'),
          string(credentialsId: 'AZURE_SUBSCRIPTION_ID', variable: 'AZURE_SUBSCRIPTION_ID')
        ]) {
          sh '''
            az login --service-principal -u "$AZURE_CLIENT_ID" -p "$AZURE_CLIENT_SECRET" --tenant "$AZURE_TENANT_ID"
            az account set --subscription "$AZURE_SUBSCRIPTION_ID"
            az acr login --name "$ACR_NAME"
          '''
        }
      }
    }

    stage('Build + Push Docker Image') {
      steps {
        sh '''
          docker build -t "$CINEK_IMAGE" .
          docker push "$CINEK_IMAGE"
        '''
      }
    }

    stage('Deploy To Azure VM') {
      steps {
        withCredentials([
          sshUserPrivateKey(credentialsId: 'AZURE_VM_SSH_KEY', keyFileVariable: 'SSH_KEY', usernameVariable: 'SSH_USER'),
          string(credentialsId: 'AZURE_VM_HOST',           variable: 'VM_HOST'),
          string(credentialsId: 'CINEK_POSTGRES_PASSWORD', variable: 'POSTGRES_PASSWORD'),
          string(credentialsId: 'CINEK_NEO4J_PASSWORD',    variable: 'NEO4J_PASSWORD'),
          string(credentialsId: 'CINEK_REDIS_PASSWORD',    variable: 'REDIS_PASSWORD'),
          string(credentialsId: 'CINEK_JWT_SECRET',        variable: 'JWT_SECRET'),
          string(credentialsId: 'CINEK_APP_BASE_URL',      variable: 'APP_BASE_URL'),
          string(credentialsId: 'CINEK_OAUTH2_REDIRECT_URI', variable: 'OAUTH2_REDIRECT_URI')
        ]) {
          sh '''
            cat > .env.deploy <<EOF
CINEK_IMAGE=$CINEK_IMAGE
POSTGRES_PASSWORD=$POSTGRES_PASSWORD
NEO4J_PASSWORD=$NEO4J_PASSWORD
REDIS_PASSWORD=$REDIS_PASSWORD
JWT_SECRET=$JWT_SECRET
APP_BASE_URL=$APP_BASE_URL
OAUTH2_REDIRECT_URI=$OAUTH2_REDIRECT_URI
EOF

            ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no "$SSH_USER@$VM_HOST" "mkdir -p $DEPLOY_PATH"
            scp -i "$SSH_KEY" -o StrictHostKeyChecking=no docker-compose.prod.yml "$SSH_USER@$VM_HOST:$DEPLOY_PATH/docker-compose.prod.yml"
            scp -i "$SSH_KEY" -o StrictHostKeyChecking=no .env.deploy "$SSH_USER@$VM_HOST:$DEPLOY_PATH/.env"

            ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no "$SSH_USER@$VM_HOST" \
              "cd $DEPLOY_PATH && docker compose -f docker-compose.prod.yml pull && docker compose -f docker-compose.prod.yml up -d --remove-orphans && docker image prune -f"

            rm -f .env.deploy
          '''
        }
      }
    }
  }

  post {
    always {
      sh 'docker logout "$ACR_LOGIN_SERVER" || true'
      cleanWs()
    }
  }
}
