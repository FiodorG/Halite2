del /S *.hlt
del /S *.class
del /S *.log

javac MyBot.java
javac MyBotRush.java
python client/hlt_client/client.py gym -r "java MyBotRush" -r "java MyBot" -r "java MyBot" -r "java MyBot" -b halite -i 30 -H 240 -W 320
pause