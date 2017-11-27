del /S *.hlt
del /S *.class
del /S *.log

javac MyBot.java
javac MyBotRush.java

halite -t -d "240 160" -s 2183388166 "java -agentlib:jdwp=transport=dt_socket,server=n,address=1045,suspend=y,quiet=y MyBotRush" "java MyBot"
pause