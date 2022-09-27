This is a project reading data from Tweeter and using SparkStream
To save the data to HBase. 
We configured a Kafka producer where we send the read data from the stream
and we read that data through a consumer and We used SparkSql to store that data into Hbase

# Prerequisite
# Docker, Kafka (kafka_2.13-3.2.0) was used for this project
# Docker images: bitnami/zookeeper:latest, bitnami/kafka:latest and qduong/cloudera-java8:v1
# Java8, Java IDE (IntelliJ or Eclipse), you can use VS Code to facilitate docker integration
# Knowing some batch/SSH command
#Spark, SparkStream and Hbase will be mandatory to understand the code

# I - LOCAL MODE
# 1 - Start the ZooKeeper service
# Note: Soon, ZooKeeper will no longer be required by Apache Kafka.
bin/zookeeper-server-start.sh config/zookeeper.properties
#2 - Start the Kafka broker service
bin/kafka-server-start.sh config/server.properties
#3 Run cs523.producer.Producer.java
#4 Run cs523.producer.Listener.java

#II - CLUSTER MODE
#Download and run Docker
#run docker-compose.yml file by the following command
docker-compose -f docker-composer.yml up -d
#Alternatively find documentation about this here: https://hub.docker.com/r/bitnami/kafka
#Open a CLI in cloudera and connect as cloudera
su cloudera
#Create a directory for the jar files of the project
mkdir jarFiles
#Copy the jar files from your host machine to the previous created directory
docker cp "PATH_TO_MY_JAR.jar" docker_image_name:"PATH_TO_JARFILE_DIRECTORY"
#From the JARFILE_DIRECTORY Run the jar file by calling first the producer and then Consumer class
hadoop jar JARFILE.jar PRODUCER_CLASS_NAME
hadoop jar JARFILE.jar CONSUMER_CLASS_NAME
#Check the result in Hbase and alternatively you can 
#Run HUE in your local browser to see the result in the UI