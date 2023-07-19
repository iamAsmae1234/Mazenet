
### Bildung

```
mvn clean compile assembly:single
cd target
```

### Auto configuration starten
```
java -jar maze-client.jar --auto
```
### Hilfe

```
java -jar maze-client.jar [-h] -n <name> -a <host-address> -p <port-number>
MazeNet v2

usage: java -jar maze-client.jar [-ns] -n <name> -a <address> -p <port>
  --auto start autconfiguration
  -l,--address <arg>     wählt die Adresse aus
  -h,--help              zeigt die Hilfe
  -n,--name <arg>        wählt den Namen aus
  -p,--port <arg>        wählt den Port 
  aus
```


