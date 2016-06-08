set JAR1=..\lib\slf4j-api-1.7.21.jar
set JAR2=..\lib\slf4j-simple-1.7.21.jar
set JAR3=..\out\artifacts\scdds_1_0_0_jar\scdds.jar

set REG_PORT=9999

java -cp %JAR1%;%JAR2%;%JAR3%  uk.co.octatec.scdds.net.registry.RegistryServer %REG_PORT%  > registry.log 2>&1


pause