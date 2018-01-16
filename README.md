# smartnews-push-improvement

Pseudo codes for an article of SmartNews Engineering Blog.

```bash
$ mvn clean packge

# usage: java -jar target/push-improvement-jar-with-dependencies.jar {numberOfSQSMessages} {numberOfDataInMessage} {senderType}
# You need to modify some constants in ApnsUtils to connect to APNs with your credentials.

$ java -jar target/push-improvement-jar-with-dependencies.jar 100 10 WITH_PROBLEM #Try original implementation
$ java -jar target/push-improvement-jar-with-dependencies.jar 100 10 FIXED1 #Try 1st fix implementation
$ java -jar target/push-improvement-jar-with-dependencies.jar 100 10 FIXED2 #Try final implementation
```
