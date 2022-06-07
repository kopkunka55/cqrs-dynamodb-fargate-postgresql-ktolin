# CQRS pattern with DynamoDB and Kotlin

This is a simple wallet application built with Ktor and DynamoDB to record wallet transaction. User can store conins with RESTful API. 

## Tech Stack

| Module                 | Tech                      |
|------------------------|---------------------------|
| Language               | Kotlin/Gradle             |
| Backend APIs Framework | Ktor                      |
| Backend APIs Hosting   | AWS Fargate               |
| ORM                    | Exposed                   |
| IaC                    | Terraform                 |
| RDBMS Migration Tool   | Flyway                    |
| Command Datasource     | Amazon DynamoDB           |
| Query Datasource       | Amazon Aurora PostgreSQL  |
| Jobs                   | Amazon ECS scheduled task |
| Unit test              | JUnit                     |

## Architecture Design

![スクリーンショット 2022-06-07 15 21 22](https://user-images.githubusercontent.com/63289889/172309799-087d4108-4e2d-4d82-af57-ccafb347b7a2.png)
## Software Design

[Onion Architecture](https://en.wikipedia.org/wiki/Hexagonal_architecture_(software))

CQRS is designed based on DDD. In this project, software architecture is constructed by Onion architecture pattern, which is one of the pattern to archive DDD. The design of each layer is following.

| Layer          | Package                       |
|----------------|-------------------------------|
| Presentation   | com.kopkunka55.controller     |
| Infrastructure | com.kopkunka55.infrastructure |
| Domain         | com.kopkunka55.repository     |
| Domain Model   | com.kopkunka55.domain         |


## DynamoDB Table Design

![record](https://user-images.githubusercontent.com/63289889/172311481-e3fbac03-7c69-448f-95ab-c715f5c11bf3.png)

The design of table is following [Single-Table-Design](https://aws.amazon.com/blogs/compute/creating-a-single-table-design-with-amazon-dynamodb/). In this app, we have to aggregate sum of the coins grouped by each hour but the problem is that time-based aggregation query is not so easy with DynamoDB. One of the good solution to archive both high-throughput and consistency, is Transaction API and Optimistic-locking. Below is an example flow to store records.

1. GetItem with the partition key, which is shortened datetime string (e.g. 2022060723). If record exists, the amount attribute of the item have to be updated.
2. To update the attribute, we use Optimistic-locking to update a record safely
3. Finally, store the record as an event data using TransactWriteBatch API, since the update operation for aggregated sum of coins and the event records have to be atomic. 

> As Sort Key of DynamoDB table, we use "X-Amzn-Trace-Id" value in request header from ALB to handle with duplicated requests.


## What does AWS Lambda actually handle with?

CQRS pattern separates Write (Command) datastore and Read (Query) datastore, so we need something to replicate the writer DB to reader one with some transformation. In this app, the Lambda function will be invoked each hour and reads aggregated data, then write back to PostgreSQL table with real timestamp. As long as we need time-based complex query, PostgreSQL would be better than DynamoDB.

## Getting Started 🚀

You can run each application locally by following steps

```bash
docker build -t cqrs-api . -f api.Dockerfile
docker build -t cqrs-rmu . -f rmu.Dockerfile
```

Command App
```bash
docker run \
-e CQRS_MODE=COMMAND 
-e AWS_SECRET_KEY_ID=xxxxxxx
-e AWS_SECRET_ACCESS_KEY=xxxxxxx
-it api
```

Query App

```bash
docker run \
-e CQRS_MODE=QUERY
-e DATABASE_ENDPOINT=xxxxxxx
-e DATABASE_USER_NAME=xxxxxxx
-e DATABASE_PASSWORD=xxxxxxx
-it api
```
Or simply run

```bash
docker-compose up -d
```

## Hosting on AWS ☁️
All the resources are manged by Terraform to archive Infrastructure as Code. You can just execute following command at `/terraform` directory.

```
terraform apply -var-file dev.tfvars
```

We can apply dynamic variable to resources without hardcoding secret values like Database password

```tfvaaars
vpc_id="vpc-xxxxxxxx" // VPC shoudl be created before applying this
region="us-east-1"
my_ip="XXX.XXX.XXX.XXX/32" // Source IP of ALB Security Group
aurora_user="xxxxxxx"
aurora_password="xxxxxx"
acm_certification_arn="arn:aws:acm:us-east-1:xxxxxxxxxx" // To terminate TLS at ALB
```

> You need to create VPC, which has both public and private subnets in advance


## Do you want Fat Jar?
You can just execute following command. [shadow](https://github.com/johnrengelman/shadow) will package all dependencies into single jar. In this app, we need two different Fat Jar for API and RMU (Read Model Updater)

```bash
 ./gradlew shadowJar --no-daemon -PmainClass=API // Query and Command APIs
 ./gradlew shadowJar --no-daemon -PmainClass=RMU // Read Model Updater
```