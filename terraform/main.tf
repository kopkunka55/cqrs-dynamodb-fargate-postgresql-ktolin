provider "aws" {
  region = var.region
}
##################################################################
# Data sources to get VPC and subnets
##################################################################

data "aws_vpc" "vpc" {
  id = var.vpc_id
}

data "aws_subnets" "private" {
  filter {
    name   = "vpc-id"
    values = [var.vpc_id]
  }

  tags = {
    Tier = "Private"
  }
}

data "aws_subnets" "isolated" {
  filter {
    name   = "vpc-id"
    values = [var.vpc_id]
  }

  tags = {
    Tier = "Isolated"
  }
}

resource "aws_security_group" "alb-sg" {
  name        = "${var.project_name}-${var.env_name}-alb-sg"
  description = "Allow HTTP inbound traffic toward ALB"
  vpc_id      = var.vpc_id
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = [var.my_ip]
  }
  tags = {
    "Name" = "${var.project_name}-${var.env_name}-alb-sg"
  }
}

resource "aws_security_group" "ecs-service-sg" {
  name        = "${var.project_name}-${var.env_name}-ecs-service-sg"
  description = "Allow HTTP inbound traffic from ALB to ECS Service"
  vpc_id      = var.vpc_id
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.alb-sg.id]
  }
  tags = {
    "Name" = "${var.project_name}-${var.env_name}-ecs-service-sg"
  }
}

resource "aws_dynamodb_table" "command-db" {
  name     = "${var.project_name}-${var.env_name}"
  hash_key = "request_id"
  range_key = "datetime"

  billing_mode = "PAY_PER_REQUEST"

  attribute {
    name = "request_id"
    type = "S"
  }
  attribute {
    name = "datetime"
    type = "S"
  }

  tags = {
    "Name" = "${var.project_name}-${var.env_name}-ddb-table"
  }
}

resource "aws_kinesis_stream" "rmu" {
  name = "${var.project_name}-${var.env_name}"
  shard_count = 1
  tags = {
    "Name" = "${var.project_name}-${var.env_name}-kinesis-stream"
  }
}

resource "aws_dynamodb_kinesis_streaming_destination" "ddb-stream-with-kinesis" {
  stream_arn = aws_kinesis_stream.rmu.arn
  table_name = aws_dynamodb_table.command-db.name
}