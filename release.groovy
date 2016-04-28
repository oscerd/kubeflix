#!/usr/bin/groovy
def updateDependencies(source){

  def properties = []
  properties << ['<fabric8.version>','io/fabric8/kubernetes-generator']
  properties << ['<kubernetes-client.version>','io/fabric8/kubernetes-client']
  properties << ['<spring-cloud-kubernetes.version>','io/fabric8/spring-cloud-kubernetes-core']
  properties << ['<docker.maven.plugin.version>','io/fabric8/docker-maven-plugin']
  properties << ['<fabric8-maven-plugin.version>','io/fabric8/fabric8-maven-plugin']

  updatePropertyVersion{
    updates = properties
    repository = source
    project = 'fabric8io/kubeflix'
  }
}

def stage(){
  return stageProject{
    project = 'fabric8io/kubeflix'
    useGitTagForNextVersion = true
  }
}

def approveRelease(project){
  def releaseVersion = project[1]
  approve{
    room = null
    version = releaseVersion
    console = null
    environment = 'fabric8'
  }
}

def release(project){
  releaseProject{
    stagedProject = project
    useGitTagForNextVersion = true
    helmPush = false
    groupId = 'io.fabric8.kubeflix'
    githubOrganisation = 'fabric8io'
    artifactIdToWatchInCentral = 'turbine-server'
    artifactExtensionToWatchInCentral = 'war'
    promoteToDockerRegistry = 'docker.io'
    dockerOrganisation = 'fabric8'
    imagesToPromoteToDockerHub = ['hystrix-dashboard','turbine-server']
    extraImagesToTag = null
  }
}

def mergePullRequest(prId){
  mergeAndWaitForPullRequest{
    project = 'fabric8io/kubeflix'
    pullRequestId = prId
  }

}
return this;
