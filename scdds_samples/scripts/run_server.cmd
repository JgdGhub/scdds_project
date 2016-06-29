set JAR1=..\lib\slf4j-api-1.7.21.jar
set JAR2=..\lib\slf4j-simple-1.7.21.jar
set JAR3=..\..\scdds\out\artifacts\scdds-1_0_0.jar
set CDIR=..\..\out\production\scdds_samples

set MAIN=uk.co.octatec.scdds_samples.custom_serializer.ServerWithCustomSerializer

set REG_PORT=9999


java -cp %JAR1%;%JAR2%;%JAR3%;%CDIR%  %MAIN%  -rport:%REG_PORT% > server.log 2>&1


pause