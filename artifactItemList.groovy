import groovy.json.JsonSlurper
println ("artifactItemList begin")
println ("binding: " + binding)
def Map<String, String> bindingMap = binding.getVariables()
bindingMap.each{entry -> 
	println ("binding key: " + entry.key + ", binding value: " + entry.value)
}
def jenkins = binding.getVariable("jenkins")
println ("jenkins: " + jenkins);

def deploymentPassword = getPassword("nexus3_deployment")
//println deploymentPassword

try {
    List<String> artifacts = new ArrayList<String>()
    def releasesArtifactsUrl = "http://localhost:8081/service/rest/v1/components?repository=maven-releases"          
    def snapshotsArtifactsUrl = "http://localhost:8081/service/rest/v1/components?repository=maven-snapshots"          
    def releasesArtifactsJson = ["curl", "-s", "-u", "deployment:"+deploymentPassword, "-H", "accept: application/json", "-k", "--url", "${releasesArtifactsUrl }"].execute().text
    def snapshotsArtifactsJson = ["curl", "-s", "-u", "deployment:"+deploymentPassword, "-H", "accept: application/json", "-k", "--url", "${snapshotsArtifactsUrl }"].execute().text

    // parse releases json
    def releasesArtifacts = parseAndGetArtifactList(releasesArtifactsJson, "FlightLogServer", true)
    // parse snapshots json
    def snapshotsArtifacts = parseAndGetArtifactList(snapshotsArtifactsJson, "FlightLogServer", false)

    artifacts.addAll(releasesArtifacts)
    artifacts.addAll(snapshotsArtifacts)

	println ("artifactItemList end")
    return artifacts
} catch (Exception e) {
    print "There was a problem fetching the artifacts"
}

def parseAndGetArtifactList(artifactsJson, itemName, isRelease) {
    List<String> artifacts = new ArrayList<>()
    def jsonSlurper = new JsonSlurper()
    def artifactsJsonObject = jsonSlurper.parseText(artifactsJson)
    def items = artifactsJsonObject.items
    def artifactPrefix = isRelease ? "RELEASE " : "SNAPSHOT "
    for(item in items){
        if (item.name == itemName)
        artifacts.add(artifactPrefix + item.version)
    } 
    return artifacts;
}

def getPassword(username) {
    def creds = com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials(
        com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials.class,
        jenkins.model.Jenkins.instance
    )

    def c = creds.findResult { it.username == username ? it : null }

    if ( c ) {
        println "found credential ${c.id} for username ${c.username}"

        def systemCredentialsProvider = jenkins.model.Jenkins.instance.getExtensionList(
            'com.cloudbees.plugins.credentials.SystemCredentialsProvider'
            ).first()

      def password = systemCredentialsProvider.credentials.first().password

	  return password.getPlainText()


    } else {
        println "could not find credential for ${username}"
    }
}
