provider "aws" {
  region = var.region
}

data "aws_caller_identity" "current" {}

##################################################################
# Data sources to get VPC and subnets
##################################################################

data "aws_vpc" "vpc" {
  id = var.vpc_id
}

data "aws_subnets" "public" {
  filter {
    name   = "vpc-id"
    values = [var.vpc_id]
  }

  tags = {
    Tier = "Public"
  }
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
  hash_key = "PK"
  range_key = "SK"

  billing_mode = "PAY_PER_REQUEST"

  attribute {
    name = "PK"
    type = "S"
  }
  attribute {
    name = "SK"
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

##################################################################
# AWS ALB
##################################################################

resource "aws_lb" "alb" {
  name               = "${var.project_name}-${var.env_name}-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb-sg.id]
  subnets            = data.aws_subnets.public.ids

  enable_deletion_protection = false

  tags = {
    "Name" = "${var.project_name}-${var.env_name}-alb"
  }
}

resource "aws_lb_listener" "alb-https-listener" {
  load_balancer_arn = aws_lb.alb.arn
  port              = "443"
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-2016-08"
  certificate_arn   = var.acm_certification_arn

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.alb-command-tg.arn
  }
  depends_on = [aws_lb_target_group.alb-command-tg, aws_lb_target_group.alb-query-tg]
}

resource "aws_lb_listener_rule" "alb-listener-rule-query" {
  listener_arn = aws_lb_listener.alb-https-listener.arn
  priority     = 99

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.alb-query-tg.arn
  }

  condition {
    path_pattern {
      values = ["/query/*"]
    }
  }
}

resource "aws_lb_listener_rule" "alb-listener-rule-command" {
  listener_arn = aws_lb_listener.alb-https-listener.arn
  priority     = 98

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.alb-command-tg.arn
  }

  condition {
    path_pattern {
      values = ["/command/*"]
    }
  }
}

resource "aws_lb_target_group" "alb-query-tg" {
  name     = "${var.project_name}-${var.env_name}-alb-query-tg"
  port     = 8080
  protocol = "HTTP"
  vpc_id   = data.aws_vpc.vpc.id
  target_type = "ip"
  health_check {
   path = "/command/health-check"
  }
  tags = {
    "Name" = "${var.project_name}-${var.env_name}-alb-query-tg"
  }
}

resource "aws_lb_target_group" "alb-command-tg" {
  name     = "${var.project_name}-${var.env_name}-alb-command-tg"
  port     = 8080
  protocol = "HTTP"
  target_type = "ip"
  vpc_id   = data.aws_vpc.vpc.id
  health_check {
    path = "/query/health-check"
  }
  tags = {
    "Name" = "${var.project_name}-${var.env_name}-alb-command-tg"
  }
}

##################################################################
# ECS Service
##################################################################

resource "aws_iam_role" "execution-role" {
  name                = "${var.project_name}-${var.env_name}-ecs-execution-role"
  managed_policy_arns = ["arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"]
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Sid    = ""
        Principal = {
          Service = "ecs-tasks.amazonaws.com"
        }
      },
    ]
  })
  tags = {
    "Name" = "${var.project_name}-${var.env_name}-ecs-execution-role"
  }
}

resource "aws_iam_role" "task-role" {
  name                = "${var.project_name}-${var.env_name}-ecs-task-role"
  managed_policy_arns = ["arn:aws:iam::aws:policy/AmazonDynamoDBFullAccess"]
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Sid    = ""
        Principal = {
          Service = "ecs-tasks.amazonaws.com"
        }
      },
    ]
  })
  tags = {
    "Name" = "${var.project_name}-${var.env_name}-ecs-task-role"
  }
}
##################################################################
# ECS Service
##################################################################

resource "aws_ecs_cluster" "ecs-cluster" {
  name = "${var.project_name}-${var.env_name}-ecs-cluster"

  setting {
    name  = "containerInsights"
    value = "enabled"
  }

  tags = {
    "Name" = "${var.project_name}-${var.env_name}-ecs-cluster"
  }
}

resource "aws_ecs_task_definition" "ecs-query-task-df" {
  family = "query-task"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = 256
  memory                   = 512
  task_role_arn = aws_iam_role.task-role.arn
  execution_role_arn = aws_iam_role.execution-role.arn
  container_definitions = jsonencode([
    {
      name      = "query-api"
      image     = "${data.aws_caller_identity.current.account_id}.dkr.ecr.us-east-1.amazonaws.com/anywallet-api:latest"
      cpu       = 256
      memory    = 512
      essential = true
      environment = [
        {
          name = "DATABASE_ENDPOINT"
          value = aws_rds_cluster.aurora-cluster.endpoint
        },
        {
          name = "DATABASE_USER"
          value = var.aurora_user
        },
        {
          name = "DATABASE_PASSWORD"
          value = var.aurora_password
        },
        {
          name = "PORT"
          value = "8080"
        },
        {
          name = "CQRS_MODE"
          value = "QUERY"
        }
      ]
      portMappings = [
        {
          containerPort = 8080
          hostPort      = 8080
        }
      ]
    },
  ])
  tags = {
    "Name" = "${var.project_name}-${var.env_name}-ecs-query-task-df"
  }
}

resource "aws_ecs_task_definition" "ecs-command-task-df" {
  family = "command-task"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = 256
  memory                   = 512
  task_role_arn = aws_iam_role.task-role.arn
  execution_role_arn = aws_iam_role.execution-role.arn
  container_definitions = jsonencode([
    {
      name      = "command-api"
      image     = "${data.aws_caller_identity.current.account_id}.dkr.ecr.us-east-1.amazonaws.com/anywallet-api:latest"
      cpu       = 256
      memory    = 512
      essential = true
      environment = [
        {
          name = "PORT"
          value = "8080"
        },
        {
          name = "CQRS_MODE"
          value = "COMMAND"
        }
      ]
      portMappings = [
        {
          containerPort = 8080
          hostPort      = 8080
        }
      ]
    },
  ])
  tags = {
    "Name" = "${var.project_name}-${var.env_name}-ecs-command-task-df"
  }
}


resource "aws_ecs_service" "ecs-query-service" {
  name            = "${var.project_name}-${var.env_name}-ecs-query-service"
  cluster         = aws_ecs_cluster.ecs-cluster.id
  task_definition = aws_ecs_task_definition.ecs-query-task-df.arn
  desired_count   = 3
  launch_type = "FARGATE"
  platform_version = "1.4.0"

  network_configuration {
    subnets = data.aws_subnets.private.ids
    security_groups = [aws_security_group.ecs-task-sg.id]
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.alb-query-tg.arn
    container_name   = "query-api"
    container_port   = 8080
  }
  tags = {
    "Name" = "${var.project_name}-${var.env_name}-ecs-query-service"
  }
}

resource "aws_ecs_service" "ecs-command-service" {
  name            = "${var.project_name}-${var.env_name}-ecs-command-service"
  cluster         = aws_ecs_cluster.ecs-cluster.id
  task_definition = aws_ecs_task_definition.ecs-command-task-df.arn
  desired_count   = 3
  launch_type = "FARGATE"
  platform_version = "1.4.0"

  network_configuration {
    subnets = data.aws_subnets.private.ids
    security_groups = [aws_security_group.ecs-task-sg.id]
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.alb-command-tg.arn
    container_name   = "command-api"
    container_port   = 8080
  }
  tags = {
    "Name" = "${var.project_name}-${var.env_name}-ecs-command-service"
  }
}
