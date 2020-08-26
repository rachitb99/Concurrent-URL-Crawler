# CS3211_Project
Final project for the course CS3211 - Parallel and Concurrent Programming

This project is concerned with implementing a java based web-scraper. Assertions, such as the non-existence of deadlocks/data races are verified with PAT (Process Analysis Toolkit). 

## Java web scraper

## PAT mdoel
PAT (Process Analysis Toolkit) is a toolkit developed at the National University of Singapore (NUS) with the intent of virefying properties, such as deadlock freeness, of a software system. A model which captures the behavoiur of the system is developed in PAT, whereafter assertions are evaluated. The PAT model illustrates the problemacies related to not establishing a critical session whilst writing to the indexed URL tree and gives the traces of execution assocciated with the violation of uniquness of records in the tree.

## To create agent
The repo that we provide will include a snapshot of the project which should be sufficient to run the agent. Nevertheless, the following section describes how to generate a jar file for the project, which can later be used as the agent. 

* Right click on project in eclipse
* Select Run as --> maven clean
* Select Run as --> maven install, this will generate a .jar file in the /target directory (The one provided is called cs3211_project-0.0.1-SNAPSHOT.jar)
* go into terminal
* navigate to the target directory ( for me it's /Users/niklas/Documents/Modules/CS3211 Parallel and Concurrent Programming/CS3211_Project/cs3211_project/target)
* run the following command java -javaagent:<jar file> -cp . <main file>
* The jar file name i used was: cs3211_project-0.0.1-SNAPSHOT.jar 
* The main file I used was: agent.TestMain

## To run the correct agent
before getting to the actual agent there are a couple of modifications that will enable you to properly see the data race. Follow these instructions: 
* rename the file seed.txt to seed_old.txt
* rename the file seed-duplicate.txt to seed.txt
This will allow you to feed the bufferes with duplicated URLs.

Moreover, to speed up the process of filling the buffers you manually want to change the MAX_CAPACITY - instance variable at line 25 of driver.WebCrawlerDriver to something small (say 5). Remember to set this back to 1000 one you are done experimenting. 

The agent is designed to solve all three parts of the p4 tasks. Therefore, it's importatnt to let the agent know which implementation to use. The agent-version is supplied as the first argument to the agent in the following manner -javaagent:file.jar="<version><PAT trace>", where <version> is replaced by one of the following [0,1,2]. They mean the following:
* 0: Sleep implementation
* 1: Lock implementation
* 2: Orchestration implementation

to run the agent, do the following. 

* pwd to the project directory (for me it is /Users/niklas/Documents/Modules/CS3211 Parallel and Concurrent Programming/CS3211_Project/cs3211_project)
* Export your project as a .jar into the target folder (File —> Export —> Java —> Runnable Jar file)
* I named it Project.jar
* The java agent is named cs3211_project-0.0.1-SNAPSHOT.jar
* The string parameter follows immediately after the agent name
* The parameters of the program are appended after the Program.jar file as usual.

Here is the syntax for running the program with an agent:

cs3211_project % java -javaagent:./target/<agent.jar>="<version><PAT trace>" -jar ./target/<program-jar.jar> <args to program>
  
Here is an example syntax for further demonstration:

cs3211_project % java -javaagent:./target/cs3211_project-0.0.1-SNAPSHOT.jar="2<init -> [fill] -> [fill] -> [lock] -> checkDuplicate.6 -> [write] ->  checkDuplicate.7 -> no_duplicates>" -jar ./target/Program.jar -time 1m -input seed.txt -output res2.txt -storedPageNum 1000

