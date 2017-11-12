javac MyBotRush.java
javac MyBotNoRush.java

halite -t -d "240 120" "java MyBotNoRush" "java -agentlib:jdwp=transport=dt_socket,server=n,address=1045,suspend=y,quiet=y MyBotRush"
pause