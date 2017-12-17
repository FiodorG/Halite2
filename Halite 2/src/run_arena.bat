del /S *.hlt
del /S *.class
del /S *.log

javac MyBot.java
javac MyBotRush.java
python client/hlt_client/client.py gym -r "java MyBot" -r "java MyBotRush" -b halite -i 50 -H 176 -W 264
pause