import groovy.json.JsonSlurper
import hudson.slaves.EnvironmentVariablesNodeProperty
import com.cloudbees.plugins.credentials.CredentialsProvider
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials
import jenkins.model.Jenkins

println ((new Date()).format("yyyy-MM-dd hh:mm:ss") + " artifactItemList begin")
//def Map<String, String> bindingMap = binding.getVariables()
//bindingMap.each{entry -> 
//	println ("binding key: " + entry.key + ", binding value: " + entry.value)
//}
def deploymentCredentialsId = binding.getVariable("deploymentCredentialsId")
println ("deploymentCredentialsId: " + deploymentCredentialsId)
def projectName = binding.getVariable("projectName")
println ("projectName: " + projectName)

//def jenkins = binding.getVariable("jenkins")
//def globalNodeProperties = jenkins.getGlobalNodeProperties()
//def envVarsNodePropertyList = globalNodeProperties.getAll(EnvironmentVariablesNodeProperty.class);
//def envVars = envVarsNodePropertyList.get(0).getEnvVars();
//println ("envVars: " + envVars);
//def deploymentUsername = envVars.get(deploymentCredentialsId)


//def deploymentPassword = getPassword(deploymentUsername)
//println deploymentPassword
def deploymentCredentials = getCredentials(deploymentCredentialsId)
//println ("deploymentCredentials: "+deploymentCredentials)

try {
    List<String> artifacts = new ArrayList<>()
	List<String> artifactItemList = new ArrayList<>()
    def releasesArtifactsUrl = "http://localhost:8081/service/rest/v1/components?repository=maven-releases"          
    def snapshotsArtifactsUrl = "http://localhost:8081/service/rest/v1/components?repository=maven-snapshots"          
    def releasesArtifactsJson = ["curl", "-s", "-u", deploymentCredentials, "-H", "accept: application/json", "-k", "--url", "${releasesArtifactsUrl }"].execute().text
    def snapshotsArtifactsJson = ["curl", "-s", "-u", deploymentCredentials, "-H", "accept: application/json", "-k", "--url", "${snapshotsArtifactsUrl }"].execute().text

    // parse releases json
    def releasesArtifacts = parseAndGetArtifactList(releasesArtifactsJson, projectName, true)
    // parse snapshots json
    def snapshotsArtifacts = parseAndGetArtifactList(snapshotsArtifactsJson, projectName, false)

    artifacts.addAll(releasesArtifacts)
    artifacts.addAll(snapshotsArtifacts)
	
	artifacts.sort() {a,b -> b.version <=> a.version } // sort on version in reverese
	
	for(artifact in artifacts){
		artifactItemList.add(artifact.releaseOrSnapshot + " " + artifact.version)
	}
	println ((new Date()).format("yyyy-MM-dd hh:mm:ss") + " artifactItemList end")
    return artifactItemList
} catch (Exception e) {
    print "There was a problem fetching the artifacts"
}

def parseAndGetArtifactList(artifactsJson, itemName, isRelease) {
    List<Artifact> artifacts = new ArrayList<>()
    def jsonSlurper = new JsonSlurper()
    def artifactsJsonObject = jsonSlurper.parseText(artifactsJson)
    def items = artifactsJsonObject.items
    def releaseOrSnapshot = isRelease ? "RELEASE" : "SNAPSHOT"
    for(item in items){
        if (item.name == itemName) {
			artifact = new Artifact(version: item.version, releaseOrSnapshot: releaseOrSnapshot)
			artifacts.add(artifact)
		}
    } 
    return artifacts;
}

//
//  Lookup the credentials by credentialsId and return the
//	username and password seperated by a colon i.e. username:password
//
def getCredentials(credentialsId) {
    def creds = CredentialsProvider.lookupCredentials(
        StandardUsernamePasswordCredentials.class,
        Jenkins.instance
    )

    def c = creds.findResult { it.id == credentialsId ? it : null }

    if ( c ) {
        println "Found credentials id: ${c.id}"

        def systemCredentialsProvider = Jenkins.instance.getExtensionList(
            'com.cloudbees.plugins.credentials.SystemCredentialsProvider'
            ).first()

        def password = systemCredentialsProvider.credentials.first().password
		//println ("password.getPlainText(): "+password.getPlainText())
	    return c.username + ":" + password.getPlainText()

    } else {
        println "Could not find credentials for username: ${username}"
    }
}

class Artifact{
    String version
	String releaseOrSnapshot
}