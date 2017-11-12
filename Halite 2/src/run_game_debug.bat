javac MyBotRush.java
javac MyBotNoRush.java

halite -t -d "240 160" -s 2183388162 "java MyBotRush" "java -agentlib:jdwp=transport=dt_socket,server=n,address=1045,suspend=y,quiet=y MyBotNoRush"
pause