## Contents
In this project, we will develop a REST Api to parse a JSON schema model divided into three demos
1. **Prototype demo 1**
    1. Develop a Spring Boot based REST Api to parse a given sample JSON schema.
    2. Save the JSON schema in a redis key value store.
    3. Demonstrate the use of operations like `GET`, `POST` and `DELETE` for the first prototype demo.
2. **Prototype demo 2**
    1. Regress on your model and perform additional operations like `PUT` and `PATCH`.
    2. Secure the REST Api with a security protocol Oauth2 with Google Api.
3. **Prototype demo 3**
    1. Adding Elasticsearch capabilities, implementing parent-child relation for data searching and operation. 
    2. Using RedisMQ for REST API queueing

## Application Structure
[**Architecture diagram**](ArchitectureDiagram.pdf)

## Pre-requisites
1. Java
2. Maven 
3. Redis Server
4. Elasticsearch and Kibana(Local or cloud based)

## Build and Run 
Run as Spring Boot Application in any IDE.

## Querying Elasticsearch
1. Run both the application `Basic Service` and `Consumer Message Queue(CMQ)`. CMQ application will create the indexes.
2. Run POST query from Postman to parse usecase.txt
3. Run custom search queries as per your use case, in parent-child rules.
