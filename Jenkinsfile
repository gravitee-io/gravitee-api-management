node() {
    def redisHost = "-DREDIS_HOST=${env.REDIS_TEST_HOST}"
    def redisPort = "-DREDIS_PORT=${env.REDIS_TEST_PORT}"
    def mvnArgs = ["-U", redisHost, redisPort, "clean", "deploy"]

    stage "Checkout"
    checkout scm

    stage "Build"

    def mvnHome = tool 'MVN33'
    def javaHome = tool 'JDK 8'
    def nodeHome = tool 'NodeJS 0.12.4'
    withEnv(["PATH+MAVEN=${mvnHome}/bin",
             "PATH+NODE=${nodeHome}/bin",
             "JAVA_HOME=${javaHome}"]) {
        def mvnCommamd = ["${mvnHome}/bin/mvn"] + mvnArgs
        sh "${mvnCommamd.join(" ")}"
        try {
            sh "ls **/target/surefire-reports/TEST-*.xml"
            step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/TEST-*.xml'])
        } catch (Exception ex) {
            echo "No tests to archive"
        }
    }
}