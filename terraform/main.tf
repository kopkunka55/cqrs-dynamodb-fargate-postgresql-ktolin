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
  name        = "alb-sg"
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
}

resource "aws_security_group" "ecs-service-sg" {
  name        = "ecs-service-sg"
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
}
