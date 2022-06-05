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

resource "aws_security_group" "ecs-task-sg" {
  name        = "${var.project_name}-${var.env_name}-ecs-task-sg"
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
    "Name" = "${var.project_name}-${var.env_name}-ecs-task-sg"
  }
}

resource "aws_security_group" "aurora-sg" {
  name        = "${var.project_name}-${var.env_name}-aurora-sg"
  description = "Allow HTTP inbound traffic from ECS to Aurora Service"
  vpc_id      = var.vpc_id
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs-task-sg.id]
  }
  tags = {
    "Name" = "${var.project_name}-${var.env_name}-aurora-sg"
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

##################################################################
# Aurora Serverless
##################################################################

resource "aws_db_subnet_group" "aurora-subnet-group" {
  name       = "main"
  subnet_ids = data.aws_subnets.isolated.ids

  tags = {
    "Name" = "${var.project_name}-${var.env_name}-aurora-subnet-group"
  }
}

resource "aws_rds_cluster" "aurora-cluster" {
  cluster_identifier = "${var.project_name}-${var.env_name}-aurora-cluster"
  db_subnet_group_name = aws_db_subnet_group.aurora-subnet-group.name
  engine             = "aurora-postgresql"
  engine_mode        = "provisioned"
  engine_version     = "13.6"
  database_name      = "anywallet"
  master_username    = var.aurora_user
  master_password    = var.aurora_password
  skip_final_snapshot = true
  vpc_security_group_ids = [aws_security_group.aurora-sg.id]

  serverlessv2_scaling_configuration {
    max_capacity = 1.0
    min_capacity = 0.5
  }

  tags = {
    "Name" = "${var.project_name}-${var.env_name}-aurora-cluster"
  }
}

resource "aws_rds_cluster_instance" "aurora-instance-1" {
  cluster_identifier = aws_rds_cluster.aurora-cluster.id
  identifier = "${var.project_name}-${var.env_name}-aurora-instance-1"
  instance_class     = "db.serverless"
  engine             = aws_rds_cluster.aurora-cluster.engine
  engine_version     = aws_rds_cluster.aurora-cluster.engine_version
  tags = {
    "Name" = "${var.project_name}-${var.env_name}-aurora-instance-1"
  }
}

resource "aws_rds_cluster_instance" "aurora-instance-2" {
  cluster_identifier = aws_rds_cluster.aurora-cluster.id
  identifier = "${var.project_name}-${var.env_name}-aurora-instance-2"
  instance_class     = "db.serverless"
  engine             = aws_rds_cluster.aurora-cluster.engine
  engine_version     = aws_rds_cluster.aurora-cluster.engine_version
  tags = {
    "Name" = "${var.project_name}-${var.env_name}-aurora-instance-2"
  }
}
