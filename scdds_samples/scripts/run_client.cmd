set JAR1=..\lib\slf4j-api-1.7.21.jar
set JAR2=..\lib\slf4j-simple-1.7.21.jar
set JAR3=..\..\scdds\out\artifacts\scdds_1_0_0_jar\scdds.jar
set CDIR=..\out\production\scdds_samples

set MAIN=uk.co.octatec.scdds_samples.basic_example.client.Client

set REG_PORT=9999


java -cp %JAR1%;%JAR2%;%JAR3%;%CDIR%  %MAIN%  -rport:%REG_PORT%  > client.log 2>&1


pause