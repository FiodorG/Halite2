javac MyBot.java
javac MyBotNull.java
halite -d "240 160" -t -s 2168 "java MyBotNull" "java -agentlib:jdwp=transport=dt_socket,server=n,address=1045,suspend=y,quiet=y MyBot"
pause