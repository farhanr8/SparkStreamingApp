## SPARK STREAMING APPLICATION

### Brief Summary

This Spark Streaming application continuously reads data from Twitter about some topic. Those Twitter feeds was then analyzed for their sentiment. For my sentiment analysis, I have chosen to run my application with the keyword "biden" on May 2021. The graphical plots have been provided below. The donut visualization was from my first run around 5pm and I only ran it for a couple of minutes. Even then it seemed to be heavily negative tweets. I ran it again at night around 11pm for close to an hour and still the tweets were mostly negative. It may have something to do with Biden’s plan to restrict travel to/from India with the rise of Covid cases. Another issue could be that the NLP algorithm that I used could be more fine-tuned because when I read some of the tweets, they seemed more neutral than negative. Improving the sentiment analysis could be a future work for this project.

<p align="center">
  <img src="https://github.com/farhanr8/SparkStreamingApp/blob/master/images/Picture1.png">
</p>
<p align="center">Figure 1. Donut Visualization</p>


![alt text](https://github.com/farhanr8/SparkStreamingApp/blob/master/images/Picture2.png "Figure 2")
<p align="center">Figure 2. Elastic Search Dashboard</p>



### Running the Application

Prerequisites:
 - Scala (version 2.12.10)
 - Spark (version 3.1.1)
 - SBT (version 1.5.1)
 - Kafka (2.7.0)
   - Zookeeper
 - ElasticSearch (7.12.1)
   - Kibana
   - Logstash

Create a Twitter Developer account at https://apps.twitter.com
This will give you certain credentials like the consumer key and the access tokens. Please fill in the appropriate fields in the _src/main/resources/app.properties_ file.

After all that, please run the command _sbt assembly_ in the folder containing the _build.sbt_ file. This should create a jar file in the _target/scala-2.12/_ folder.

To run the code, I used the following commands in order:

Start Zookeeper:

	bin/zookeeper-server-start.sh config/zookeeper.properties

Start Kafka:

	bin/kafka-server-start.sh config/server.properties

Create a topic:

	bin/kafka-topics.sh --create --topic hw3 --bootstrap-server localhost:9092

Start a consumer on topic:

	bin/kafka-console-consumer.sh --topic hw3 --from-beginning --bootstrap-server localhost:9092

Run Application:

	spark-submit --packages org.apache.spark:spark-sql-kafka-0-10_2.12:3.1.1 --class TweetSentiment target/scala-2.12/kafka-assembly-0.1.jar hw3 biden

### Data Visualization 

Start ElasticSearch:

	cd into the elastic search bin folder
	./elasticsearch

Start Kibana:

	cd into the kibana bin folder
	./kibana

Start Logstash:

	cd into the logstash bin folder
	./logstash -f <PathToProject>/Assignment3/logstash-simple.conf

Now on your browser, go to the webpage http://localhost:5601/

It should notify you that the application is picking up the streaming data. Continue to create an index with the same name as provided in the logstash-simple configuration file. Now you can go to the visualize tab and create a dashboard as you see fit.


