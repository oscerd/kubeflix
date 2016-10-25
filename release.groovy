#!/usr/bin/groovy
def updateDependencies(source){

  def properties = []
  // lets disable property update as FIS releases break it!
  //properties << ['<fabric8.version>','io/fabric8/kubernetes-generator']
  //properties << ['<fabric8.maven.plugin.version>','io/fabric8/fabric8-maven-plugin']
  //properties << ['<kubernetes-client.version>','io/fabric8/kubernetes-client']
  //properties << ['<spring-cloud-kubernetes.version>','io/fabric8/spring-cloud-kubernetes-core']

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
    artifactExtensionToWatchInCentral = 'jar'
    promoteToDockerRegistry = 'docker.io'
    dockerOrganisation = 'fabric8'
    imagesToPromoteToDockerHub = ['hystrix-dashboard','turbine-server']
    extraImagesToTag = null
  }
}

def updateDownstreamDependencies(stagedProject) {
  pushPomPropertyChangePR {
    propertyName = 'kubeflix.version'
    projects = [
            'fabric8io/fabric8-platform',
            'fabric8io/fabric8-maven-dependencies',
            'fabric8io/ipaas-platform'
    ]
    version = stagedProject[1]
  }
}

def mergePullRequest(prId){
  mergeAndWaitForPullRequest{
    project = 'fabric8io/kubeflix'
    pullRequestId = prId
  }

}
return this;
