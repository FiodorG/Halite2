del /S *.hlt
del /S *.class
del /S *.log

javac MyBot.java
javac MyBotRush.java

halite -t -d "320 240" -s 2950638373 "java MyBotRush" "java MyBot" "java MyBot" "java -agentlib:jdwp=transport=dt_socket,server=n,address=1045,suspend=y,quiet=y MyBot"
pause