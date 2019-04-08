
## Introduction

This project is a fork of the original Paysim project. It implements extended features for fraud detection and credit scoring.

## Project Leader

Dr. Edgar Lopez-Rojas
http://edgarlopez.net 

More on PaySim: http://edgarlopez.net/simulation-tools/paysim/

Dataset sample: https://www.kaggle.com/ntnu-testimon/paysim1

## Description

PaySim, a Mobile Money Payment Simulator The Mobile Money Payment Simulation case study is based on a real company that has developed a mobile money implementation that provides mobile phone users with the ability to transfer money between themselves using the phone as a sort of electronic wallet. The task at hand is to develop an approach that detects suspicious activities that are indicative of fraud. Unfortunately, during the initial part of our research this service was only been running in a demo mode. This prevented us from collecting any data that could had been used for analysis of possible detection methods. The development of PaySim covers two phases. During the first phase, we modelled and implemented a MABS that used the schema of the real mobile money service and generated synthetic data following scenarios that were based on predictions of what could be possible when the real system starts operating. During the second phase we got access to transactional financial logs of the system and developed a new version of the simulator which uses aggregated transactional data to generate financial information more alike the original source. Kaggle has featured PaySim1 as dataset of the week of april 2018. See the full article: http://blog.kaggle.com/2017/05/01/datasets-of-the-week-april-2017/ 

## Installation and Usage
First you can use one of the GitHub-releases. The releases are compiled and just need to be extracted. After that you can use.

  &nbsp;&nbsp;&nbsp;&nbsp; java -jar paysim.jar
  
The other option is to compile the Java sources by yourself. The project is an maven project. Thus you can build it with the standard maven command

  &nbsp;&nbsp;&nbsp;&nbsp; mvn package
  
Again you have a jar application which you can execute.



## PaySim first paper of the simulator:

Please refer to this dataset using the following citations:

E. A. Lopez-Rojas , A. Elmir, and S. Axelsson. "PaySim: A financial mobile money simulator for fraud detection". In: The 28th European Modeling and Simulation Symposium-EMSS, Larnaca, Cyprus. 2016


## Acknowledgements
This work is part of the research project ”Scalable resource-efficient systems for big data analytics” funded by the Knowledge Foundation (grant: 20140032) in Sweden.

Master's thesis: Elmir A. PaySim Financial Simulator : PaySim Financial Simulator [Internet] [Dissertation]. 2016. Available from: http://urn.kb.se/resolve?urn=urn:nbn:se:bth-14061

PhD Thesis Dr. Edgar Lopez-Rojas
http://bth.diva-portal.org/smash/record.jsf?pid=diva2%3A955852&dswid=-1552
