del /S *.hlt
del /S *.class
del /S *.log

javac MyBot.java
javac MyBotRush.java

halite -t -d "336 224" -s 1753065728 "java MyBot" "java -agentlib:jdwp=transport=dt_socket,server=n,address=1045,suspend=y,quiet=y MyBotRush"
pause