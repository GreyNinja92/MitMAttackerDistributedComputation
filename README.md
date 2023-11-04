# Name: Saksham Khatod
# Man in the Middle Attack (Apache Spark)

This project uses Apache Spark to parallelize Random Walks on graphs.
It computes traceability links between two graphs and decides whether to iterate or attack. 
It then compares its result with the solution i.e. golden yaml file to compute  accuracy & precision scores. At the end of the algorithm, it states how many attacks were successful & unsuccessful.

To run the project, make sure you have sbt, scala, jdk installed on your machine.
This project uses scala version 2.12.17, sbt version 1.9.6 & jdk from openjdk 11.0.20.1 2023-08-24.
This project includes 6 tests built using scalatest.

This project is built for both local machines & AWS EMR. Most files have comments with AWS EMR specific code.
If you want to deploy this project to AWS EMR, uncomment code blocks with AWS EMR written on the top and remove variables with the same names.

Each file is filled with comments that explain the methods used for the project.

To run the project, navigate to the project directory and run the following.

```
export SBT_OPTS="-XX:+CMSClassUnloadingEnabled -Xmx2G -Xms1G"
sbt clean compile test run
```

This algorithm averages around 86% accuracy & 100% of attacks are successful.

Here's a youtube video link that explains the process of building this project and running it on AWS EMR : https://www.youtube.com/watch?v=aFvPYwzhfKQ

