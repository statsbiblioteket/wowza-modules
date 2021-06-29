#!/usr/bin/env groovy


openshift.withCluster() { // Use "default" cluster or fallback to OpenShift cluster detection


    echo "Hello from the project running Jenkins: ${openshift.project()}"

    //Create template with maven settings.xml, so we have credentials for nexus
    podTemplate(
            inheritFrom: 'kb-jenkins-agent-wowza',
            cloud: 'openshift', //cloud must be openshift
            label: 'agent-with-settings.xml',
            name: 'agent-with-settings.xml',
            volumes: [ //mount the settings.xml
                       secretVolume(mountPath: '/etc/m2', secretName: 'maven-settings')
            ]) {

        //Stages outside a node declaration runs on the jenkins host

        String projectName = encodeName("${JOB_NAME}")
        echo "name=${projectName}"

        try {
            //GO to a node with maven and settings.xml
            node('agent-with-settings.xml') {
                //Do not use concurrent builds
                properties([disableConcurrentBuilds()])

                def mvnCmd = "mvn -s /etc/m2/settings.xml --batch-mode"

                stage('checkout') {
                    checkout scm
                }

                stage('Mvn clean verify') {
                    sh "${mvnCmd} -PallTests clean verify"
                }

                stage('Analyze build results') {            
                    recordIssues aggregatingResults: true, 
                        tools: [java(), 
                                javaDoc(),
                                mavenConsole(),
                                taskScanner(highTags:'FIXME', normalTags:'TODO', includePattern: '**/*.java', excludePattern: 'target/**/*')]                
                }

                stage('Create test project') {
                    recreateProject(projectName)

                    openshift.withProject(projectName) {

                        stage("Create build and deploy application") { 
                            openshift.newBuild("--strategy source", "--binary", "-i kb-infra/kb-s2i-wowza", "--name wowza")
                            openshift.startBuild("wowza", "--from-dir=.", "--follow")
                            openshift.newApp("wowza:latest")
                            openshift.create("route", "edge", "--service=wowza")
                        }
                    }
                }

                stage('Push to Nexus (if Master)') {
                    sh 'env'
                    echo "Branch name ${env.BRANCH_NAME}"
                    if (env.BRANCH_NAME == 'master') {
	                sh "${mvnCmd} clean deploy -DskipTests=true"
                    } else {
	                echo "Branch ${env.BRANCH_NAME} is not master, so no mvn deploy"
                    }
                }

                stage('Promote image') {
                    if (env.BRANCH_NAME == 'master') {
                        configFileProvider([configFile(fileId: "imagePromoter", variable: 'promoter')]) {
                            def promoter = load promoter
                            promoter.promoteImage("wowza", "${projectName}",  "medieplatform", "latest")
                        }
                    } else {
                        echo "Branch ${env.BRANCH_NAME} is not master, so no mvn deploy"
                    }
                }

                stage('Cleanup') {
                    echo "Cleaning up"
                    openshift.selector("project/${projectName}").delete()
                }
            }
        } catch (e) {
            currentBuild.result = 'FAILURE'
            throw e
        } finally {
            configFileProvider([configFile(fileId: "notifier", variable: 'notifier')]) {  
                def notifier = load notifier             
                notifier.notifyInCaseOfFailureOrImprovement(true, "#playground")
            } 
        }
    }
}


private void recreateProject(String projectName) {
    echo "Delete the project ${projectName}, ignore errors if the project does not exist"
    try {
        openshift.selector("project/${projectName}").delete()

        openshift.selector("project/${projectName}").watch {
            echo "Waiting for the project ${projectName} to be deleted"
            return it.count() == 0
        }

    } catch (e) {

    }
//
//    //Wait for the project to be gone
//    sh "until ! oc get project ${projectName}; do date;sleep 2; done; exit 0"

    echo "Create the project ${projectName}"
    openshift.newProject(projectName)
}

/**
 * Encode the jobname as a valid openshift project name
 * @param jobName the name of the job
 * @return the jobname as a valid openshift project name
 */
private static String encodeName(groovy.lang.GString jobName) {
    def jobTokens = jobName.tokenize("/")
    def repo = jobTokens[0]
    if(repo.contains('-')) {
        repo = repo.tokenize("-").collect{it.take(1)}.join("")
    } else {
        repo = repo.take(3)
    }

    def name = ([repo] + jobTokens.drop(1)).join("-")
            .replaceAll("\\s", "-")
            .replaceAll("_", "-")
            .replace("/", '-')
            .replaceAll("^openshift-", "")
            .toLowerCase()
    return name
}

