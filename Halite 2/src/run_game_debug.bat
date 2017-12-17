del /S *.hlt
del /S *.class
del /S *.log

javac MyBot.java
javac MyBotRush.java

halite -t -d "312 208" -s 379717505 "java MyBotRush" "java -agentlib:jdwp=transport=dt_socket,server=n,address=1045,suspend=y,quiet=y MyBot"
pause